package com.gchat.domain.usecase

import com.gchat.domain.model.CulturalContextResult
import com.gchat.domain.repository.CulturalContextRepository
import javax.inject.Inject

/**
 * Use case for getting cultural context (idioms, slang, cultural references) for a message.
 * 
 * Orchestrates the call to the Cultural Context repository to analyze text
 * and provide explanations for expressions that may not translate literally.
 */
class GetCulturalContextUseCase @Inject constructor(
    private val culturalContextRepository: CulturalContextRepository
) {
    /**
     * Analyzes a message for cultural context.
     * 
     * @param messageId The ID of the message to analyze
     * @param text The text content to analyze
     * @param language The language of the text
     * @param mode Analysis mode: 'all', 'slang', or 'idioms'
     * @return Result containing CulturalContextResult with detected expressions
     */
    suspend operator fun invoke(
        messageId: String,
        text: String,
        language: String,
        mode: String = "all"
    ): Result<CulturalContextResult> {
        // Validate input
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("Text cannot be empty"))
        }
        
        if (language.isBlank()) {
            return Result.failure(IllegalArgumentException("Language cannot be empty"))
        }

        return culturalContextRepository.getCulturalContext(
            messageId = messageId,
            text = text,
            language = language,
            mode = mode
        )
    }
}

