package com.gchat.domain.usecase

import com.gchat.domain.model.FormalityLevel
import com.gchat.domain.repository.FormalityRepository
import javax.inject.Inject

/**
 * Use case for adjusting the formality level of message text.
 * 
 * Coordinates the formality adjustment operation through the repository layer,
 * ensuring consistent access to AI-powered tone adjustment across the app.
 * 
 * Example usage:
 * ```
 * val result = adjustFormalityUseCase(
 *     text = "Hey! What's up?",
 *     language = "en",
 *     targetFormality = FormalityLevel.FORMAL
 * )
 * // Result: "Hello. How are you?"
 * ```
 */
class AdjustFormalityUseCase @Inject constructor(
    private val formalityRepository: FormalityRepository
) {
    /**
     * Adjusts the formality of the given text.
     * 
     * @param text The original message text
     * @param language The language code (e.g., "en", "es", "fr")
     * @param targetFormality The desired formality level
     * @return Result containing the adjusted text or an error
     */
    suspend operator fun invoke(
        text: String,
        language: String,
        targetFormality: FormalityLevel
    ): Result<String> {
        // Validate input
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Text cannot be empty"))
        }
        
        if (language.isBlank()) {
            return Result.failure(IllegalArgumentException("Language cannot be empty"))
        }
        
        // Delegate to repository
        return formalityRepository.adjustFormality(text, language, targetFormality)
    }
}

