package com.gchat.domain.repository

/**
 * Repository for managing auto-translate settings
 * 
 * Handles both global user settings and per-conversation overrides
 */
interface AutoTranslateRepository {
    
    /**
     * Set global auto-translate preference for a user
     * 
     * @param userId User ID
     * @param enabled Whether auto-translate is enabled globally
     * @return Result indicating success or failure
     */
    suspend fun setGlobalAutoTranslate(userId: String, enabled: Boolean): Result<Unit>
    
    /**
     * Set auto-translate preference for a specific conversation
     * 
     * @param conversationId Conversation ID
     * @param enabled Whether auto-translate is enabled for this conversation
     * @return Result indicating success or failure
     */
    suspend fun setConversationAutoTranslate(conversationId: String, enabled: Boolean): Result<Unit>
    
    /**
     * Determine if auto-translate should be enabled for a conversation
     * 
     * Logic: Per-conversation setting takes precedence over global setting
     * - If conversation has explicit setting (true/false), use that
     * - Otherwise, fallback to user's global setting
     * 
     * @param conversationId Conversation ID
     * @param userId User ID
     * @return True if auto-translate should be enabled
     */
    suspend fun shouldAutoTranslate(conversationId: String, userId: String): Boolean
}

