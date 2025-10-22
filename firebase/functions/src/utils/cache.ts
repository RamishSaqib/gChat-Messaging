/**
 * Cache Utility Module
 * 
 * Handles caching of translations and AI responses in Firestore
 */

import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import * as crypto from 'crypto';

const db = getFirestore();

/**
 * Generate a deterministic hash for cache keys
 */
export function generateCacheKey(text: string, ...params: string[]): string {
  const combined = [text, ...params].join('|');
  return crypto.createHash('sha256').update(combined).digest('hex');
}

/**
 * Get cached translation
 */
export async function getCachedTranslation(
  text: string,
  targetLanguage: string
): Promise<any | null> {
  try {
    const cacheKey = generateCacheKey(text, targetLanguage);
    const cacheRef = db.collection('translations').doc(cacheKey);
    const cached = await cacheRef.get();

    if (!cached.exists) {
      return null;
    }

    const data = cached.data();
    
    // Check if cache is expired (30 days TTL)
    const cachedAt = data?.cachedAt?.toDate();
    if (cachedAt) {
      const now = new Date();
      const daysSince = (now.getTime() - cachedAt.getTime()) / (1000 * 60 * 60 * 24);
      
      if (daysSince > 30) {
        // Cache expired, delete it
        await cacheRef.delete();
        return null;
      }
    }

    return data;
  } catch (error) {
    console.error('Cache read error:', error);
    return null; // Fail gracefully
  }
}

/**
 * Cache a translation
 */
export async function cacheTranslation(
  text: string,
  translatedText: string,
  sourceLanguage: string,
  targetLanguage: string,
  userId: string
): Promise<void> {
  try {
    const cacheKey = generateCacheKey(text, targetLanguage);
    const cacheRef = db.collection('translations').doc(cacheKey);

    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30); // 30 days from now

    await cacheRef.set({
      originalText: text,
      translatedText,
      sourceLanguage,
      targetLanguage,
      cachedAt: FieldValue.serverTimestamp(),
      expiresAt: expiresAt,
      userId,
      hitCount: 1,
    }, { merge: true });
  } catch (error) {
    console.error('Cache write error:', error);
    // Don't throw - caching failure shouldn't break the request
  }
}

/**
 * Increment cache hit count
 */
export async function incrementCacheHit(cacheKey: string): Promise<void> {
  try {
    const cacheRef = db.collection('translations').doc(cacheKey);
    await cacheRef.update({
      hitCount: FieldValue.increment(1),
      lastAccessedAt: FieldValue.serverTimestamp(),
    });
  } catch (error) {
    console.error('Cache hit increment error:', error);
  }
}

