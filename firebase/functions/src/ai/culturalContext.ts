import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { openai, MODELS } from '../utils/openai';
import { checkRateLimit } from '../utils/rateLimit';
import * as crypto from 'crypto';

interface CulturalContextRequest {
  messageId: string;
  text: string;
  language: string;
  mode?: 'all' | 'slang' | 'idioms'; // 'all' by default, for PR #18 slang mode
}

interface CulturalContextItem {
  phrase: string;
  literalTranslation?: string;
  actualMeaning: string;
  culturalContext: string;
  examples?: string[];
}

interface CulturalContextResponse {
  messageId: string;
  contexts: CulturalContextItem[];
  language: string;
  cached: boolean;
}

/**
 * Cloud Function: getCulturalContext
 * 
 * Analyzes text for idioms, slang, cultural references, and expressions
 * that may not translate literally. Uses GPT-4 for contextual understanding.
 * 
 * Features:
 * - Detects idioms, slang, and cultural references
 * - Provides literal translations and actual meanings
 * - Explains cultural context and usage
 * - Caches results in Firestore (30-day TTL)
 * - Rate limiting: 100 requests/hour per user
 */
export const getCulturalContext = onCall<CulturalContextRequest>(
  { 
    cors: true,
    region: 'us-central1',
  },
  async (request) => {
    const { messageId, text, language, mode = 'all' } = request.data;
    const userId = request.auth?.uid;

    // Validate auth
    if (!userId) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    // Validate input
    if (!text || text.trim().length === 0) {
      throw new HttpsError('invalid-argument', 'Text is required');
    }

    if (!language) {
      throw new HttpsError('invalid-argument', 'Language is required');
    }

    if (!messageId) {
      throw new HttpsError('invalid-argument', 'Message ID is required');
    }

    // Check rate limit: 100 requests per hour
    try {
      await checkRateLimit(userId, 'cultural-context', {
        maxRequests: 100,
        windowMinutes: 60
      });
    } catch (error: any) {
      // checkRateLimit throws HttpsError if limit exceeded
      throw error;
    }

    // Generate cache key using hash of text + language + mode
    const textHash = crypto
      .createHash('sha256')
      .update(`${text}-${language}-${mode}`)
      .digest('hex');

    const db = getFirestore();
    const cacheRef = db.collection('culturalContexts').doc(textHash);

    try {
      // Check cache first
      const cachedDoc = await cacheRef.get();
      if (cachedDoc.exists) {
        const cachedData = cachedDoc.data();
        const now = Date.now();
        const cacheAge = now - cachedData!.timestamp;
        const CACHE_TTL = 30 * 24 * 60 * 60 * 1000; // 30 days in milliseconds

        if (cacheAge < CACHE_TTL) {
          console.log(`Cache hit for cultural context: ${textHash.substring(0, 8)}`);
          return {
            messageId,
            contexts: cachedData!.contexts,
            language,
            cached: true,
          } as CulturalContextResponse;
        } else {
          // Cache expired, delete it
          await cacheRef.delete();
        }
      }

      console.log(`Cache miss, generating cultural context for: "${text.substring(0, 50)}..."`);

      // Build GPT-4 prompt based on mode
      let systemPrompt: string;
      let userPrompt: string;

      if (mode === 'slang') {
        systemPrompt = `You are an expert linguist specializing in modern slang, internet language, and regional dialects. Analyze text and identify slang terms, memes, and informal expressions.`;
        userPrompt = `Analyze the following ${language} text and identify any slang, internet memes, Gen Z language, or informal expressions.

For each slang term found, provide:
1. The term/phrase (exact text from message)
2. Actual meaning/definition
3. Context of use (when and how it's typically used)
4. Examples of usage (1-2 examples)

If there are no slang terms, return an empty array.

Text: "${text}"

Return ONLY valid JSON in this exact format, no markdown or code blocks:
{
  "contexts": [
    {
      "phrase": "the slang term",
      "actualMeaning": "what it means",
      "culturalContext": "when and how it's used",
      "examples": ["example 1", "example 2"]
    }
  ]
}`;
      } else if (mode === 'idioms') {
        systemPrompt = `You are an expert linguist specializing in idioms and figurative language. Analyze text and identify idiomatic expressions.`;
        userPrompt = `Analyze the following ${language} text and identify any idioms or figurative expressions.

For each idiom found, provide:
1. The phrase/expression (exact text from message)
2. Literal translation (what it would mean word-for-word)
3. Actual meaning
4. Cultural context and origin
5. Examples of usage (1-2 examples)

If there are no idioms, return an empty array.

Text: "${text}"

Return ONLY valid JSON in this exact format, no markdown or code blocks:
{
  "contexts": [
    {
      "phrase": "the idiom",
      "literalTranslation": "word-for-word meaning",
      "actualMeaning": "actual meaning",
      "culturalContext": "origin and usage notes",
      "examples": ["example 1", "example 2"]
    }
  ]
}`;
      } else {
        // mode === 'all' (default)
        systemPrompt = `You are an expert linguist specializing in cultural communication. Analyze text and identify idioms, slang, cultural references, and expressions that may not translate literally.`;
        userPrompt = `Analyze the following ${language} text and identify any idioms, slang, cultural references, or expressions that may not translate literally.

For each expression found, provide:
1. The phrase/expression (exact text from message)
2. Literal translation (if applicable, what it would mean word-for-word)
3. Actual meaning
4. Cultural context and usage notes
5. Examples of usage (1-2 examples, optional)

If there are no special expressions, return an empty array.

Text: "${text}"

Return ONLY valid JSON in this exact format, no markdown or code blocks:
{
  "contexts": [
    {
      "phrase": "the expression",
      "literalTranslation": "word-for-word meaning (optional)",
      "actualMeaning": "actual meaning",
      "culturalContext": "cultural context and usage",
      "examples": ["example 1", "example 2"]
    }
  ]
}`;
      }

      // Call GPT-4
      const completion = await openai.chat.completions.create({
        model: MODELS.GPT4,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt },
        ],
        temperature: 0.3, // Lower temperature for more consistent, factual responses
        max_tokens: 1000,
      });

      const responseText = completion.choices[0]?.message?.content;
      if (!responseText) {
        throw new HttpsError('internal', 'No response from AI model');
      }

      // Parse JSON response
      let parsedResponse: { contexts: CulturalContextItem[] };
      try {
        // Remove markdown code blocks if present
        const cleanedResponse = responseText
          .replace(/```json\n?/g, '')
          .replace(/```\n?/g, '')
          .trim();
        parsedResponse = JSON.parse(cleanedResponse);
      } catch (parseError) {
        console.error('Failed to parse AI response:', responseText);
        throw new HttpsError('internal', 'Failed to parse AI response');
      }

      // Validate response structure
      if (!Array.isArray(parsedResponse.contexts)) {
        throw new HttpsError('internal', 'Invalid response format from AI');
      }

      // Cache the result in Firestore
      await cacheRef.set({
        text,
        language,
        mode,
        contexts: parsedResponse.contexts,
        timestamp: Date.now(),
        expiresAt: FieldValue.serverTimestamp(),
      });

      console.log(`Cultural context generated and cached: ${parsedResponse.contexts.length} items found`);

      return {
        messageId,
        contexts: parsedResponse.contexts,
        language,
        cached: false,
      } as CulturalContextResponse;

    } catch (error: any) {
      console.error('Cultural context error:', error);
      
      if (error instanceof HttpsError) {
        throw error;
      }

      // Handle OpenAI API errors
      if (error.status === 429) {
        throw new HttpsError('resource-exhausted', 'AI service rate limit exceeded. Please try again later.');
      }

      throw new HttpsError('internal', `Failed to get cultural context: ${error.message}`);
    }
  }
);

/**
 * Cloud Function: adjustFormality
 * 
 * Rewrites text to match a desired formality level (casual, neutral, formal).
 * Used for PR #17: Formality Level Adjustment.
 * 
 * Features:
 * - Casual: contractions, informal language, slang, emojis
 * - Neutral: standard conversational tone
 * - Formal: professional language, no contractions, polite phrasing
 * - Rate limiting: 50 requests/hour per user
 * - Caches results (text + formality level as key)
 */
export const adjustFormality = onCall<{
  text: string;
  language: string;
  targetFormality: 'casual' | 'neutral' | 'formal';
}>(
  {
    cors: true,
    region: 'us-central1',
  },
  async (request) => {
    const { text, language, targetFormality } = request.data;
    const userId = request.auth?.uid;

    // Validate auth
    if (!userId) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    // Validate input
    if (!text || text.trim().length === 0) {
      throw new HttpsError('invalid-argument', 'Text is required');
    }

    if (!language) {
      throw new HttpsError('invalid-argument', 'Language is required');
    }

    if (!['casual', 'neutral', 'formal'].includes(targetFormality)) {
      throw new HttpsError('invalid-argument', 'Invalid formality level');
    }

    // Check rate limit: 50 requests per hour
    try {
      await checkRateLimit(userId, 'formality', {
        maxRequests: 50,
        windowMinutes: 60
      });
    } catch (error: any) {
      // checkRateLimit throws HttpsError if limit exceeded
      throw error;
    }

    // Generate cache key
    const cacheKey = crypto
      .createHash('sha256')
      .update(`${text}-${language}-${targetFormality}`)
      .digest('hex');

    const db = getFirestore();
    const cacheRef = db.collection('formalityAdjustments').doc(cacheKey);

    try {
      // Check cache first
      const cachedDoc = await cacheRef.get();
      if (cachedDoc.exists) {
        const cachedData = cachedDoc.data();
        const now = Date.now();
        const cacheAge = now - cachedData!.timestamp;
        const CACHE_TTL = 7 * 24 * 60 * 60 * 1000; // 7 days

        if (cacheAge < CACHE_TTL) {
          console.log(`Cache hit for formality adjustment: ${cacheKey.substring(0, 8)}`);
          return {
            adjustedText: cachedData!.adjustedText,
            cached: true,
          };
        } else {
          await cacheRef.delete();
        }
      }

      console.log(`Adjusting formality to ${targetFormality} for: "${text.substring(0, 50)}..."`);

      // Build GPT-4 prompt
      const formalityRules: { [key: string]: string } = {
        casual: 'Use contractions (e.g., "can\'t", "won\'t"), informal language, slang when appropriate, and emojis if fitting. Keep the tone friendly and relaxed.',
        neutral: 'Use standard conversational tone. Balanced language that\'s neither too formal nor too casual. Clear and professional but approachable.',
        formal: 'Use professional language, no contractions (e.g., "cannot" instead of "can\'t"), polite and respectful phrasing. Maintain a professional tone suitable for business communication.',
      };

      const systemPrompt = `You are an expert in ${language} language and communication styles. Rewrite text to match specific formality levels while preserving the original meaning and intent.`;
      
      const userPrompt = `Rewrite the following ${language} text to match a ${targetFormality.toUpperCase()} tone.

Rules for ${targetFormality} tone:
${formalityRules[targetFormality]}

Important:
- Preserve the core meaning and intent
- Keep the same person/perspective (1st, 2nd, 3rd)
- Maintain similar length (don't make it much longer or shorter)
- Return ONLY the rewritten text, no explanations or comments

Original text: "${text}"

Rewritten text:`;

      // Call GPT-4
      const completion = await openai.chat.completions.create({
        model: MODELS.GPT4,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt },
        ],
        temperature: 0.7,
        max_tokens: 500,
      });

      const adjustedText = completion.choices[0]?.message?.content?.trim();
      if (!adjustedText) {
        throw new HttpsError('internal', 'No response from AI model');
      }

      // Cache the result
      await cacheRef.set({
        originalText: text,
        language,
        targetFormality,
        adjustedText,
        timestamp: Date.now(),
      });

      console.log(`Formality adjusted successfully`);

      return {
        adjustedText,
        cached: false,
      };

    } catch (error: any) {
      console.error('Formality adjustment error:', error);
      
      if (error instanceof HttpsError) {
        throw error;
      }

      if (error.status === 429) {
        throw new HttpsError('resource-exhausted', 'AI service rate limit exceeded. Please try again later.');
      }

      throw new HttpsError('internal', `Failed to adjust formality: ${error.message}`);
    }
  }
);
