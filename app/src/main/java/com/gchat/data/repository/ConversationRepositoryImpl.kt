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
import kotlinx.coroutines.flow.onEach
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
        if (userId != null) {
            if (syncJob == null || syncJob?.isActive == false) {
                android.util.Log.d("ConversationRepo", "Starting new sync job for user: $userId")
                syncJob = scope.launch {
                    syncConversationsFromFirestore(userId)
                }
            } else {
                android.util.Log.d("ConversationRepo", "Sync job already active")
            }
        }
        
        // Return Flow from local database
        return conversationDao.getAllConversationsFlow()
            .onEach { entities ->
                android.util.Log.d("ConversationRepo", "Room Flow emitted ${entities.size} conversations")
            }
            .map { entities ->
                entities.map { entity ->
                    // Create lastMessage from entity fields (no need to look up in messages table)
                    val lastMessage = if (entity.lastMessageId != null) {
                        com.gchat.domain.model.Message(
                            id = entity.lastMessageId,
                            conversationId = entity.id,
                            senderId = entity.lastMessageSenderId ?: "",
                            type = try {
                                if (entity.lastMessageType != null) {
                                    com.gchat.domain.model.MessageType.valueOf(entity.lastMessageType)
                                } else {
                                    com.gchat.domain.model.MessageType.TEXT
                                }
                            } catch (e: Exception) {
                                com.gchat.domain.model.MessageType.TEXT
                            },
                            text = entity.lastMessageText,
                            mediaUrl = entity.lastMessageMediaUrl,
                            timestamp = entity.lastMessageTimestamp,
                            status = com.gchat.domain.model.MessageStatus.SENT,
                            readBy = emptyMap()
                        )
                    } else null
                    
                    ConversationMapper.toDomain(entity, lastMessage)
                }
            }
            .onEach { conversations ->
                android.util.Log.d("ConversationRepo", "Mapped to ${conversations.size} domain conversations")
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
                            type = try {
                                if (it.lastMessageType != null) {
                                    com.gchat.domain.model.MessageType.valueOf(it.lastMessageType)
                                } else {
                                    com.gchat.domain.model.MessageType.TEXT
                                }
                            } catch (e: Exception) {
                                com.gchat.domain.model.MessageType.TEXT
                            },
                            text = it.lastMessageText,
                            mediaUrl = it.lastMessageMediaUrl,
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
                                    type = try {
                                        if (entity.lastMessageType != null) {
                                            com.gchat.domain.model.MessageType.valueOf(entity.lastMessageType)
                                        } else {
                                            com.gchat.domain.model.MessageType.TEXT
                                        }
                                    } catch (e: Exception) {
                                        com.gchat.domain.model.MessageType.TEXT
                                    },
                                    text = entity.lastMessageText,
                                    mediaUrl = entity.lastMessageMediaUrl,
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
                                type = try {
                                    if (localEntity.lastMessageType != null) {
                                        com.gchat.domain.model.MessageType.valueOf(localEntity.lastMessageType)
                                    } else {
                                        com.gchat.domain.model.MessageType.TEXT
                                    }
                                } catch (e: Exception) {
                                    com.gchat.domain.model.MessageType.TEXT
                                },
                                text = localEntity.lastMessageText,
                                mediaUrl = localEntity.lastMessageMediaUrl,
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
        messageType: String?,
        mediaUrl: String?,
        senderId: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            // Don't clear deletedAt - keep deletion timestamps for message filtering
            // Conversation will automatically reappear when lastMessageTimestamp > deletionTimestamp
            // ChatViewModel will filter messages based on deletion timestamp
            
            // Update locally
            conversationDao.updateLastMessage(
                conversationId,
                messageId,
                messageText,
                messageType,
                mediaUrl,
                senderId,
                timestamp
            )
            
            // Sync to Firestore
            firestoreConversationDataSource.updateLastMessage(
                conversationId,
                messageId,
                messageText,
                messageType,
                mediaUrl,
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
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            
            android.util.Log.d("ConversationRepo", "Marking conversation $conversationId as deleted for user $currentUserId")
            
            // Don't delete locally - keep conversation and messages in database
            // The ViewModel will filter it out based on deletedAt field
            // ChatViewModel will filter messages by timestamp when chat reappears
            
            // Set deletion timestamp in Firestore
            val firestoreResult = firestoreConversationDataSource.removeUserFromConversation(conversationId, currentUserId)
            firestoreResult.onFailure { error ->
                android.util.Log.w("ConversationRepo", "Failed to set deletion timestamp in Firestore: ${error.message}")
            }
            
            android.util.Log.d("ConversationRepo", "Deletion timestamp set in Firestore, conversation will be filtered from UI")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ConversationRepo", "Failed to mark conversation as deleted: ${e.message}", e)
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
                        // Create new conversation with both participants but mark creator
                        // Conversation will only be visible to receiver after first message
                        val newConversation = Conversation(
                            id = UUID.randomUUID().toString(),
                            type = ConversationType.ONE_ON_ONE,
                            participants = listOf(currentUserId, otherUserId), // Both users
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            creatorId = currentUserId // Mark who created it
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
            
            // Then sync to Firestore (await to ensure completion)
            val result = firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf("name" to newName, "updatedAt" to timestamp)
            )
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateGroupIcon(conversationId: String, newIconUrl: String?): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Update locally first for immediate UI update
            conversationDao.updateGroupIcon(conversationId, newIconUrl, timestamp)
            
            // Then sync to Firestore (await to ensure completion)
            val result = firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf("iconUrl" to newIconUrl, "updatedAt" to timestamp)
            )
            
            result
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
    
    override suspend fun updateAutoTranslate(conversationId: String, enabled: Boolean): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Update locally first for immediate UI update
            conversationDao.updateAutoTranslate(conversationId, enabled, timestamp)
            
            // Then sync to Firestore (await and return the result)
            val result = firestoreConversationDataSource.updateConversation(
                conversationId,
                mapOf(
                    "autoTranslateEnabled" to enabled,
                    "updatedAt" to timestamp
                )
            )
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun syncConversationsFromFirestore(userId: String) {
        try {
            android.util.Log.d("ConversationRepo", "Starting Firestore sync for user: $userId")
            firestoreConversationDataSource.observeConversations(userId)
                .collect { conversations ->
                    android.util.Log.d("ConversationRepo", "Firestore sync received ${conversations.size} conversations")
                    // Use upsertAll to ensure Room Flow observers are always triggered
                    // @Update always triggers flow, @Insert with REPLACE doesn't always trigger
                    val entities = conversations.map { ConversationMapper.toEntity(it) }
                    conversationDao.upsertAll(entities)
                    android.util.Log.d("ConversationRepo", "Updated local database with ${entities.size} conversations")
                }
        } catch (e: Exception) {
            // Log error but don't crash - user will see locally cached data
            android.util.Log.e("ConversationRepo", "Error syncing conversations: ${e.message}", e)
            e.printStackTrace()
        }
    }
}

