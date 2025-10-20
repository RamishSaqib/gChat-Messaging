package com.gchat.domain.usecase

import com.gchat.domain.model.AuthResult
import com.gchat.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user registration
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        if (email.isBlank()) {
            return AuthResult.Error("Email cannot be empty")
        }
        if (password.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters")
        }
        if (displayName.isBlank()) {
            return AuthResult.Error("Display name cannot be empty")
        }
        
        return authRepository.register(email, password, displayName)
    }
}

