package com.gchat.data.repository

import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.AutoTranslateRepository
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AutoTranslateRepository
 * 
 * Coordinates between AuthRepository and ConversationRepository
 * to manage auto-translate settings
 */
@Singleton
class AutoTranslateRepositoryImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository
) : AutoTranslateRepository {
    
    override suspend fun setGlobalAutoTranslate(userId: String, enabled: Boolean): Result<Unit> {
        return authRepository.updateUserProfile(userId, mapOf("autoTranslateEnabled" to enabled))
    }
    
    override suspend fun setConversationAutoTranslate(conversationId: String, enabled: Boolean): Result<Unit> {
        // We'll need to check ConversationRepositoryImpl for the updateConversation method
        // For now, let's use a specific method for this
        return try {
            conversationRepository.updateAutoTranslate(conversationId, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun shouldAutoTranslate(conversationId: String, userId: String): Boolean {
        return try {
            // Get conversation
            val conversation = conversationRepository.getConversation(conversationId).getOrNull()
            
            // If conversation has auto-translate explicitly set (true or false), use that
            // Note: We treat the conversation setting as the override if it's explicitly set
            // Since the default is false, we need to check if it's true
            if (conversation?.autoTranslateEnabled == true) {
                return true
            }
            
            // Otherwise, fallback to user's global setting
            val user = userRepository.getUser(userId).getOrNull()
            user?.autoTranslateEnabled ?: false
        } catch (e: Exception) {
            // On error, default to false (don't auto-translate)
            false
        }
    }
}

