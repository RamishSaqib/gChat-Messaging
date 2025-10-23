package com.gchat.domain.usecase

import com.gchat.domain.model.SmartReply
import com.gchat.domain.repository.SmartReplyRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for generating smart reply suggestions
 * 
 * Handles business logic for smart reply generation, including
 * rate limiting, caching, and error handling
 */
@Singleton
class GenerateSmartRepliesUseCase @Inject constructor(
    private val smartReplyRepository: SmartReplyRepository
) {
    
    /**
     * Generate context-aware reply suggestions for an incoming message
     * 
     * @param conversationId The conversation ID
     * @param incomingMessageId The ID of the message to reply to
     * @param targetLanguage The language for reply suggestions
     * @return Result containing list of 3 smart reply suggestions
     */
    suspend operator fun invoke(
        conversationId: String,
        incomingMessageId: String,
        targetLanguage: String
    ): Result<List<SmartReply>> {
        return try {
            // Validate inputs
            if (conversationId.isBlank() || incomingMessageId.isBlank()) {
                return Result.failure(IllegalArgumentException("Invalid conversation or message ID"))
            }
            
            if (targetLanguage.isBlank()) {
                return Result.failure(IllegalArgumentException("Target language is required"))
            }
            
            // Generate replies through repository
            smartReplyRepository.generateReplies(
                conversationId = conversationId,
                incomingMessageId = incomingMessageId,
                targetLanguage = targetLanguage
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

