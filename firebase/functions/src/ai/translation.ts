/**
 * Translation Functions
 * 
 * Provides real-time message translation and language detection
 */

import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { openai, MODELS, CONFIGS } from '../utils/openai';
import { getCachedTranslation, cacheTranslation } from '../utils/cache';
import { checkRateLimit } from '../utils/rateLimit';

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
    memory: '512MiB',
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
      // Check rate limit
      await checkRateLimit(request.auth.uid, 'translation', {
        maxRequests: 100,
        windowMinutes: 60,
      });

      // Check cache first
      const cached = await getCachedTranslation(text, targetLanguage);
      if (cached) {
        console.log('Cache hit for translation');
        return {
          translatedText: cached.translatedText,
          sourceLanguage: cached.sourceLanguage,
          targetLanguage: cached.targetLanguage,
          cached: true,
        };
      }

      console.log(`Translating text to ${targetLanguage}...`);

      // Call OpenAI for translation
      const completion = await openai.chat.completions.create({
        model: MODELS.TRANSLATION,
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
        temperature: CONFIGS.TRANSLATION.temperature,
        max_tokens: CONFIGS.TRANSLATION.maxTokens,
      });

      const translatedText = completion.choices[0].message.content || '';

      // Detect source language if not provided
      let detectedSourceLanguage = sourceLanguage || 'auto';
      if (!sourceLanguage && text.length > 5) {
        // Use GPT to detect language
        const langDetection = await detectLanguageInternal(text);
        detectedSourceLanguage = langDetection;
      }

      // Cache the translation
      await cacheTranslation(
        text,
        translatedText,
        detectedSourceLanguage,
        targetLanguage,
        request.auth.uid
      );

      console.log('Translation completed successfully');

      return {
        translatedText,
        sourceLanguage: detectedSourceLanguage,
        targetLanguage,
        cached: false,
      };
    } catch (error: any) {
      console.error('Translation error:', error);
      
      // Re-throw rate limit errors
      if (error.code === 'resource-exhausted') {
        throw error;
      }
      
      throw new HttpsError('internal', `Translation failed: ${error.message}`);
    }
  }
);

/**
 * Internal helper for language detection (used by translation)
 */
async function detectLanguageInternal(text: string): Promise<string> {
  try {
    const completion = await openai.chat.completions.create({
      model: MODELS.TRANSLATION,
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
      temperature: CONFIGS.LANGUAGE_DETECTION.temperature,
      max_tokens: CONFIGS.LANGUAGE_DETECTION.maxTokens,
    });

    return completion.choices[0].message.content?.trim().toLowerCase() || 'en';
  } catch (error) {
    console.error('Language detection error:', error);
    return 'auto';
  }
}

/**
 * Detect the language of a message (public API)
 */
export const detectLanguage = onCall<LanguageDetectionRequest>(
  {
    memory: '256MiB',
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
      // Check rate limit
      await checkRateLimit(request.auth.uid, 'languageDetection', {
        maxRequests: 200,
        windowMinutes: 60,
      });

      const languageCode = await detectLanguageInternal(text);

      return {
        languageCode,
        text,
      };
    } catch (error: any) {
      console.error('Language detection error:', error);
      
      // Re-throw rate limit errors
      if (error.code === 'resource-exhausted') {
        throw error;
      }
      
      throw new HttpsError('internal', 'Language detection failed');
    }
  }
);

