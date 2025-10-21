package com.gchat.data.repository

import com.gchat.data.local.dao.MessageDao
import com.gchat.data.mapper.MessageMapper
import com.gchat.data.remote.firestore.FirestoreMessageDataSource
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.gchat.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MessageRepository
 *
 * Offline-first: Local Room database is the source of truth.
 * Real-time sync from Firestore updates local database.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val firestoreMessageDataSource: FirestoreMessageDataSource
) : MessageRepository {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        // Start background sync from Firestore
        scope.launch {
            syncMessagesFromFirestore(conversationId)
        }
        
        // Return Flow from local database (single source of truth)
        return messageDao.getMessagesFlow(conversationId)
            .map { entities -> entities.map { MessageMapper.toDomain(it) } }
    }
    
    override suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            // 1. Write to local database immediately (optimistic update)
            messageDao.insert(MessageMapper.toEntity(message))
            
            // 2. Sync to Firestore in background
            val result = firestoreMessageDataSource.sendMessage(message)
            
            result.fold(
                onSuccess = {
                    // Update status to SENT
                    messageDao.updateStatus(message.id, MessageStatus.SENT.name)
                },
                onFailure = {
                    // Mark as FAILED
                    messageDao.updateStatus(message.id, MessageStatus.FAILED.name)
                }
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getMessages(conversationId: String, limit: Int): Result<List<Message>> {
        return try {
            // Try to get from Firestore first
            val firestoreResult = firestoreMessageDataSource.getMessages(conversationId, limit)
            
            firestoreResult.fold(
                onSuccess = { messages ->
                    // Cache in local database
                    messages.forEach { message ->
                        messageDao.insert(MessageMapper.toEntity(message))
                    }
                    Result.success(messages)
                },
                onFailure = {
                    // Fallback to local database
                    val localMessages = messageDao.getMessages(conversationId, limit)
                        .map { MessageMapper.toDomain(it) }
                    Result.success(localMessages)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus
    ): Result<Unit> {
        return try {
            // Update locally
            messageDao.updateStatus(messageId, status.name)
            
            // Sync to Firestore
            firestoreMessageDataSource.updateMessageStatus(conversationId, messageId, status)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markMessageAsRead(
        messageId: String,
        conversationId: String,
        userId: String,
        readTimestamp: Long
    ): Result<Unit> {
        return try {
            // Update locally
            val message = messageDao.getMessageById(messageId)
            if (message != null) {
                val readByMap = try {
                    // Parse existing readBy map
                    kotlinx.serialization.json.Json.decodeFromString<Map<String, Long>>(message.readBy)
                        .toMutableMap()
                } catch (_: Exception) {
                    mutableMapOf()
                }
                
                // Add or update the read timestamp for this user
                if (!readByMap.containsKey(userId)) {
                    readByMap[userId] = readTimestamp
                    messageDao.updateReadBy(
                        messageId,
                        kotlinx.serialization.json.Json.encodeToString(readByMap)
                    )
                    messageDao.updateStatus(messageId, MessageStatus.READ.name)
                }
            }
            
            // Sync to Firestore
            firestoreMessageDataSource.markMessageAsRead(conversationId, messageId, userId, readTimestamp)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            // Delete locally
            val message = messageDao.getMessageById(messageId)
            if (message != null) {
                messageDao.delete(message)
            }
            
            // Delete from Firestore
            firestoreMessageDataSource.deleteMessage(conversationId, messageId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync messages from Firestore to local database
     */
    private suspend fun syncMessagesFromFirestore(conversationId: String) {
        try {
            firestoreMessageDataSource.observeMessages(conversationId)
                .collect { messages ->
                    messages.forEach { message ->
                        messageDao.insert(MessageMapper.toEntity(message))
                    }
                }
        } catch (e: Exception) {
            // Ignore errors (e.g., permission denied after logout)
            // User will see locally cached messages
        }
    }
}
