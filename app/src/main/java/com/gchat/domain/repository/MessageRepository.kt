package com.gchat.domain.repository

import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations
 */
interface MessageRepository {
    fun getMessagesFlow(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun getMessages(conversationId: String, limit: Int = 50): Result<List<Message>>
    suspend fun updateMessageStatus(conversationId: String, messageId: String, status: MessageStatus): Result<Unit>
    suspend fun markMessageAsRead(messageId: String, conversationId: String, userId: String, readTimestamp: Long): Result<Unit>
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit>
}

