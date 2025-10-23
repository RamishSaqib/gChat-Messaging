package com.gchat.data.repository

import com.gchat.data.local.dao.UserDao
import com.gchat.data.mapper.UserMapper
import com.gchat.data.remote.firestore.FirestoreUserDataSource
import com.gchat.domain.model.User
import com.gchat.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestoreUserDataSource: FirestoreUserDataSource
) : UserRepository {
    
    // Repository-scoped coroutine for background operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun getUserFlow(userId: String): Flow<User?> {
        // CRITICAL: Check cache SYNCHRONOUSLY before creating the Flow
        // This ensures StateFlow has the correct initialValue immediately
        val cachedUser = runBlocking { userDao.getUserById(userId) }
        android.util.Log.d("UserRepository", "getUserFlow called for userId: $userId, cached: ${cachedUser != null}, displayName: ${cachedUser?.displayName}")
        
        // Start with cached value, then observe Firestore for updates
        return flow {
            // Emit cached data immediately (if available)
            if (cachedUser != null) {
                emit(UserMapper.toDomain(cachedUser))
            }
            
            // Then observe Firestore for real-time updates
            android.util.Log.d("UserRepository", "Starting Firestore observation for userId: $userId")
            firestoreUserDataSource.observeUser(userId).collect { user ->
                android.util.Log.d("UserRepository", "Firestore user received: ${user != null}, displayName: ${user?.displayName}")
                if (user != null) {
                    // Cache locally when updates arrive
                    userDao.insert(UserMapper.toEntity(user))
                    emit(user)
                } else if (cachedUser == null) {
                    // Only emit null if we don't have cached data
                    android.util.Log.d("UserRepository", "Emitting null (no cached data)")
                    emit(null)
                }
            }
        }
    }
    
    override suspend fun getUser(userId: String): Result<User> {
        return try {
            // Try local cache FIRST (instant)
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                android.util.Log.d("UserRepository", "getUser returning cached user: ${cachedUser.displayName}")
                return Result.success(UserMapper.toDomain(cachedUser))
            }
            
            android.util.Log.d("UserRepository", "getUser no cache, fetching from Firestore for: $userId")
            
            // If not in cache, fetch from Firestore
            val firestoreResult = firestoreUserDataSource.getUser(userId)
            
            firestoreResult.fold(
                onSuccess = { user ->
                    // Cache locally
                    userDao.insert(UserMapper.toEntity(user))
                    android.util.Log.d("UserRepository", "getUser cached Firestore user: ${user.displayName}")
                    Result.success(user)
                },
                onFailure = {
                    Result.failure(Exception("User not found"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> {
        return try {
            // CACHE FIRST: Try local database immediately (5-20ms)
            val localUsers = userDao.getUsersByIds(userIds)
                .map { UserMapper.toDomain(it) }
            
            android.util.Log.d("UserRepository", "getUsersByIds - found ${localUsers.size}/${userIds.size} users in cache")
            
            // If we have ALL users cached, return immediately
            if (localUsers.size == userIds.size) {
                return Result.success(localUsers)
            }
            
            // Otherwise, fetch missing users from Firestore (BLOCKING for initial load)
            val cachedUserIds = localUsers.map { it.id }.toSet()
            val missingUserIds = userIds.filter { it !in cachedUserIds }
            
            android.util.Log.d("UserRepository", "getUsersByIds - fetching ${missingUserIds.size} missing users from Firestore")
            
            // Fetch missing users from Firestore and WAIT for them
            val firestoreResult = firestoreUserDataSource.getUsersByIds(missingUserIds)
            firestoreResult.onSuccess { newUsers ->
                // Cache them immediately
                newUsers.forEach { user ->
                    userDao.insert(UserMapper.toEntity(user))
                }
                android.util.Log.d("UserRepository", "getUsersByIds - cached ${newUsers.size} new users from Firestore")
            }
            
            // Return ALL users (cached + newly fetched)
            val allUsers = localUsers + (firestoreResult.getOrNull() ?: emptyList())
            Result.success(allUsers)
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Don't catch cancellation - rethrow it
            throw e
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "getUsersByIds error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Update locally
            userDao.updateOnlineStatus(userId, isOnline, timestamp)
            
            // Sync to Firestore
            firestoreUserDataSource.updateOnlineStatus(userId, isOnline)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchUsers(query: String, currentUserId: String): Result<List<User>> {
        return try {
            // Search from Firestore (no local caching for search results)
            firestoreUserDataSource.searchUsers(query, currentUserId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return firestoreUserDataSource.updateFcmToken(userId, token)
    }
    
    override suspend fun updateGlobalAutoTranslate(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            firestoreUserDataSource.updateUser(userId, mapOf("autoTranslateEnabled" to enabled))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateGlobalSmartReplies(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            firestoreUserDataSource.updateUser(userId, mapOf("smartRepliesEnabled" to enabled))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

