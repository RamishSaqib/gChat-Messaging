package com.gchat.data.repository

import com.gchat.data.local.dao.UserDao
import com.gchat.data.mapper.UserMapper
import com.gchat.data.remote.firestore.FirestoreUserDataSource
import com.gchat.domain.model.User
import com.gchat.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
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
    
    override fun getUserFlow(userId: String): Flow<User?> {
        // Offline-first approach: Start with cached data, then observe Firestore updates
        return kotlinx.coroutines.flow.flow {
            // Emit cached data first (immediate load)
            val cachedUser = userDao.getUserById(userId)
            if (cachedUser != null) {
                emit(UserMapper.toDomain(cachedUser))
            }
            
            // Then observe Firestore for real-time updates
            firestoreUserDataSource.observeUser(userId).collect { user ->
                if (user != null) {
                    // Cache locally when updates arrive
                    userDao.insert(UserMapper.toEntity(user))
                    emit(user)
                } else if (cachedUser == null) {
                    // Only emit null if we don't have cached data
                    emit(null)
                }
            }
        }
    }
    
    override suspend fun getUser(userId: String): Result<User> {
        return try {
            // Try Firestore first
            val firestoreResult = firestoreUserDataSource.getUser(userId)
            
            firestoreResult.fold(
                onSuccess = { user ->
                    // Cache locally
                    userDao.insert(UserMapper.toEntity(user))
                    Result.success(user)
                },
                onFailure = {
                    // Fallback to local
                    val localUser = userDao.getUserById(userId)
                    if (localUser != null) {
                        Result.success(UserMapper.toDomain(localUser))
                    } else {
                        Result.failure(Exception("User not found"))
                    }
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUsersByIds(userIds: List<String>): Result<List<User>> {
        return try {
            // Try Firestore first
            val firestoreResult = firestoreUserDataSource.getUsersByIds(userIds)
            
            firestoreResult.fold(
                onSuccess = { users ->
                    // Cache locally
                    users.forEach { user ->
                        userDao.insert(UserMapper.toEntity(user))
                    }
                    Result.success(users)
                },
                onFailure = {
                    // Fallback to local
                    val localUsers = userDao.getUsersByIds(userIds)
                        .map { UserMapper.toDomain(it) }
                    Result.success(localUsers)
                }
            )
        } catch (e: Exception) {
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
}

