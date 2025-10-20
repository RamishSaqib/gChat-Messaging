package com.gchat.data.remote.firestore

import com.gchat.data.mapper.UserMapper
import com.gchat.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for User operations
 */
@Singleton
class FirestoreUserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    private val usersCollection = firestore.collection("users")
    
    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection
                .document(user.id)
                .set(UserMapper.toFirestore(user))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = usersCollection
                .document(userId)
                .get()
                .await()
            
            val user = UserMapper.fromFirestore(document)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.let { UserMapper.fromFirestore(it) }
                trySend(user)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun updateUser(userId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            usersCollection
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return updateUser(
            userId,
            mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
        )
    }
    
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return updateUser(userId, mapOf("fcmToken" to token))
    }
    
    suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> {
        return try {
            if (userIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Firestore 'in' query limit is 10, so we need to batch
            val users = mutableListOf<User>()
            userIds.chunked(10).forEach { chunk ->
                val documents = usersCollection
                    .whereIn("__name__", chunk)
                    .get()
                    .await()
                
                documents.mapNotNull { UserMapper.fromFirestore(it) }
                    .let { users.addAll(it) }
            }
            
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

