package com.gchat.domain.usecase

import com.gchat.domain.model.Message
import com.gchat.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Use case for adding a reaction to a message
 * 
 * Validates the emoji and calls the repository to add the reaction
 */
class AddReactionUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    // Supported emojis (Facebook Messenger style)
    private val SUPPORTED_EMOJIS = setOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    
    suspend operator fun invoke(
        message: Message,
        userId: String,
        emoji: String
    ): Result<Unit> {
        // Validate emoji is supported
        if (emoji !in SUPPORTED_EMOJIS) {
            return Result.failure(IllegalArgumentException("Unsupported emoji: $emoji"))
        }
        
        // Call repository to add reaction
        return messageRepository.addReaction(
            messageId = message.id,
            conversationId = message.conversationId,
            userId = userId,
            emoji = emoji
        )
    }
}

