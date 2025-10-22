import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { openai, MODELS, CONFIGS } from '../utils/openai';
import { checkRateLimit } from '../utils/rateLimit';

/**
 * Request structure for data extraction
 */
interface DataExtractionRequest {
  text: string;
  messageId?: string;
  conversationId?: string;
}

/**
 * Extracted entity types
 */
export enum EntityType {
  ACTION_ITEM = 'ACTION_ITEM',
  DATE_TIME = 'DATE_TIME',
  CONTACT = 'CONTACT',
  LOCATION = 'LOCATION',
}

/**
 * Base extracted entity
 */
interface ExtractedEntity {
  type: EntityType;
  text: string;
  confidence: number;
  metadata: Record<string, any>;
}

/**
 * Action item entity
 */
interface ActionItemEntity extends ExtractedEntity {
  type: EntityType.ACTION_ITEM;
  metadata: {
    task: string;
    priority?: 'low' | 'medium' | 'high';
    assignedTo?: string;
    dueDate?: string;
  };
}

/**
 * Date/time entity
 */
interface DateTimeEntity extends ExtractedEntity {
  type: EntityType.DATE_TIME;
  metadata: {
    dateTime: string; // ISO 8601 format
    isRange: boolean;
    endDateTime?: string;
    description?: string;
  };
}

/**
 * Contact entity
 */
interface ContactEntity extends ExtractedEntity {
  type: EntityType.CONTACT;
  metadata: {
    name?: string;
    email?: string;
    phone?: string;
  };
}

/**
 * Location entity
 */
interface LocationEntity extends ExtractedEntity {
  type: EntityType.LOCATION;
  metadata: {
    address: string;
    latitude?: number;
    longitude?: number;
    placeName?: string;
  };
}

/**
 * Response structure
 */
interface DataExtractionResponse {
  entities: ExtractedEntity[];
  messageId?: string;
  conversationId?: string;
  extractedAt: number;
}

/**
 * Extract intelligent data from message text using GPT-4 with function calling
 */
export const extractIntelligentData = onCall<DataExtractionRequest>(
  {
    region: 'us-central1',
    memory: '512MiB',
    timeoutSeconds: 30,
  },
  async (request) => {
    // Verify authentication
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { text, messageId, conversationId } = request.data;

    // Validate input
    if (!text || typeof text !== 'string' || text.trim().length === 0) {
      throw new HttpsError('invalid-argument', 'Text is required and must be a non-empty string');
    }

    if (text.length > 10000) {
      throw new HttpsError('invalid-argument', 'Text must be less than 10,000 characters');
    }

    try {
      // Rate limiting: 50 extractions per hour
      await checkRateLimit(request.auth.uid, 'dataExtraction', {
        maxRequests: 50,
        windowMinutes: 60,
      });

      console.log(`Extracting data from text (user: ${request.auth.uid})`);

      // Call OpenAI with function calling to extract entities
      const completion = await openai.chat.completions.create({
        model: MODELS.DATA_EXTRACTION || 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `You are an intelligent data extraction assistant. Analyze the provided text and extract any relevant entities:
- Action items (tasks, todos, things to do)
- Dates and times (meetings, events, deadlines, appointments)
- Contact information (names with email addresses or phone numbers)
- Locations (addresses, places, venues)

For each entity, provide:
1. The exact text from the message
2. The entity type
3. Confidence score (0.0 to 1.0)
4. Structured metadata

Be conservative - only extract entities you're confident about. If there are no entities, return an empty array.`,
          },
          {
            role: 'user',
            content: text,
          },
        ],
        functions: [
          {
            name: 'extract_entities',
            description: 'Extract structured entities from text',
            parameters: {
              type: 'object',
              properties: {
                entities: {
                  type: 'array',
                  description: 'Array of extracted entities',
                  items: {
                    type: 'object',
                    properties: {
                      type: {
                        type: 'string',
                        enum: ['ACTION_ITEM', 'DATE_TIME', 'CONTACT', 'LOCATION'],
                        description: 'Type of entity',
                      },
                      text: {
                        type: 'string',
                        description: 'Exact text from message containing the entity',
                      },
                      confidence: {
                        type: 'number',
                        description: 'Confidence score from 0.0 to 1.0',
                        minimum: 0.0,
                        maximum: 1.0,
                      },
                      metadata: {
                        type: 'object',
                        description: 'Type-specific metadata',
                        properties: {
                          // Action item fields
                          task: { type: 'string' },
                          priority: { type: 'string', enum: ['low', 'medium', 'high'] },
                          assignedTo: { type: 'string' },
                          dueDate: { type: 'string' },
                          // Date/time fields
                          dateTime: { type: 'string' },
                          isRange: { type: 'boolean' },
                          endDateTime: { type: 'string' },
                          description: { type: 'string' },
                          // Contact fields
                          name: { type: 'string' },
                          email: { type: 'string' },
                          phone: { type: 'string' },
                          // Location fields
                          address: { type: 'string' },
                          latitude: { type: 'number' },
                          longitude: { type: 'number' },
                          placeName: { type: 'string' },
                        },
                      },
                    },
                    required: ['type', 'text', 'confidence', 'metadata'],
                  },
                },
              },
              required: ['entities'],
            },
          },
        ],
        function_call: { name: 'extract_entities' },
        temperature: CONFIGS.DATA_EXTRACTION?.temperature || 0.2,
        max_tokens: CONFIGS.DATA_EXTRACTION?.maxTokens || 1000,
      });

      // Parse function call response
      const functionCall = completion.choices[0].message.function_call;
      if (!functionCall || functionCall.name !== 'extract_entities') {
        throw new Error('Invalid function call response from OpenAI');
      }

      const extractedData = JSON.parse(functionCall.arguments);
      const entities: ExtractedEntity[] = extractedData.entities || [];

      console.log(`Extracted ${entities.length} entities`);

      const response: DataExtractionResponse = {
        entities,
        messageId,
        conversationId,
        extractedAt: Date.now(),
      };

      return response;
    } catch (error: any) {
      // Re-throw rate limit errors
      if (error.code === 'resource-exhausted') {
        throw error;
      }

      console.error('Data extraction error:', error);

      // Handle OpenAI API errors
      if (error.response?.status === 429) {
        throw new HttpsError('resource-exhausted', 'OpenAI API rate limit exceeded. Please try again later.');
      }

      if (error.response?.status === 401) {
        throw new HttpsError('internal', 'OpenAI API authentication failed');
      }

      throw new HttpsError('internal', `Data extraction failed: ${error.message}`);
    }
  }
);

/**
 * Batch extraction request
 */
interface BatchExtractionRequest {
  messages: Array<{
    id: string;
    text: string;
  }>;
  conversationId: string;
}

/**
 * Batch extraction response
 */
interface BatchExtractionResponse {
  results: Array<{
    messageId: string;
    entities: ExtractedEntity[];
  }>;
  conversationId: string;
  totalEntities: number;
  extractedAt: number;
}

/**
 * Extract data from multiple messages in a conversation
 */
export const extractBatchData = onCall<BatchExtractionRequest>(
  {
    region: 'us-central1',
    memory: '512MiB',
    timeoutSeconds: 60,
  },
  async (request) => {
    // Verify authentication
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { messages, conversationId } = request.data;

    // Validate input
    if (!Array.isArray(messages) || messages.length === 0) {
      throw new HttpsError('invalid-argument', 'Messages array is required and must not be empty');
    }

    if (messages.length > 50) {
      throw new HttpsError('invalid-argument', 'Cannot process more than 50 messages at once');
    }

    if (!conversationId || typeof conversationId !== 'string') {
      throw new HttpsError('invalid-argument', 'Conversation ID is required');
    }

    try {
      // Rate limiting: 10 batch extractions per hour (stricter than single)
      await checkRateLimit(request.auth.uid, 'batchDataExtraction', {
        maxRequests: 10,
        windowMinutes: 60,
      });

      console.log(`Batch extracting data from ${messages.length} messages (user: ${request.auth.uid})`);

      const results: Array<{ messageId: string; entities: ExtractedEntity[] }> = [];
      let totalEntities = 0;

      // Process each message
      for (const message of messages) {
        if (!message.id || !message.text || typeof message.text !== 'string') {
          console.warn(`Skipping invalid message:`, message);
          continue;
        }

        try {
          // Extract data from this message
          const extractionResult = await extractIntelligentData.run({
            data: {
              text: message.text,
              messageId: message.id,
              conversationId,
            },
            auth: request.auth,
            rawRequest: request.rawRequest,
          });

          if (extractionResult.data.entities.length > 0) {
            results.push({
              messageId: message.id,
              entities: extractionResult.data.entities,
            });
            totalEntities += extractionResult.data.entities.length;
          }
        } catch (error: any) {
          console.error(`Failed to extract from message ${message.id}:`, error.message);
          // Continue processing other messages even if one fails
        }
      }

      console.log(`Batch extraction complete: ${totalEntities} total entities from ${results.length} messages`);

      const response: BatchExtractionResponse = {
        results,
        conversationId,
        totalEntities,
        extractedAt: Date.now(),
      };

      return response;
    } catch (error: any) {
      // Re-throw rate limit errors
      if (error.code === 'resource-exhausted') {
        throw error;
      }

      console.error('Batch extraction error:', error);
      throw new HttpsError('internal', `Batch extraction failed: ${error.message}`);
    }
  }
);

