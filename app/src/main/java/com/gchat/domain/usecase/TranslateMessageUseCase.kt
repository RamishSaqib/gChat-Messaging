package com.gchat.domain.usecase

import com.gchat.domain.model.Translation
import com.gchat.domain.repository.TranslationRepository
import javax.inject.Inject

/**
 * Use case for translating messages
 * 
 * Handles business logic for translation including caching and error handling
 */
class TranslateMessageUseCase @Inject constructor(
    private val translationRepository: TranslationRepository
) {
    /**
     * Translate a message to the target language
     * 
     * @param messageId ID of the message being translated
     * @param text Text to translate
     * @param targetLanguage Target language ISO code
     * @param sourceLanguage Optional source language (auto-detect if null)
     * @return Result with Translation or error
     */
    suspend operator fun invoke(
        messageId: String,
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): Result<Translation> {
        // Validate input
        if (text.isBlank()) {
            return Result.failure(Exception("Text cannot be empty"))
        }
        
        if (targetLanguage.length != 2) {
            return Result.failure(Exception("Invalid target language code"))
        }
        
        // Call repository to translate
        return try {
            translationRepository.translateMessage(
                messageId = messageId,
                text = text,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

