package com.gchat.domain.repository

import com.gchat.domain.model.FormalityLevel

/**
 * Repository interface for formality adjustment operations.
 * 
 * Provides AI-powered message tone adjustment (casual, neutral, formal).
 */
interface FormalityRepository {
    
    /**
     * Adjusts the formality level of a text message.
     * 
     * Uses AI to rewrite the given text to match the desired formality level
     * while preserving the original meaning and intent.
     * 
     * @param text The original message text to adjust
     * @param language The language of the text (ISO 639-1 code, e.g., "en", "es")
     * @param targetFormality The desired formality level (CASUAL, NEUTRAL, or FORMAL)
     * @return Result containing the adjusted text, or an error if adjustment failed
     * 
     * @see FormalityLevel
     */
    suspend fun adjustFormality(
        text: String,
        language: String,
        targetFormality: FormalityLevel
    ): Result<String>
}

