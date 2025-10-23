package com.gchat.data.repository

import com.gchat.data.remote.firebase.FirebaseFormalityDataSource
import com.gchat.domain.model.FormalityLevel
import com.gchat.domain.repository.FormalityRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FormalityRepository.
 * 
 * Coordinates formality adjustment operations with Firebase Cloud Functions.
 * Includes in-memory caching to reduce API calls for frequently adjusted text.
 */
@Singleton
class FormalityRepositoryImpl @Inject constructor(
    private val firebaseFormalityDataSource: FirebaseFormalityDataSource
) : FormalityRepository {
    
    // Simple in-memory cache: key = "$text-$language-$formality", value = (adjustedText, timestamp)
    private val cache = mutableMapOf<String, Pair<String, Long>>()
    private val cacheMutex = Mutex()
    private val CACHE_TTL_MS = 10 * 60 * 1000 // 10 minutes
    
    override suspend fun adjustFormality(
        text: String,
        language: String,
        targetFormality: FormalityLevel
    ): Result<String> = cacheMutex.withLock {
        val cacheKey = "$text-$language-${targetFormality.name}"
        val cachedEntry = cache[cacheKey]
        
        // Check cache
        if (cachedEntry != null && System.currentTimeMillis() - cachedEntry.second < CACHE_TTL_MS) {
            return Result.success(cachedEntry.first)
        }
        
        // Cache miss - call Firebase
        return firebaseFormalityDataSource.adjustFormality(text, language, targetFormality)
            .onSuccess { adjustedText ->
                // Cache the result
                cache[cacheKey] = Pair(adjustedText, System.currentTimeMillis())
                
                // Clean up old cache entries (keep cache size reasonable)
                if (cache.size > 50) {
                    val now = System.currentTimeMillis()
                    cache.entries.removeIf { now - it.value.second > CACHE_TTL_MS }
                }
            }
    }
}

