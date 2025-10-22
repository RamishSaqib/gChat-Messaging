/**
 * Audio Transcription Functions
 * 
 * Provides voice message transcription using OpenAI Whisper API
 */

import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { openai } from '../utils/openai';
import { checkRateLimit } from '../utils/rateLimit';
import * as admin from 'firebase-admin';
import axios from 'axios';
import { tmpdir } from 'os';
import { join } from 'path';
import { writeFile, unlink } from 'fs/promises';
import { randomBytes } from 'crypto';

interface TranscriptionRequest {
  audioUrl: string;
  messageId: string;
}

/**
 * Transcribe a voice message using OpenAI Whisper API
 */
export const transcribeVoiceMessage = onCall<TranscriptionRequest>(
  {
    memory: '1GiB',
    timeoutSeconds: 120,
    region: 'us-central1',
  },
  async (request) => {
    // Verify authentication
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { audioUrl, messageId } = request.data;

    // Validate input
    if (!audioUrl || !messageId) {
      throw new HttpsError('invalid-argument', 'Audio URL and message ID are required');
    }

    let tempFilePath: string | null = null;

    try {
      // Check rate limit (50 transcriptions per hour)
      await checkRateLimit(request.auth.uid, 'transcription', {
        maxRequests: 50,
        windowMinutes: 60,
      });

      console.log(`Transcribing audio for message ${messageId}...`);

      // Download audio file from Firebase Storage URL
      const response = await axios.get(audioUrl, {
        responseType: 'arraybuffer',
        timeout: 30000, // 30 seconds timeout
        maxContentLength: 10 * 1024 * 1024, // 10MB max
      });

      // Get content type to determine file extension
      const contentType = response.headers['content-type'] || 'audio/m4a';
      const extension = getFileExtension(contentType);

      // Save to temporary file
      const tempFileName = `${randomBytes(16).toString('hex')}.${extension}`;
      tempFilePath = join(tmpdir(), tempFileName);
      await writeFile(tempFilePath, response.data);

      console.log(`Audio file downloaded to ${tempFilePath}`);

      // Call OpenAI Whisper API
      const fs = require('fs');
      const transcription = await openai.audio.transcriptions.create({
        file: fs.createReadStream(tempFilePath),
        model: 'whisper-1',
        language: undefined, // Auto-detect language
        response_format: 'json',
        temperature: 0.2, // Lower temperature for more consistent transcriptions
      });

      console.log('Transcription completed successfully');

      // Store transcription in Firestore for caching
      const transcriptionData = {
        text: transcription.text,
        language: 'unknown', // Whisper doesn't return language in all response formats
        messageId,
        userId: request.auth.uid,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      await admin.firestore()
        .collection('transcriptions')
        .doc(messageId)
        .set(transcriptionData);

      // Clean up temp file
      if (tempFilePath) {
        await unlink(tempFilePath);
        tempFilePath = null;
      }

      return {
        text: transcription.text,
        language: 'unknown', // Whisper doesn't return language in all response formats
        messageId,
      };
    } catch (error: any) {
      console.error('Transcription error:', error);

      // Clean up temp file on error
      if (tempFilePath) {
        try {
          await unlink(tempFilePath);
        } catch (cleanupError) {
          console.error('Failed to cleanup temp file:', cleanupError);
        }
      }

      // Re-throw rate limit errors
      if (error.code === 'resource-exhausted') {
        throw error;
      }

      // Handle network errors
      if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
        throw new HttpsError('deadline-exceeded', 'Audio download timeout');
      }

      // Handle file size errors
      if (error.message && error.message.includes('maxContentLength')) {
        throw new HttpsError('invalid-argument', 'Audio file exceeds 10MB limit');
      }

      throw new HttpsError('internal', `Transcription failed: ${error.message}`);
    }
  }
);

/**
 * Get the transcription for a message (from cache)
 */
export const getTranscription = onCall<{ messageId: string }>(
  {
    memory: '256MiB',
    timeoutSeconds: 30,
    region: 'us-central1',
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { messageId } = request.data;

    if (!messageId) {
      throw new HttpsError('invalid-argument', 'Message ID is required');
    }

    try {
      const transcriptionDoc = await admin.firestore()
        .collection('transcriptions')
        .doc(messageId)
        .get();

      if (!transcriptionDoc.exists) {
        throw new HttpsError('not-found', 'Transcription not found');
      }

      const data = transcriptionDoc.data();

      return {
        text: data?.text || '',
        language: data?.language || 'unknown',
        messageId,
        cached: true,
      };
    } catch (error: any) {
      console.error('Get transcription error:', error);

      if (error.code === 'not-found') {
        throw error;
      }

      throw new HttpsError('internal', 'Failed to retrieve transcription');
    }
  }
);

/**
 * Helper function to determine file extension from MIME type
 */
function getFileExtension(mimeType: string): string {
  const mimeToExt: { [key: string]: string } = {
    'audio/m4a': 'm4a',
    'audio/mp4': 'm4a',
    'audio/x-m4a': 'm4a',
    'audio/mpeg': 'mp3',
    'audio/mp3': 'mp3',
    'audio/wav': 'wav',
    'audio/x-wav': 'wav',
    'audio/wave': 'wav',
    'audio/ogg': 'ogg',
    'audio/webm': 'webm',
  };

  return mimeToExt[mimeType.toLowerCase()] || 'm4a';
}

