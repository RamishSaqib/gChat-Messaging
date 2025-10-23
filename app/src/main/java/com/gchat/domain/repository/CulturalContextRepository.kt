package com.gchat.domain.repository

import com.gchat.domain.model.CulturalContextResult

/**
 * Repository interface for cultural context operations.
 * Analyzes messages for idioms, slang, and cultural references.
 */
interface CulturalContextRepository {

    /**
     * Gets cultural context (idioms, slang, cultural references) for a message.
     * 
     * @param messageId The ID of the message
     * @param text The text to analyze
     * @param language The language of the text
     * @param mode Analysis mode: 'all' (default), 'slang', or 'idioms'
     * @return Result containing CulturalContextResult or an error
     */
    suspend fun getCulturalContext(
        messageId: String,
        text: String,
        language: String,
        mode: String = "all"
    ): Result<CulturalContextResult>
}

