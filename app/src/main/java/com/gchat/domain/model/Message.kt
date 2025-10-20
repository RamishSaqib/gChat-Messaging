package com.gchat.domain.model

/**
 * Message domain model
 * 
 * Represents a single message in a conversation
 */
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val type: MessageType = MessageType.TEXT,
    val text: String? = null,
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val readBy: List<String> = emptyList(),
    val translation: Translation? = null,
    val culturalContext: String? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM // For "User joined", "Group created", etc.
}

enum class MessageStatus {
    SENDING,    // Optimistic UI - message being sent
    SENT,       // Successfully sent to server
    DELIVERED,  // Delivered to recipient device
    READ,       // Read by recipient
    FAILED      // Failed to send
}

data class Translation(
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val cachedAt: Long = System.currentTimeMillis()
)

