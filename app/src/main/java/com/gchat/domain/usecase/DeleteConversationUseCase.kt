package com.gchat.domain.usecase

import com.gchat.domain.repository.ConversationRepository
import javax.inject.Inject

/**
 * Use case for deleting a conversation
 * 
 * This removes the conversation from the user's view and removes them from
 * the participants list in Firestore. If they're the last participant,
 * the entire conversation is deleted.
 */
class DeleteConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return conversationRepository.deleteConversation(conversationId)
    }
}

