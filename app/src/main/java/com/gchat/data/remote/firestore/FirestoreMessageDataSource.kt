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
                    close(error)
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
        userId: String
    ): Result<Unit> {
        return try {
            val messageRef = getMessagesCollection(conversationId).document(messageId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(messageRef)
                val currentReadBy = (snapshot.get("readBy") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toMutableList()
                    ?: mutableListOf()
                
                if (!currentReadBy.contains(userId)) {
                    currentReadBy.add(userId)
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
}

