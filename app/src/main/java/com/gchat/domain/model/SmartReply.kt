package com.gchat.domain.model

/**
 * Smart Reply Suggestion
 * 
 * Represents an AI-generated reply suggestion for a message
 */
data class SmartReply(
    val replyText: String,
    val confidence: Float = 1.0f, // 0.0-1.0, how well the reply fits the context
    val category: ReplyCategory = ReplyCategory.NEUTRAL
)

/**
 * Category of reply suggestion
 */
enum class ReplyCategory {
    AFFIRMATIVE, // "Yes", "Sure", "Sounds good", "Agreed"
    NEGATIVE,    // "No", "Sorry", "Can't", "Not available"
    QUESTION,    // Asking for clarification or more information
    NEUTRAL      // General responses, neutral acknowledgments
}

