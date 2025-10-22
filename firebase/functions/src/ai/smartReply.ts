/**
 * Smart Reply Functions
 * 
 * Generates context-aware reply suggestions in multiple languages
 */

import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';
import { getFirestore } from 'firebase-admin/firestore';

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

const db = getFirestore();

interface SmartReplyRequest {
  conversationId: string;
  incomingMessage: string;
  targetLanguage: string;
}

/**
 * Generate contextual smart reply suggestions
 */
export const generateSmartReplies = onCall<SmartReplyRequest>(
  {
    memory: '512MiB',
    timeoutSeconds: 60,
    region: 'us-central1',
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { conversationId, incomingMessage, targetLanguage } = request.data;
    const userId = request.auth.uid;

    if (!conversationId || !incomingMessage) {
      throw new HttpsError('invalid-argument', 'Conversation ID and incoming message are required');
    }

    try {
      // 1. Retrieve conversation context (RAG pipeline)
      const conversationContext = await getConversationContext(conversationId, userId, 50);

      // 2. Analyze user's communication style
      const userStyle = await analyzeUserStyle(userId, conversationContext);

      // 3. Generate contextual replies
      const completion = await openai.chat.completions.create({
        model: 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `You are an AI assistant generating natural reply suggestions. Generate 3 short, contextually appropriate replies in ${targetLanguage}.

User's communication style:
- Tone: ${userStyle.tone}
- Emoji usage: ${userStyle.emojiUsage}
- Average message length: ${userStyle.avgLength} words
- Formality: ${userStyle.formality}

Generate replies that:
1. Match this communication style
2. Are appropriate responses to the incoming message
3. Are culturally appropriate for ${targetLanguage}
4. Are concise (1-10 words each)
5. Represent different response types (enthusiastic, neutral, polite decline)

Return ONLY a JSON array of 3 reply strings, nothing else.`,
          },
          {
            role: 'user',
            content: `Recent conversation:\n${conversationContext}\n\nIncoming message: "${incomingMessage}"\n\nGenerate 3 reply options.`,
          },
        ],
        temperature: 0.7,
        max_tokens: 500,
      });

      const responseText = completion.choices[0].message.content || '[]';
      const replies = JSON.parse(responseText);

      // Store analytics
      await db.collection('smartReplyAnalytics').add({
        userId,
        conversationId,
        incomingMessage,
        generatedReplies: replies,
        language: targetLanguage,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      return {
        replies,
        userStyle,
      };
    } catch (error) {
      console.error('Smart reply generation error:', error);
      throw new HttpsError('internal', 'Smart reply generation failed');
    }
  }
);

/**
 * Retrieve conversation context for RAG pipeline
 */
async function getConversationContext(
  conversationId: string,
  userId: string,
  limit: number
): Promise<string> {
  const messagesSnapshot = await db
    .collection('conversations')
    .doc(conversationId)
    .collection('messages')
    .orderBy('timestamp', 'desc')
    .limit(limit)
    .get();

  const messages = messagesSnapshot.docs
    .reverse()
    .map((doc) => {
      const data = doc.data();
      const isUser = data.senderId === userId;
      return `${isUser ? 'You' : 'Other'}: ${data.text}`;
    })
    .join('\n');

  return messages;
}

/**
 * Analyze user's communication style from their message history
 */
async function analyzeUserStyle(userId: string, context: string) {
  // Extract user's messages from context
  const userMessages = context
    .split('\n')
    .filter((line) => line.startsWith('You:'))
    .map((line) => line.substring(5));

  if (userMessages.length === 0) {
    return {
      tone: 'casual',
      emojiUsage: 'occasional',
      avgLength: 5,
      formality: 'casual',
    };
  }

  // Calculate average message length
  const avgLength = Math.round(
    userMessages.reduce((sum, msg) => sum + msg.split(' ').length, 0) / userMessages.length
  );

  // Detect emoji usage
  const emojiRegex = /[\u{1F600}-\u{1F64F}\u{1F300}-\u{1F5FF}\u{1F680}-\u{1F6FF}\u{2600}-\u{26FF}\u{2700}-\u{27BF}]/u;
  const emojiCount = userMessages.filter((msg) => emojiRegex.test(msg)).length;
  const emojiUsage = emojiCount / userMessages.length > 0.3 ? 'frequent' : 'occasional';

  // Determine tone and formality
  const tone = avgLength < 5 ? 'brief and casual' : 'conversational';
  const formality = avgLength > 15 || userMessages.some((msg) => msg.includes('please') || msg.includes('thank you'))
    ? 'formal'
    : 'casual';

  return {
    tone,
    emojiUsage,
    avgLength,
    formality,
  };
}

import * as admin from 'firebase-admin';

