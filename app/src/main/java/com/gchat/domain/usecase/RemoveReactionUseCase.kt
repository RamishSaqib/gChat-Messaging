package com.gchat.domain.usecase

import com.gchat.domain.model.Message
import com.gchat.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Use case for removing a user's reaction from a message
 */
class RemoveReactionUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        message: Message,
        userId: String
    ): Result<Unit> {
        // Call repository to remove reaction
        return messageRepository.removeReaction(
            messageId = message.id,
            conversationId = message.conversationId,
            userId = userId
        )
    }
}

