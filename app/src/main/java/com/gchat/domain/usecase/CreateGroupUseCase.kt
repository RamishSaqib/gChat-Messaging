package com.gchat.domain.usecase

import com.gchat.domain.model.Conversation
import com.gchat.domain.model.ConversationType
import com.gchat.domain.repository.ConversationRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for creating a group conversation
 */
class CreateGroupUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        creatorUserId: String,
        participantIds: List<String>,
        groupName: String,
        groupIconUrl: String? = null
    ): Result<Conversation> {
        // Validate inputs
        if (groupName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }
        
        if (participantIds.size < 2) {
            return Result.failure(IllegalArgumentException("Group must have at least 3 participants (including creator)"))
        }
        
        if (participantIds.size > 49) {
            return Result.failure(IllegalArgumentException("Group cannot have more than 50 participants"))
        }
        
        // Ensure creator is included in participants
        val allParticipants = (participantIds + creatorUserId).distinct()
        
        // Create group conversation
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            type = ConversationType.GROUP,
            participants = allParticipants,
            name = groupName.trim(),
            iconUrl = groupIconUrl,
            groupAdmins = listOf(creatorUserId), // Creator is the initial admin
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // Save to repository
        return conversationRepository.createConversation(conversation).fold(
            onSuccess = { Result.success(conversation) },
            onFailure = { Result.failure(it) }
        )
    }
}

