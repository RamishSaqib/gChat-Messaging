package com.gchat.data.remote.firestore

import com.gchat.domain.model.TypingIndicator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for typing indicators
 */
@Singleton
class FirestoreTypingDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private fun getTypingCollection(conversationId: String) =
        firestore.collection("conversations")
            .document(conversationId)
            .collection("typing")
    
    /**
     * Set typing status for a user in a conversation
     */
    suspend fun setTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return try {
            if (isTyping) {
                // Set typing indicator
                getTypingCollection(conversationId)
                    .document(userId)
                    .set(mapOf(
                        "isTyping" to true,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    .await()
            } else {
                // Remove typing indicator
                getTypingCollection(conversationId)
                    .document(userId)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observe typing indicators for a conversation
     * Returns a Flow of user IDs who are currently typing
     */
    fun observeTypingIndicators(conversationId: String): Flow<List<TypingIndicator>> = callbackFlow {
        val listener = getTypingCollection(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val typingIndicators = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        TypingIndicator(
                            conversationId = conversationId,
                            userId = doc.id,
                            isTyping = doc.getBoolean("isTyping") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.filter { it.isTyping } ?: emptyList()
                
                trySend(typingIndicators)
            }
        
        awaitClose { listener.remove() }
    }
}

