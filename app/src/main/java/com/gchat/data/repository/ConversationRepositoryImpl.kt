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
    
    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private var syncJob: kotlinx.coroutines.Job? = null
    
    override fun getConversationsFlow(): Flow<List<Conversation>> {
        // Start background sync if not already running
        val userId = auth.currentUser?.uid
        if (userId != null && (syncJob == null || syncJob?.isActive == false)) {
            syncJob = scope.launch {
                syncConversationsFromFirestore(userId)
            }
        }
        
        // Return Flow from local database
        return conversationDao.getAllConversationsFlow()
            .map { entities ->
                entities.map { entity ->
                    // Create lastMessage from entity fields (no need to look up in messages table)
                    val lastMessage = if (entity.lastMessageId != null) {
                        com.gchat.domain.model.Message(
                            id = entity.lastMessageId,
                            conversationId = entity.id,
                            senderId = entity.lastMessageSenderId ?: "",
                            type = com.gchat.domain.model.MessageType.TEXT,
                            text = entity.lastMessageText,
                            mediaUrl = null,
                            timestamp = entity.lastMessageTimestamp,
                            status = com.gchat.domain.model.MessageStatus.SENT,
                            readBy = emptyMap()
                        )
                    } else null
                    
                    ConversationMapper.toDomain(entity, lastMessage)
                }
            }
    }
    
    override fun getConversationFlow(conversationId: String): Flow<Conversation?> {
        return conversationDao.getConversationByIdFlow(conversationId)
            .map { entity ->
                entity?.let {
                    // Create lastMessage from entity fields (no need to look up in messages table)
                    val lastMessage = if (it.lastMessageId != null) {
                        com.gchat.domain.model.Message(
                            id = it.lastMessageId,
                            conversationId = it.id,
                            senderId = it.lastMessageSenderId ?: "",
                            type = com.gchat.domain.model.MessageType.TEXT,
                            text = it.lastMessageText,
                            mediaUrl = null,
                            timestamp = it.lastMessageTimestamp,
                            status = com.gchat.domain.model.MessageStatus.SENT,
                            readBy = emptyMap()
                        )
                    } else null
                    
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
                            // Create lastMessage from entity fields
                            val lastMessage = if (entity.lastMessageId != null) {
                                com.gchat.domain.model.Message(
                                    id = entity.lastMessageId,
                                    conversationId = entity.id,
                                    senderId = entity.lastMessageSenderId ?: "",
                                    type = com.gchat.domain.model.MessageType.TEXT,
                                    text = entity.lastMessageText,
                                    mediaUrl = null,
                                    timestamp = entity.lastMessageTimestamp,
                                    status = com.gchat.domain.model.MessageStatus.SENT,
                                    readBy = emptyMap()
                                )
                            } else null
                            
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
                        // Create lastMessage from entity fields
                        val lastMessage = if (localEntity.lastMessageId != null) {
                            com.gchat.domain.model.Message(
                                id = localEntity.lastMessageId,
                                conversationId = localEntity.id,
                                senderId = localEntity.lastMessageSenderId ?: "",
                                type = com.gchat.domain.model.MessageType.TEXT,
                                text = localEntity.lastMessageText,
                                mediaUrl = null,
                                timestamp = localEntity.lastMessageTimestamp,
                                status = com.gchat.domain.model.MessageStatus.SENT,
                                readBy = emptyMap()
                            )
                        } else null
                        
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
    
    override suspend fun updateGroupName(conversationId: String, newName: String): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Update locally first for immediate UI update
            conversationDao.updateGroupName(conversationId, newName, timestamp)
            
            // Then sync to Firestore
            firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf("name" to newName, "updatedAt" to timestamp)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateGroupIcon(conversationId: String, newIconUrl: String?): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Update locally first for immediate UI update
            conversationDao.updateGroupIcon(conversationId, newIconUrl, timestamp)
            
            // Then sync to Firestore
            firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf("iconUrl" to newIconUrl, "updatedAt" to timestamp)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addParticipants(conversationId: String, participantIds: List<String>): Result<Unit> {
        return try {
            val conversation = getConversation(conversationId).getOrThrow()
            val updatedParticipants = (conversation.participants + participantIds).distinct()
            
            firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf(
                    "participants" to updatedParticipants,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeParticipant(conversationId: String, participantId: String): Result<Unit> {
        return try {
            val conversation = getConversation(conversationId).getOrThrow()
            val updatedParticipants = conversation.participants.filter { it != participantId }
            val updatedAdmins = conversation.groupAdmins.filter { it != participantId }
            
            firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf(
                    "participants" to updatedParticipants,
                    "groupAdmins" to updatedAdmins,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun promoteToAdmin(conversationId: String, userId: String): Result<Unit> {
        return try {
            val conversation = getConversation(conversationId).getOrThrow()
            if (!conversation.groupAdmins.contains(userId)) {
                val updatedAdmins = conversation.groupAdmins + userId
                
                firestoreConversationDataSource.updateConversation(
                    conversationId,
                    mapOf(
                        "groupAdmins" to updatedAdmins,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun leaveGroup(conversationId: String, userId: String): Result<Unit> {
        return removeParticipant(conversationId, userId)
    }
    
    override suspend fun setNickname(conversationId: String, userId: String, nickname: String?): Result<Unit> {
        return try {
            val conversation = getConversation(conversationId).getOrThrow()
            val updatedNicknames = conversation.nicknames.toMutableMap()
            
            if (nickname.isNullOrBlank()) {
                // Remove nickname if empty or null
                updatedNicknames.remove(userId)
            } else {
                // Set/update nickname
                updatedNicknames[userId] = nickname
            }
            
            firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf(
                    "nicknames" to updatedNicknames,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncConversationsFromFirestore(userId: String) {
        try {
            firestoreConversationDataSource.observeConversations(userId)
                .collect { conversations ->
                    // Batch insert all conversations for better performance
                    val entities = conversations.map { ConversationMapper.toEntity(it) }
                    conversationDao.insertAll(entities)
                }
        } catch (e: Exception) {
            // Log error but don't crash - user will see locally cached data
            println("Error syncing conversations: ${e.message}")
        }
    }
}

