package com.gchat.domain.usecase

import com.gchat.domain.model.Conversation
import com.gchat.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving conversations
 */
class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return conversationRepository.getConversationsFlow()
    }
}

