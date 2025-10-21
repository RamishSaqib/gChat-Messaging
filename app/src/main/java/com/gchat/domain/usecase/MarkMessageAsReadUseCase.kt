package com.gchat.domain.usecase

import com.gchat.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Use case to mark messages as read by the current user
 */
class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Mark a single message as read by the given user
     * 
     * @param messageId The ID of the message to mark as read
     * @param conversationId The ID of the conversation containing the message
     * @param userId The ID of the user marking the message as read
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        messageId: String,
        conversationId: String,
        userId: String
    ): Result<Unit> {
        return messageRepository.markMessageAsRead(
            messageId = messageId,
            conversationId = conversationId,
            userId = userId,
            readTimestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Mark multiple messages as read by the given user
     * 
     * @param messageIds List of message IDs to mark as read
     * @param conversationId The ID of the conversation containing the messages
     * @param userId The ID of the user marking the messages as read
     * @return Result indicating success or failure
     */
    suspend fun markMultiple(
        messageIds: List<String>,
        conversationId: String,
        userId: String
    ): Result<Unit> {
        val timestamp = System.currentTimeMillis()
        
        return try {
            messageIds.forEach { messageId ->
                messageRepository.markMessageAsRead(
                    messageId = messageId,
                    conversationId = conversationId,
                    userId = userId,
                    readTimestamp = timestamp
                ).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

