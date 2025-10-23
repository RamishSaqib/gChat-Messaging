package com.gchat.data.repository

import com.gchat.data.remote.firebase.FirebaseSmartReplyDataSource
import com.gchat.domain.model.SmartReply
import com.gchat.domain.model.UserCommunicationStyle
import com.gchat.domain.repository.SmartReplyRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SmartReplyRepository
 * 
 * Includes in-memory caching to avoid duplicate API calls for the same message
 */
@Singleton
class SmartReplyRepositoryImpl @Inject constructor(
    private val firebaseSmartReplyDataSource: FirebaseSmartReplyDataSource
) : SmartReplyRepository {
    
    // In-memory cache for smart replies
    // Key: messageId, Value: CacheEntry with replies and timestamp
    private val replyCache = mutableMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()
    
    // Cache TTL: 5 minutes (300,000 ms)
    private val cacheTtlMs = 300_000L
    
    override suspend fun generateReplies(
        conversationId: String,
        incomingMessageId: String,
        targetLanguage: String
    ): Result<List<SmartReply>> {
        return try {
            // Check cache first
            cacheMutex.withLock {
                val cached = replyCache[incomingMessageId]
                if (cached != null && !cached.isExpired()) {
                    android.util.Log.d("SmartReplyRepository", "Cache hit for message: $incomingMessageId")
                    return Result.success(cached.replies)
                }
            }
            
            // Cache miss - fetch from Cloud Function
            android.util.Log.d("SmartReplyRepository", "Cache miss, fetching from Cloud Function...")
            
            val result = firebaseSmartReplyDataSource.generateSmartReplies(
                conversationId = conversationId,
                incomingMessageId = incomingMessageId,
                targetLanguage = targetLanguage
            )
            
            result.onSuccess { response ->
                // Cache the replies
                cacheMutex.withLock {
                    replyCache[incomingMessageId] = CacheEntry(
                        replies = response.replies,
                        userStyle = response.userStyle,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // Clean up old cache entries (keep cache size manageable)
                    cleanupExpiredEntries()
                }
                
                android.util.Log.d("SmartReplyRepository", "Cached ${response.replies.size} smart replies")
            }
            
            result.map { it.replies }
        } catch (e: Exception) {
            android.util.Log.e("SmartReplyRepository", "Error generating smart replies", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserStyle(
        conversationId: String
    ): Result<UserCommunicationStyle> {
        // For now, return a cached style if available from any message in this conversation
        // In a more advanced implementation, we could aggregate styles across all messages
        cacheMutex.withLock {
            val cachedEntry = replyCache.values.firstOrNull { 
                it.userStyle != null && !it.isExpired() 
            }
            
            if (cachedEntry?.userStyle != null) {
                return Result.success(cachedEntry.userStyle)
            }
        }
        
        // No cached style available - would need to be fetched during reply generation
        return Result.failure(Exception("User style not available. Generate smart replies first."))
    }
    
    /**
     * Remove expired entries from cache to prevent memory bloat
     */
    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        val expiredKeys = replyCache.filter { (_, entry) ->
            now - entry.timestamp > cacheTtlMs
        }.keys
        
        expiredKeys.forEach { replyCache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            android.util.Log.d("SmartReplyRepository", "Cleaned up ${expiredKeys.size} expired cache entries")
        }
    }
    
    /**
     * Clear all cached replies (e.g., when user logs out)
     */
    suspend fun clearCache() {
        cacheMutex.withLock {
            replyCache.clear()
            android.util.Log.d("SmartReplyRepository", "Cache cleared")
        }
    }
    
    /**
     * Cache entry for smart replies
     */
    private data class CacheEntry(
        val replies: List<SmartReply>,
        val userStyle: UserCommunicationStyle?,
        val timestamp: Long
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > 300_000L // 5 minutes
        }
    }
}

