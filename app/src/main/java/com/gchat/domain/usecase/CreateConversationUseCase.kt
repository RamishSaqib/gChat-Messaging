package com.gchat.domain.usecase

import com.gchat.domain.model.Conversation
import com.gchat.domain.repository.ConversationRepository
import javax.inject.Inject

/**
 * Use case for creating or finding a conversation
 */
class CreateConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        currentUserId: String,
        otherUserId: String
    ): Result<Conversation> {
        return conversationRepository.findOrCreateOneOnOneConversation(
            currentUserId,
            otherUserId
        )
    }
}

