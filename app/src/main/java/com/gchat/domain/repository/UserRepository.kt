package com.gchat.domain.repository

import com.gchat.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user operations
 */
interface UserRepository {
    fun getUserFlow(userId: String): Flow<User?>
    suspend fun getUser(userId: String): Result<User>
    suspend fun getUsersByIds(userIds: List<String>): Result<List<User>>
    suspend fun searchUsers(query: String, currentUserId: String): Result<List<User>>
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit>
    
    // AI Feature preferences
    suspend fun updateGlobalAutoTranslate(userId: String, enabled: Boolean): Result<Unit>
    suspend fun updateGlobalSmartReplies(userId: String, enabled: Boolean): Result<Unit>
}

