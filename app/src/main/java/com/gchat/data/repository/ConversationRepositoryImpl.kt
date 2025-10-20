package com.gchat.data.repository

import com.gchat.data.local.dao.ConversationDao
import com.gchat.data.local.dao.MessageDao
import com.gchat.data.mapper.ConversationMapper
import com.gchat.data.mapper.MessageMapper
import com.gchat.data.remote.firestore.FirestoreConversationDataSource
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.ConversationType
import com.gchat.domain.repository.ConversationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ConversationRepository
 * 
 * Offline-first architecture
 */
@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val firestoreConversationDataSource: FirestoreConversationDataSource,
    private val auth: FirebaseAuth
) : ConversationRepository {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun getConversationsFlow(): Flow<List<Conversation>> {
        // Start background sync
        val userId = auth.currentUser?.uid
        if (userId != null) {
            scope.launch {
                syncConversationsFromFirestore(userId)
            }
        }
        
        // Return Flow from local database
        return conversationDao.getAllConversationsFlow()
            .map { entities ->
                entities.map { entity ->
                    val lastMessage = entity.lastMessageId?.let {
                        messageDao.getMessageById(it)?.let { MessageMapper.toDomain(it) }
                    }
                    ConversationMapper.toDomain(entity, lastMessage)
                }
            }
    }
    
    override fun getConversationFlow(conversationId: String): Flow<Conversation?> {
        return conversationDao.getConversationByIdFlow(conversationId)
            .map { entity ->
                entity?.let {
                    val lastMessage = it.lastMessageId?.let { msgId ->
                        messageDao.getMessageById(msgId)?.let { MessageMapper.toDomain(it) }
                    }
                    ConversationMapper.toDomain(it, lastMessage)
                }
            }
    }
    
    override suspend fun getConversations(): Result<List<Conversation>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            
            // Try Firestore first
            val firestoreResult = firestoreConversationDataSource.getUserConversations(userId)
            
            firestoreResult.fold(
                onSuccess = { conversations ->
                    // Cache locally
                    conversations.forEach { conversation ->
                        conversationDao.insert(ConversationMapper.toEntity(conversation))
                    }
                    Result.success(conversations)
                },
                onFailure = {
                    // Fallback to local
                    val localConversations = conversationDao.getAllConversations()
                        .map { entity ->
                            val lastMessage = entity.lastMessageId?.let { msgId ->
                                messageDao.getMessageById(msgId)?.let { MessageMapper.toDomain(it) }
                            }
                            ConversationMapper.toDomain(entity, lastMessage)
                        }
                    Result.success(localConversations)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getConversation(conversationId: String): Result<Conversation> {
        return try {
            val firestoreResult = firestoreConversationDataSource.getConversation(conversationId)
            
            firestoreResult.fold(
                onSuccess = { conversation ->
                    // Cache locally
                    conversationDao.insert(ConversationMapper.toEntity(conversation))
                    Result.success(conversation)
                },
                onFailure = {
                    // Fallback to local
                    val localEntity = conversationDao.getConversationById(conversationId)
                    if (localEntity != null) {
                        val lastMessage = localEntity.lastMessageId?.let { msgId ->
                            messageDao.getMessageById(msgId)?.let { MessageMapper.toDomain(it) }
                        }
                        Result.success(ConversationMapper.toDomain(localEntity, lastMessage))
                    } else {
                        Result.failure(Exception("Conversation not found"))
                    }
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createConversation(conversation: Conversation): Result<Unit> {
        return try {
            // Write locally
            conversationDao.insert(ConversationMapper.toEntity(conversation))
            
            // Sync to Firestore
            firestoreConversationDataSource.createConversation(conversation)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateLastMessage(
        conversationId: String,
        messageId: String,
        messageText: String?,
        senderId: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            // Update locally
            conversationDao.updateLastMessage(
                conversationId,
                messageId,
                messageText,
                senderId,
                timestamp
            )
            
            // Sync to Firestore
            firestoreConversationDataSource.updateLastMessage(
                conversationId,
                messageId,
                messageText,
                senderId,
                timestamp
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            // Delete locally
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                conversationDao.delete(conversation)
                messageDao.deleteByConversation(conversationId)
            }
            
            // Delete from Firestore
            firestoreConversationDataSource.deleteConversation(conversationId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun findOrCreateOneOnOneConversation(
        currentUserId: String,
        otherUserId: String
    ): Result<Conversation> {
        return try {
            // Check if conversation already exists in Firestore
            val findResult = firestoreConversationDataSource.findOneOnOneConversation(
                currentUserId,
                otherUserId
            )
            
            findResult.fold(
                onSuccess = { existingConversation ->
                    if (existingConversation != null) {
                        // Cache locally
                        conversationDao.insert(ConversationMapper.toEntity(existingConversation))
                        Result.success(existingConversation)
                    } else {
                        // Create new conversation
                        val newConversation = Conversation(
                            id = UUID.randomUUID().toString(),
                            type = ConversationType.ONE_ON_ONE,
                            participants = listOf(currentUserId, otherUserId),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        
                        createConversation(newConversation)
                        Result.success(newConversation)
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncConversationsFromFirestore(userId: String) {
        try {
            firestoreConversationDataSource.observeConversations(userId)
                .collect { conversations ->
                    conversations.forEach { conversation ->
                        conversationDao.insert(ConversationMapper.toEntity(conversation))
                    }
                }
        } catch (e: Exception) {
            // Ignore errors (e.g., permission denied after logout)
            // User will see locally cached data
        }
    }
}

