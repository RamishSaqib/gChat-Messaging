package com.gchat.domain.model

/**
 * Typing indicator model
 * 
 * Represents a user typing in a conversation
 */
data class TypingIndicator(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

