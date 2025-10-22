/**
 * gChat Cloud Functions
 * 
 * AI-powered translation, smart replies, and cultural context features
 */

import * as admin from 'firebase-admin';

// Initialize Firebase Admin
admin.initializeApp();

// Export all functions
export { translateMessage, detectLanguage } from './ai/translation';
export { generateSmartReplies } from './ai/smartReply';
export { getCulturalContext, adjustFormality } from './ai/culturalContext';
export { extractIntelligentData, extractBatchData } from './ai/dataExtraction';
export { onMessageCreated } from './triggers/onMessageCreated';

// Health check function
import { onRequest } from 'firebase-functions/v2/https';

export const healthCheck = onRequest((req, res) => {
  res.status(200).json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    service: 'gChat Cloud Functions',
    version: '1.0.0'
  });
});

