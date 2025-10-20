package com.gchat.domain.usecase

import com.gchat.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Use case for marking messages as read
 */
class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return messageRepository.markMessageAsRead(conversationId, messageId, userId)
    }
}

