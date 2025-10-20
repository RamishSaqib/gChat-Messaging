/**
 * Cultural Context Functions
 * 
 * Provides cultural insights, idiom explanations, and formality adjustments
 */

import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

interface CulturalContextRequest {
  text: string;
  language: string;
}

interface FormalityAdjustmentRequest {
  text: string;
  sourceLanguage: string;
  targetLanguage: string;
  formalityLevel: 'formal' | 'casual';
}

/**
 * Get cultural context for idioms, slang, and cultural references
 */
export const getCulturalContext = onCall<CulturalContextRequest>(
  {
    memory: '256MB',
    timeoutSeconds: 30,
    region: 'us-central1',
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { text, language } = request.data;

    if (!text || !language) {
      throw new HttpsError('invalid-argument', 'Text and language are required');
    }

    try {
      const completion = await openai.chat.completions.create({
        model: 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `Analyze the following ${language} text and identify any idioms, slang, cultural references, or expressions that may not translate literally.

For each identified phrase, provide:
1. The phrase itself
2. Literal translation (if applicable)
3. Actual meaning
4. Cultural context and usage notes
5. Whether it's formal, casual, or slang

Return the results as a JSON array. If there are no special expressions, return an empty array.

Example output:
[
  {
    "phrase": "break a leg",
    "literalTranslation": "romperse una pierna",
    "actualMeaning": "good luck",
    "context": "English idiom used to wish someone success, especially before a performance",
    "formality": "casual"
  }
]`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        temperature: 0.3,
        max_tokens: 1000,
      });

      const responseText = completion.choices[0].message.content || '[]';
      const culturalInsights = JSON.parse(responseText);

      return {
        text,
        language,
        insights: culturalInsights,
        hasInsights: culturalInsights.length > 0,
      };
    } catch (error) {
      console.error('Cultural context error:', error);
      throw new HttpsError('internal', 'Cultural context analysis failed');
    }
  }
);

/**
 * Adjust message formality level for appropriate cross-cultural communication
 */
export const adjustFormality = onCall<FormalityAdjustmentRequest>(
  {
    memory: '256MB',
    timeoutSeconds: 30,
    region: 'us-central1',
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { text, sourceLanguage, targetLanguage, formalityLevel } = request.data;

    if (!text || !targetLanguage || !formalityLevel) {
      throw new HttpsError('invalid-argument', 'Text, target language, and formality level are required');
    }

    try {
      const formalityInstructions = formalityLevel === 'formal'
        ? 'Use formal language, honorifics, and polite expressions appropriate for professional or respectful communication.'
        : 'Use casual, conversational language appropriate for friends and informal settings.';

      const completion = await openai.chat.completions.create({
        model: 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `You are a professional translator specializing in cultural communication. Translate the following text from ${sourceLanguage} to ${targetLanguage}.

${formalityInstructions}

For languages with formal/informal registers (like Japanese keigo, Spanish usted/tú, German Sie/du, etc.), strictly adhere to the ${formalityLevel} level.

Return ONLY the translated text with appropriate formality, nothing else.`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        temperature: 0.3,
        max_tokens: 1000,
      });

      const adjustedText = completion.choices[0].message.content || '';

      return {
        originalText: text,
        adjustedText,
        sourceLanguage,
        targetLanguage,
        formalityLevel,
      };
    } catch (error) {
      console.error('Formality adjustment error:', error);
      throw new HttpsError('internal', 'Formality adjustment failed');
    }
  }
);

