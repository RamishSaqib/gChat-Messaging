package com.gchat.domain.repository

import com.gchat.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for conversation operations
 */
interface ConversationRepository {
    fun getConversationsFlow(): Flow<List<Conversation>>
    fun getConversationFlow(conversationId: String): Flow<Conversation?>
    suspend fun getConversations(): Result<List<Conversation>>
    suspend fun getConversation(conversationId: String): Result<Conversation>
    suspend fun createConversation(conversation: Conversation): Result<Unit>
    suspend fun updateLastMessage(conversationId: String, messageId: String, messageText: String?, senderId: String, timestamp: Long): Result<Unit>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun findOrCreateOneOnOneConversation(currentUserId: String, otherUserId: String): Result<Conversation>
}

