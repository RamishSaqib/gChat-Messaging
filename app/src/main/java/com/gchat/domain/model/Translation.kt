package com.gchat.domain.model

/**
 * Translation domain model
 * 
 * Represents a translated message with source and target language information
 */
data class Translation(
    val id: String, // Unique ID for caching
    val messageId: String, // Original message ID
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String, // ISO 639-1 code (e.g., "en", "es", "fr")
    val targetLanguage: String, // ISO 639-1 code
    val timestamp: Long = System.currentTimeMillis(),
    val cached: Boolean = false // Whether loaded from cache
) {
    companion object {
        /**
         * Supported languages with their ISO 639-1 codes
         */
        val SUPPORTED_LANGUAGES = mapOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "ja" to "Japanese",
            "ko" to "Korean",
            "zh" to "Chinese",
            "ar" to "Arabic",
            "hi" to "Hindi",
            "nl" to "Dutch",
            "pl" to "Polish",
            "tr" to "Turkish",
            "vi" to "Vietnamese",
            "th" to "Thai",
            "sv" to "Swedish",
            "da" to "Danish",
            "fi" to "Finnish"
        )
        
        /**
         * Get language name from ISO code
         */
        fun getLanguageName(code: String): String {
            return SUPPORTED_LANGUAGES[code] ?: code.uppercase()
        }
    }
}

