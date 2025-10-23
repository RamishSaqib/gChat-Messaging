package com.gchat.domain.repository

import com.gchat.domain.model.Translation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for translation operations
 */
interface TranslationRepository {
    
    /**
     * Translate text from source language to target language
     * 
     * @param messageId ID of the message being translated
     * @param text Text to translate
     * @param sourceLanguage Source language code (optional, auto-detect if null)
     * @param targetLanguage Target language code
     * @return Result with Translation or error
     */
    suspend fun translateMessage(
        messageId: String,
        text: String,
        sourceLanguage: String?,
        targetLanguage: String
    ): Result<Translation>
    
    /**
     * Get cached translation for a message
     * 
     * @param messageId Message ID
     * @param targetLanguage Target language code
     * @return Flow of Translation or null if not cached
     */
    fun getCachedTranslation(messageId: String, targetLanguage: String): Flow<Translation?>
    
    /**
     * Detect the language of given text
     * 
     * @param text Text to analyze
     * @param senderLanguageHint Optional sender's preferred language for disambiguation
     * @return Result with detected language code or error
     */
    suspend fun detectLanguage(text: String, senderLanguageHint: String? = null): Result<String>
    
    /**
     * Clear all cached translations (for cleanup/testing)
     */
    suspend fun clearCache(): Result<Unit>
}

