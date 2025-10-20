package com.gchat.domain.model

/**
 * Authentication result
 * 
 * Sealed class representing the result of authentication operations
 */
sealed class AuthResult {
    object Idle : AuthResult()
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

