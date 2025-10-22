/**
 * Rate Limiting Utility
 * 
 * Prevents abuse of AI features
 */

import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { HttpsError } from 'firebase-functions/v2/https';

const db = getFirestore();

interface RateLimitConfig {
  maxRequests: number;
  windowMinutes: number;
}

const DEFAULT_LIMIT: RateLimitConfig = {
  maxRequests: 100,
  windowMinutes: 60, // 1 hour
};

/**
 * Check if user has exceeded rate limit
 * Returns true if allowed, throws HttpsError if limit exceeded
 */
export async function checkRateLimit(
  userId: string,
  feature: string,
  config: RateLimitConfig = DEFAULT_LIMIT
): Promise<boolean> {
  try {
    const rateLimitRef = db
      .collection('rateLimits')
      .doc(userId)
      .collection('features')
      .doc(feature);

    const doc = await rateLimitRef.get();
    const now = Date.now();
    const windowMs = config.windowMinutes * 60 * 1000;

    if (!doc.exists) {
      // First request - create document
      await rateLimitRef.set({
        requests: [now],
        lastReset: now,
      });
      return true;
    }

    const data = doc.data();
    const requests: number[] = data?.requests || [];
    const lastReset: number = data?.lastReset || now;

    // Remove requests outside the time window
    const recentRequests = requests.filter((timestamp) => now - timestamp < windowMs);

    // Check if limit exceeded
    if (recentRequests.length >= config.maxRequests) {
      const oldestRequest = Math.min(...recentRequests);
      const resetIn = Math.ceil((windowMs - (now - oldestRequest)) / 1000 / 60); // minutes
      
      throw new HttpsError(
        'resource-exhausted',
        `Rate limit exceeded. Try again in ${resetIn} minutes.`
      );
    }

    // Add current request and update
    recentRequests.push(now);
    await rateLimitRef.set({
      requests: recentRequests,
      lastReset: now > lastReset + windowMs ? now : lastReset,
    });

    return true;
  } catch (error: any) {
    // Re-throw HttpsError
    if (error.code === 'resource-exhausted') {
      throw error;
    }
    
    // Log other errors but allow request (fail open)
    console.error('Rate limit check error:', error);
    return true;
  }
}

