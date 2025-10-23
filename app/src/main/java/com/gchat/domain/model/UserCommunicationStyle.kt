package com.gchat.domain.model

/**
 * User Communication Style
 * 
 * Analyzed from user's message history to personalize smart reply suggestions
 */
data class UserCommunicationStyle(
    val avgMessageLength: Int, // Average words per message
    val emojiUsage: EmojiUsage,
    val tone: Tone,
    val commonPhrases: List<String> = emptyList(), // Frequently used phrases
    val usesContractions: Boolean = true, // "can't" vs "cannot"
    val punctuationStyle: PunctuationStyle = PunctuationStyle.STANDARD
)

/**
 * Frequency of emoji usage
 */
enum class EmojiUsage {
    FREQUENT,   // 2+ emojis per message on average
    OCCASIONAL, // 0.5-2 emojis per message
    RARE        // Less than 0.5 emojis per message
}

/**
 * Overall communication tone
 */
enum class Tone {
    CASUAL,          // Informal, lots of slang, contractions, emojis
    CONVERSATIONAL,  // Balanced, natural conversation
    FORMAL           // Professional, proper grammar, minimal emojis
}

/**
 * Punctuation patterns
 */
enum class PunctuationStyle {
    MINIMAL,     // Few punctuation marks, short sentences
    STANDARD,    // Normal punctuation usage
    EXPRESSIVE   // Multiple exclamation marks, question marks, ellipses
}

