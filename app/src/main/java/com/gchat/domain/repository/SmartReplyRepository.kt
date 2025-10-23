package com.gchat.domain.repository

import com.gchat.domain.model.SmartReply
import com.gchat.domain.model.UserCommunicationStyle

/**
 * Repository interface for smart reply generation
 * 
 * Generates context-aware, personalized reply suggestions using RAG and user style analysis
 */
interface SmartReplyRepository {
    
    /**
     * Generate smart reply suggestions for an incoming message
     * 
     * @param conversationId The conversation ID
     * @param incomingMessageId The ID of the message to reply to
     * @param targetLanguage The language for reply suggestions (ISO 639-1 code)
     * @return Result containing list of smart reply suggestions
     */
    suspend fun generateReplies(
        conversationId: String,
        incomingMessageId: String,
        targetLanguage: String
    ): Result<List<SmartReply>>
    
    /**
     * Get user's communication style analysis for a conversation
     * 
     * @param conversationId The conversation ID
     * @return Result containing user style analysis
     */
    suspend fun getUserStyle(
        conversationId: String
    ): Result<UserCommunicationStyle>
}

