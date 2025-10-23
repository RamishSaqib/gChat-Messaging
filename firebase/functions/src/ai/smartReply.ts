/**
 * Smart Reply Functions
 * 
 * Generates context-aware, personalized reply suggestions using RAG (Retrieval-Augmented Generation)
 * and user communication style analysis
 */

import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getFirestore } from 'firebase-admin/firestore';
import { openai, MODELS } from '../utils/openai';
import { checkRateLimit } from '../utils/rateLimit';

interface SmartReplyRequest {
  conversationId: string;
  incomingMessageId: string;
  targetLanguage: string;
}

interface Message {
  id: string;
  text: string;
  senderId: string;
  timestamp: number;
  type: string;
}

interface UserCommunicationStyle {
  avgMessageLength: number; // words
  emojiUsage: 'FREQUENT' | 'OCCASIONAL' | 'RARE';
  tone: 'CASUAL' | 'CONVERSATIONAL' | 'FORMAL';
  commonPhrases: string[];
  usesContractions: boolean;
  punctuationStyle: string; // 'minimal', 'standard', 'expressive'
}

interface SmartReply {
  replyText: string;
  confidence: number; // 0.0-1.0
  category: 'AFFIRMATIVE' | 'NEGATIVE' | 'QUESTION' | 'NEUTRAL';
}

/**
 * Generate context-aware smart reply suggestions
 */
export const generateSmartReplies = onCall<SmartReplyRequest>(
  {
    memory: '1GiB', // Need more memory for RAG processing
    timeoutSeconds: 120,
    region: 'us-central1',
  },
  async (request) => {
    // Verify authentication
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { conversationId, incomingMessageId, targetLanguage } = request.data;
    const currentUserId = request.auth.uid;

    // Validate input
    if (!conversationId || !incomingMessageId || !targetLanguage) {
      throw new HttpsError(
        'invalid-argument',
        'conversationId, incomingMessageId, and targetLanguage are required'
      );
    }

    try {
      // Check rate limit (50 requests per hour)
      await checkRateLimit(currentUserId, 'smartReply', {
        maxRequests: 50,
        windowMinutes: 60,
      });

      console.log(`Generating smart replies for conversation ${conversationId}...`);

      const db = getFirestore();

      // === STEP 1: Verify conversation access ===
      const conversationDoc = await db.collection('conversations').doc(conversationId).get();
      if (!conversationDoc.exists) {
        throw new HttpsError('not-found', 'Conversation not found');
      }

      const conversation = conversationDoc.data();
      const participants = conversation?.participants || [];
      
      if (!participants.includes(currentUserId)) {
        throw new HttpsError('permission-denied', 'User is not a participant in this conversation');
      }

      // === STEP 2: Fetch incoming message ===
      const incomingMessageDoc = await db
        .collection('conversations')
        .doc(conversationId)
        .collection('messages')
        .doc(incomingMessageId)
        .get();

      if (!incomingMessageDoc.exists) {
        throw new HttpsError('not-found', 'Incoming message not found');
      }

      const incomingMessage = incomingMessageDoc.data() as Message;

      // Only generate replies for text messages
      if (incomingMessage.type !== 'TEXT' || !incomingMessage.text) {
        throw new HttpsError('invalid-argument', 'Can only generate replies for text messages');
      }

      // === STEP 3: RAG Pipeline - Fetch recent conversation context (last 50 messages) ===
      const messagesSnapshot = await db
        .collection('conversations')
        .doc(conversationId)
        .collection('messages')
        .where('type', '==', 'TEXT')
        .orderBy('timestamp', 'desc')
        .limit(50)
        .get();

      const allMessages: Message[] = messagesSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      } as Message));

      // Reverse to chronological order
      allMessages.reverse();

      // === STEP 4: Analyze user's communication style ===
      const userMessages = allMessages.filter(msg => msg.senderId === currentUserId);
      const userStyle = analyzeUserStyle(userMessages);

      console.log('User communication style:', JSON.stringify(userStyle, null, 2));

      // === STEP 5: Build conversation context for GPT-4 ===
      // Take last 30 messages to keep within token limits
      const recentMessages = allMessages.slice(-30);
      const conversationContext = recentMessages
        .map(msg => {
          const label = msg.senderId === currentUserId ? 'You' : 'Other';
          return `${label}: ${msg.text}`;
        })
        .join('\n');

      // === STEP 6: Generate smart replies using GPT-4 ===
      const systemPrompt = buildSystemPrompt(userStyle, targetLanguage);
      const userPrompt = buildUserPrompt(conversationContext, incomingMessage.text);

      console.log('Calling GPT-4 for smart reply generation...');

      const completion = await openai.chat.completions.create({
        model: MODELS.ASSISTANT,
        messages: [
          {
            role: 'system',
            content: systemPrompt,
          },
          {
            role: 'user',
            content: userPrompt,
          },
        ],
        temperature: 0.8, // Higher for diverse replies
        max_tokens: 500,
        response_format: { type: 'json_object' },
      });

      const responseText = completion.choices[0].message.content || '{}';
      const parsedResponse = JSON.parse(responseText);

      // Validate and format response
      const replies: SmartReply[] = (parsedResponse.replies || []).map((reply: any, index: number) => ({
        replyText: reply.text || reply.replyText || '',
        confidence: reply.confidence || 0.9 - (index * 0.1), // Decreasing confidence
        category: reply.category || 'NEUTRAL',
      }));

      // Ensure we have 3 replies
      if (replies.length < 3) {
        console.warn(`Only ${replies.length} replies generated, expected 3`);
      }

      console.log(`Successfully generated ${replies.length} smart replies`);

      return {
        replies: replies.slice(0, 3), // Return max 3 replies
        userStyle,
        cached: false,
      };
    } catch (error: any) {
      console.error('Smart reply generation error:', error);

      // Re-throw rate limit and permission errors
      if (error.code === 'resource-exhausted' || error.code === 'permission-denied') {
        throw error;
      }

      throw new HttpsError('internal', `Smart reply generation failed: ${error.message}`);
    }
  }
);

/**
 * Analyze user's communication style from their message history
 */
function analyzeUserStyle(userMessages: Message[]): UserCommunicationStyle {
  if (userMessages.length === 0) {
    // Return default style if no message history
    return {
      avgMessageLength: 10,
      emojiUsage: 'OCCASIONAL',
      tone: 'CONVERSATIONAL',
      commonPhrases: [],
      usesContractions: true,
      punctuationStyle: 'standard',
    };
  }

  // Calculate average message length (in words)
  const totalWords = userMessages.reduce((sum, msg) => {
    return sum + (msg.text?.split(/\s+/).length || 0);
  }, 0);
  const avgMessageLength = Math.round(totalWords / userMessages.length);

  // Analyze emoji usage
  const emojiRegex = /[\p{Emoji_Presentation}\p{Emoji}\u200D]/gu;
  let totalEmojis = 0;
  userMessages.forEach(msg => {
    const matches = msg.text?.match(emojiRegex);
    totalEmojis += matches ? matches.length : 0;
  });
  const emojisPerMessage = totalEmojis / userMessages.length;
  const emojiUsage = emojisPerMessage > 2 ? 'FREQUENT' : emojisPerMessage > 0.5 ? 'OCCASIONAL' : 'RARE';

  // Detect contractions (indicative of casual tone)
  const contractionRegex = /\b(can't|won't|don't|didn't|isn't|aren't|wasn't|weren't|haven't|hasn't|hadn't|wouldn't|shouldn't|couldn't|I'm|you're|he's|she's|it's|we're|they're|I've|you've|we've|they've|I'll|you'll|he'll|she'll|we'll|they'll)\b/gi;
  let totalContractions = 0;
  userMessages.forEach(msg => {
    const matches = msg.text?.match(contractionRegex);
    totalContractions += matches ? matches.length : 0;
  });
  const usesContractions = totalContractions > userMessages.length * 0.2; // 20% threshold

  // Analyze punctuation style
  let exclamationCount = 0;
  let questionCount = 0;
  let multiPunctuation = 0;
  userMessages.forEach(msg => {
    exclamationCount += (msg.text?.match(/!/g) || []).length;
    questionCount += (msg.text?.match(/\?/g) || []).length;
    multiPunctuation += (msg.text?.match(/[!?]{2,}|\.{3,}/g) || []).length;
  });
  const punctuationStyle = 
    multiPunctuation > userMessages.length * 0.3 ? 'expressive' :
    exclamationCount + questionCount < userMessages.length * 0.1 ? 'minimal' :
    'standard';

  // Determine overall tone
  const isShortMessages = avgMessageLength < 8;
  const tone = 
    (!usesContractions && punctuationStyle === 'standard' && emojiUsage === 'RARE') ? 'FORMAL' :
    (usesContractions && (isShortMessages || emojiUsage === 'FREQUENT')) ? 'CASUAL' :
    'CONVERSATIONAL';

  // Extract common phrases (simple approach - find repeated 2-3 word sequences)
  const phraseMap = new Map<string, number>();
  userMessages.forEach(msg => {
    const words = msg.text?.toLowerCase().split(/\s+/) || [];
    for (let i = 0; i < words.length - 1; i++) {
      const phrase = `${words[i]} ${words[i + 1]}`;
      phraseMap.set(phrase, (phraseMap.get(phrase) || 0) + 1);
    }
  });

  // Get top 5 common phrases (appearing at least 3 times)
  const commonPhrases = Array.from(phraseMap.entries())
    .filter(([_, count]) => count >= 3)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([phrase]) => phrase);

  return {
    avgMessageLength,
    emojiUsage,
    tone,
    commonPhrases,
    usesContractions,
    punctuationStyle,
  };
}

/**
 * Build system prompt for GPT-4 based on user style
 */
function buildSystemPrompt(style: UserCommunicationStyle, targetLanguage: string): string {
  const toneDescriptions = {
    CASUAL: 'very casual and friendly, using contractions, emojis, and informal language',
    CONVERSATIONAL: 'natural and conversational, balanced between casual and professional',
    FORMAL: 'professional and polite, using complete sentences and proper grammar',
  };

  const emojiGuidance = {
    FREQUENT: 'Include emojis generously to match the user\'s expressive style.',
    OCCASIONAL: 'Include emojis occasionally when appropriate.',
    RARE: 'Use emojis sparingly or not at all.',
  };

  return `You are an AI assistant generating reply suggestions for a messaging app. 
Your goal is to provide 3 contextually appropriate reply options that match the user's communication style.

TARGET LANGUAGE: ${targetLanguage}
All replies must be in ${targetLanguage}.

USER'S COMMUNICATION STYLE:
- Tone: ${style.tone} (${toneDescriptions[style.tone]})
- Average message length: ${style.avgMessageLength} words
- Emoji usage: ${style.emojiUsage} (${emojiGuidance[style.emojiUsage]})
- Contractions: ${style.usesContractions ? 'Uses contractions frequently' : 'Prefers full words'}
- Punctuation: ${style.punctuationStyle}
${style.commonPhrases.length > 0 ? `- Common phrases: ${style.commonPhrases.join(', ')}` : ''}

REPLY GENERATION RULES:
1. Generate exactly 3 diverse replies
2. Match the user's communication style closely
3. Make replies contextually relevant to the conversation
4. Vary reply lengths: one short (3-5 words), one medium (6-12 words), one longer (13-20 words)
5. Categorize each reply as: AFFIRMATIVE, NEGATIVE, QUESTION, or NEUTRAL
6. Assign confidence scores (0.0-1.0) based on how well the reply fits the context

OUTPUT FORMAT:
Return a JSON object with this exact structure:
{
  "replies": [
    {
      "text": "Reply text here",
      "confidence": 0.95,
      "category": "AFFIRMATIVE"
    },
    {
      "text": "Another reply",
      "confidence": 0.90,
      "category": "QUESTION"
    },
    {
      "text": "Third reply option",
      "confidence": 0.85,
      "category": "NEUTRAL"
    }
  ]
}`;
}

/**
 * Build user prompt with conversation context
 */
function buildUserPrompt(conversationContext: string, incomingMessage: string): string {
  return `CONVERSATION CONTEXT:
${conversationContext}

INCOMING MESSAGE (requiring reply):
Other: ${incomingMessage}

Generate 3 diverse, contextually appropriate reply suggestions that match my communication style.`;
}
