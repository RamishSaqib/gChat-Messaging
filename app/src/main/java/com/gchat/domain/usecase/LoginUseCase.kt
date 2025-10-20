package com.gchat.domain.usecase

import com.gchat.domain.model.AuthResult
import com.gchat.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user login
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        if (email.isBlank()) {
            return AuthResult.Error("Email cannot be empty")
        }
        if (password.isBlank()) {
            return AuthResult.Error("Password cannot be empty")
        }
        
        return authRepository.login(email, password)
    }
}

