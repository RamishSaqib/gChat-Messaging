/**
 * OpenAI Utility Module
 * 
 * Centralized OpenAI client configuration and helper functions
 */

import { OpenAI } from 'openai';

// Lazy-initialize OpenAI client to avoid errors during deployment
let _openai: OpenAI | null = null;

export function getOpenAI(): OpenAI {
  if (!_openai) {
    _openai = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY,
    });
  }
  return _openai;
}

// Export for backward compatibility
export const openai = new Proxy({} as OpenAI, {
  get(target, prop) {
    return (getOpenAI() as any)[prop];
  }
});

// Default models
export const MODELS = {
  TRANSLATION: process.env.OPENAI_MODEL || 'gpt-4-turbo-preview',
  ASSISTANT: 'gpt-4-turbo-preview',
  DATA_EXTRACTION: 'gpt-4-turbo-preview',
  GPT4: 'gpt-4-turbo-preview',
};

// Default configurations
export const CONFIGS = {
  TRANSLATION: {
    temperature: 0.3, // Low for consistency
    maxTokens: 2000,
  },
  LANGUAGE_DETECTION: {
    temperature: 0.1, // Very low for deterministic output
    maxTokens: 10,
  },
  ASSISTANT: {
    temperature: 0.7, // Higher for natural conversation
    maxTokens: 1000,
  },
  DATA_EXTRACTION: {
    temperature: 0.2, // Low for accurate extraction
    maxTokens: 1500,
  },
};

/**
 * Call OpenAI with error handling and retries
 */
export async function callOpenAI(
  messages: OpenAI.Chat.ChatCompletionMessageParam[],
  config: {
    model?: string;
    temperature?: number;
    maxTokens?: number;
    responseFormat?: { type: 'json_object' };
  } = {}
): Promise<string> {
  try {
    const completion = await openai.chat.completions.create({
      model: config.model || MODELS.TRANSLATION,
      messages,
      temperature: config.temperature || 0.3,
      max_tokens: config.maxTokens || 1000,
      response_format: config.responseFormat,
    });

    return completion.choices[0].message.content || '';
  } catch (error: any) {
    console.error('OpenAI API error:', error);
    throw new Error(`OpenAI API failed: ${error.message}`);
  }
}

