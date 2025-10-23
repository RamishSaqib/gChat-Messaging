package com.gchat.domain.model

/**
 * Formality levels for message tone adjustment.
 * 
 * Allows users to rewrite their messages in different formality styles
 * before sending (e.g., making casual text more formal for professional contexts).
 */
enum class FormalityLevel {
    /**
     * Casual tone - uses contractions, informal language, slang, emojis.
     * Example: "Hey! What's up? Wanna grab lunch later? 😊"
     */
    CASUAL,
    
    /**
     * Neutral tone - standard conversational language, balanced and friendly.
     * Example: "Hi! How are you? Would you like to have lunch later?"
     */
    NEUTRAL,
    
    /**
     * Formal tone - professional language, no contractions, polite phrasing.
     * Example: "Hello. I hope you are well. Would you be available for lunch later today?"
     */
    FORMAL;
    
    /**
     * Display name for UI
     */
    fun displayName(): String = when (this) {
        CASUAL -> "Casual"
        NEUTRAL -> "Neutral"
        FORMAL -> "Formal"
    }
    
    /**
     * Icon description for UI
     */
    fun icon(): String = when (this) {
        CASUAL -> "😊"
        NEUTRAL -> "💬"
        FORMAL -> "🎩"
    }
}

