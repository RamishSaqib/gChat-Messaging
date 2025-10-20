/**
 * Translation Functions
 * 
 * Provides real-time message translation and language detection
 */

import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';
import { getFirestore } from 'firebase-admin/firestore';

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

const db = getFirestore();

interface TranslationRequest {
  text: string;
  sourceLanguage: string;
  targetLanguage: string;
}

interface LanguageDetectionRequest {
  text: string;
}

/**
 * Translate a message from one language to another
 */
export const translateMessage = onCall<TranslationRequest>(
  {
    memory: '512MB',
    timeoutSeconds: 60,
    region: 'us-central1',
  },
  async (request) => {
    // Verify authentication
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { text, sourceLanguage, targetLanguage } = request.data;

    // Validate input
    if (!text || !targetLanguage) {
      throw new HttpsError('invalid-argument', 'Text and target language are required');
    }

    if (text.length > 10000) {
      throw new HttpsError('invalid-argument', 'Text exceeds maximum length of 10000 characters');
    }

    try {
      // Check cache first
      const cacheKey = `${text}_${targetLanguage}`;
      const cacheRef = db.collection('translations').doc(cacheKey);
      const cached = await cacheRef.get();

      if (cached.exists) {
        const cacheData = cached.data();
        return {
          translatedText: cacheData?.translatedText,
          sourceLanguage: cacheData?.sourceLanguage,
          targetLanguage: cacheData?.targetLanguage,
          cached: true,
        };
      }

      // Call OpenAI for translation
      const completion = await openai.chat.completions.create({
        model: process.env.OPENAI_MODEL || 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `You are a professional translator. Translate text from ${sourceLanguage || 'detected language'} to ${targetLanguage}. 
Maintain the original tone, context, and intent. Provide natural, conversational translations. 
Do not add explanations or notes - only return the translated text.`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        temperature: 0.3, // Lower temperature for consistency
        max_tokens: 2000,
      });

      const translatedText = completion.choices[0].message.content || '';

      // Cache the translation
      await cacheRef.set({
        originalText: text,
        translatedText,
        sourceLanguage: sourceLanguage || 'auto',
        targetLanguage,
        cachedAt: admin.firestore.FieldValue.serverTimestamp(),
        userId: request.auth.uid,
      });

      return {
        translatedText,
        sourceLanguage: sourceLanguage || 'auto',
        targetLanguage,
        cached: false,
      };
    } catch (error) {
      console.error('Translation error:', error);
      throw new HttpsError('internal', 'Translation failed');
    }
  }
);

/**
 * Detect the language of a message
 */
export const detectLanguage = onCall<LanguageDetectionRequest>(
  {
    memory: '256MB',
    timeoutSeconds: 30,
    region: 'us-central1',
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { text } = request.data;

    if (!text) {
      throw new HttpsError('invalid-argument', 'Text is required');
    }

    try {
      const completion = await openai.chat.completions.create({
        model: 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `Detect the language of the following text. Return ONLY the ISO 639-1 language code (e.g., "en", "es", "fr", "ja", "zh", "ar"). 
If multiple languages are present, return the primary language. Do not return anything except the 2-letter code.`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        temperature: 0.1,
        max_tokens: 10,
      });

      const languageCode = completion.choices[0].message.content?.trim().toLowerCase() || 'en';

      return {
        languageCode,
        text,
      };
    } catch (error) {
      console.error('Language detection error:', error);
      throw new HttpsError('internal', 'Language detection failed');
    }
  }
);

import * as admin from 'firebase-admin';

