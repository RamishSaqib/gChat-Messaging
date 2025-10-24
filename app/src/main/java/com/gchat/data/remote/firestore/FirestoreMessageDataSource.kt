package com.gchat.data.remote.firestore

import com.gchat.data.mapper.MessageMapper
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for Message operations
 */
@Singleton
class FirestoreMessageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private fun getMessagesCollection(conversationId: String) =
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
    
    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            getMessagesCollection(message.conversationId)
                .document(message.id)
                .set(MessageMapper.toFirestore(message))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeMessages(conversationId: String, limit: Int = 50): Flow<List<Message>> = callbackFlow {
        val listener = getMessagesCollection(conversationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle PERMISSION_DENIED gracefully (happens after logout)
                    if (error.message?.contains("PERMISSION_DENIED") == true) {
                        android.util.Log.d("FirestoreMessage", "Snapshot listener permission denied (user logged out?), closing gracefully")
                        close() // Close without error to avoid crash
                    } else {
                        android.util.Log.e("FirestoreMessage", "Snapshot listener error: ${error.message}", error)
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents
                    ?.mapNotNull { MessageMapper.fromFirestore(it) }
                    ?.reversed() // Reverse to get chronological order
                    ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getMessages(conversationId: String, limit: Int = 50): Result<List<Message>> {
        return try {
            val documents = getMessagesCollection(conversationId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val messages = documents.mapNotNull { MessageMapper.fromFirestore(it) }
                .reversed()
            
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus
    ): Result<Unit> {
        return try {
            getMessagesCollection(conversationId)
                .document(messageId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String,
        userId: String,
        readTimestamp: Long
    ): Result<Unit> {
        return try {
            val messageRef = getMessagesCollection(conversationId).document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                
                // Parse existing readBy map (userId -> timestamp)
                val currentReadBy = try {
                    (snapshot.get("readBy") as? Map<*, *>)?.mapNotNull { (key, value) ->
                        val uid = key as? String
                        val ts = when (value) {
                            is Long -> value
                            is Number -> value.toLong()
                            else -> null
                        }
                        if (uid != null && ts != null) uid to ts else null
                    }?.toMap()?.toMutableMap() ?: mutableMapOf()
                } catch (e: Exception) {
                    // Fallback: handle old list format for backward compatibility
                    val oldList = (snapshot.get("readBy") as? List<*>)?.mapNotNull { it as? String }
                    oldList?.associateWith { System.currentTimeMillis() }?.toMutableMap() ?: mutableMapOf()
                }
                
                // Add or update this user's read timestamp
                if (!currentReadBy.containsKey(userId)) {
                    currentReadBy[userId] = readTimestamp
                    transaction.update(messageRef, mapOf(
                        "readBy" to currentReadBy,
                        "status" to MessageStatus.READ.name
                    ))
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMessageTranscription(
        conversationId: String,
        messageId: String,
        transcription: String
    ): Result<Unit> {
        return try {
            // Update the message in Firestore directly with conversationId
            firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("transcription", transcription)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            getMessagesCollection(conversationId)
                .document(messageId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addReaction(
        conversationId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Result<Unit> {
        return try {
            val messageRef = getMessagesCollection(conversationId).document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                
                // Parse existing reactions map (emoji -> list of userIds)
                val currentReactions = try {
                    (snapshot.get("reactions") as? Map<*, *>)?.mapNotNull { (key, value) ->
                        val emojiKey = key as? String
                        val userIds = (value as? List<*>)?.mapNotNull { it as? String }?.toMutableList()
                        if (emojiKey != null && userIds != null) emojiKey to userIds else null
                    }?.toMap()?.toMutableMap() ?: mutableMapOf()
                } catch (e: Exception) {
                    mutableMapOf()
                }
                
                // Remove user from all other reactions first (user can only have one reaction per message)
                currentReactions.values.forEach { userIds ->
                    userIds.remove(userId)
                }
                // Remove empty reaction lists
                currentReactions.entries.removeIf { it.value.isEmpty() }
                
                // Add user to the selected emoji reaction
                val userList = currentReactions.getOrPut(emoji) { mutableListOf() }
                if (!userList.contains(userId)) {
                    userList.add(userId)
                }
                
                // Update in Firestore
                transaction.update(messageRef, "reactions", currentReactions)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreMessage", "Add reaction error", e)
            Result.failure(e)
        }
    }
    
    suspend fun removeReaction(
        conversationId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val messageRef = getMessagesCollection(conversationId).document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                
                // Parse existing reactions map
                val currentReactions = try {
                    (snapshot.get("reactions") as? Map<*, *>)?.mapNotNull { (key, value) ->
                        val emojiKey = key as? String
                        val userIds = (value as? List<*>)?.mapNotNull { it as? String }?.toMutableList()
                        if (emojiKey != null && userIds != null) emojiKey to userIds else null
                    }?.toMap()?.toMutableMap() ?: mutableMapOf()
                } catch (e: Exception) {
                    mutableMapOf()
                }
                
                // Remove user from all reactions
                currentReactions.values.forEach { userIds ->
                    userIds.remove(userId)
                }
                // Remove empty reaction lists
                currentReactions.entries.removeIf { it.value.isEmpty() }
                
                // Update in Firestore
                transaction.update(messageRef, "reactions", currentReactions)
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreMessage", "Remove reaction error", e)
            Result.failure(e)
        }
    }
}

