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
    suspend fun updateLastMessage(conversationId: String, messageId: String, messageText: String?, messageType: String?, mediaUrl: String?, senderId: String, timestamp: Long): Result<Unit>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun findOrCreateOneOnOneConversation(currentUserId: String, otherUserId: String): Result<Conversation>
    
    // Group management methods
    suspend fun updateGroupName(conversationId: String, newName: String): Result<Unit>
    suspend fun updateGroupIcon(conversationId: String, newIconUrl: String?): Result<Unit>
    suspend fun addParticipants(conversationId: String, participantIds: List<String>): Result<Unit>
    suspend fun removeParticipant(conversationId: String, participantId: String): Result<Unit>
    suspend fun promoteToAdmin(conversationId: String, userId: String): Result<Unit>
    suspend fun leaveGroup(conversationId: String, userId: String): Result<Unit>
    
    // Nickname management
    suspend fun setNickname(conversationId: String, userId: String, nickname: String?): Result<Unit>
}

