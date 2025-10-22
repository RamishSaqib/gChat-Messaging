package com.gchat.data.remote.firestore

import com.gchat.data.mapper.ConversationMapper
import com.gchat.domain.model.Conversation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for Conversation operations
 */
@Singleton
class FirestoreConversationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private val conversationsCollection = firestore.collection("conversations")
    
    suspend fun createConversation(conversation: Conversation): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversation.id)
                .set(ConversationMapper.toFirestore(conversation))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getConversation(conversationId: String): Result<Conversation> {
        return try {
            val document = conversationsCollection
                .document(conversationId)
                .get()
                .await()
            
            val conversation = ConversationMapper.fromFirestore(document)
            if (conversation != null) {
                Result.success(conversation)
            } else {
                Result.failure(Exception("Conversation not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = conversationsCollection
            .whereArrayContains("participants", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    // Handle PERMISSION_DENIED gracefully (happens after logout)
                    if (error.message?.contains("PERMISSION_DENIED") == true) {
                        android.util.Log.d("FirestoreConversation", "Snapshot listener permission denied (user logged out?), closing gracefully")
                        close() // Close without error to avoid crash
                    } else {
                        android.util.Log.e("FirestoreConversation", "Snapshot listener error: ${error.message}", error)
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val conversations = snapshot?.documents
                    ?.mapNotNull { ConversationMapper.fromFirestore(it) }
                    ?: emptyList()
                
                val hasPendingWrites = snapshot?.metadata?.hasPendingWrites() ?: false
                val isFromCache = snapshot?.metadata?.isFromCache() ?: false
                android.util.Log.d("FirestoreConversation", "Snapshot listener emitted ${conversations.size} conversations (hasPendingWrites: $hasPendingWrites, fromCache: $isFromCache)")
                
                // Log document changes for debugging
                snapshot?.documentChanges?.forEach { change ->
                    android.util.Log.d("FirestoreConversation", "Document change: ${change.type} for conversation ${change.document.id}")
                }
                
                // Always emit, even if from cache - this ensures UI updates quickly
                trySend(conversations)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun observeConversation(conversationId: String): Flow<Conversation?> = callbackFlow {
        val listener = conversationsCollection
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle PERMISSION_DENIED gracefully (happens after logout)
                    if (error.message?.contains("PERMISSION_DENIED") == true) {
                        android.util.Log.d("FirestoreConversation", "Snapshot listener permission denied (user logged out?), closing gracefully")
                        close() // Close without error to avoid crash
                    } else {
                        android.util.Log.e("FirestoreConversation", "Snapshot listener error: ${error.message}", error)
                        close(error)
                    }
                    return@addSnapshotListener
                }
                
                val conversation = snapshot?.let { ConversationMapper.fromFirestore(it) }
                trySend(conversation)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getUserConversations(userId: String): Result<List<Conversation>> {
        return try {
            val documents = conversationsCollection
                .whereArrayContains("participants", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val conversations = documents.mapNotNull { ConversationMapper.fromFirestore(it) }
            Result.success(conversations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateConversation(
        conversationId: String,
        updates: Map<String, Any?>
    ): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateLastMessage(
        conversationId: String,
        messageId: String,
        messageText: String?,
        messageType: String?,
        mediaUrl: String?,
        senderId: String,
        timestamp: Long
    ): Result<Unit> {
        android.util.Log.d("FirestoreConvDS", "updateLastMessage - type: $messageType, mediaUrl: $mediaUrl, text: $messageText")
        return updateConversation(
            conversationId,
            mapOf(
                "lastMessage" to mapOf(
                    "id" to messageId,
                    "senderId" to senderId,
                    "text" to messageText,
                    "type" to messageType,
                    "mediaUrl" to mediaUrl,
                    "timestamp" to timestamp
                ),
                "updatedAt" to timestamp
            )
        )
    }
    
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark conversation as deleted for a user. Doesn't remove from participants.
     * Allows for per-user deletion and restoration when new messages arrive.
     */
    suspend fun removeUserFromConversation(conversationId: String, userId: String): Result<Unit> {
        return try {
            val docRef = conversationsCollection.document(conversationId)
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                // Conversation doesn't exist in Firestore, that's okay
                android.util.Log.d("FirestoreConversation", "Conversation $conversationId not found in Firestore, skipping")
                return Result.success(Unit)
            }
            
            // Set deletion timestamp for user in deletedAt map without removing from participants
            val deletionTimestamp = System.currentTimeMillis()
            android.util.Log.d("FirestoreConversation", "Setting deletion timestamp $deletionTimestamp for user $userId in conversation $conversationId")
            docRef.update("deletedAt.$userId", deletionTimestamp).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.w("FirestoreConversation", "Failed to mark conversation as deleted: ${e.message}")
            // Don't fail if the document doesn't exist
            if (e.message?.contains("NOT_FOUND") == true || e.message?.contains("PERMISSION_DENIED") == true) {
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Find existing one-on-one conversation between two users
     */
    suspend fun findOneOnOneConversation(userId1: String, userId2: String): Result<Conversation?> {
        return try {
            val documents = conversationsCollection
                .whereEqualTo("type", "ONE_ON_ONE")
                .whereArrayContains("participants", userId1)
                .get()
                .await()
            
            val conversation = documents.mapNotNull { ConversationMapper.fromFirestore(it) }
                .firstOrNull { it.participants.contains(userId2) }
            
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

