package com.gchat.domain.repository

import com.gchat.domain.model.AuthResult
import com.gchat.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    val currentUser: Flow<User?>
    val isAuthenticated: Flow<Boolean>
    
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(email: String, password: String, displayName: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUserId(): String?
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Result<Unit>
}

