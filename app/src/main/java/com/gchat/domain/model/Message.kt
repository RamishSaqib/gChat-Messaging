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
    val audioDuration: Int? = null, // Duration in seconds
    val audioWaveform: List<Float>? = null, // Waveform data for visualization
    val transcription: String? = null, // AI transcription of voice message
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val readBy: Map<String, Long> = emptyMap(), // userId -> readTimestamp
    val translation: Translation? = null,
    val culturalContext: String? = null,
    val reactions: Map<String, List<String>> = emptyMap() // emoji -> list of userIds
) {
    /**
     * Check if message has been read by a specific user
     */
    fun isReadBy(userId: String): Boolean = readBy.containsKey(userId)
    
    /**
     * Get the timestamp when a specific user read the message
     */
    fun getReadTimestamp(userId: String): Long? = readBy[userId]
    
    /**
     * Check if message has been read by all participants (excluding sender)
     */
    fun isReadByAll(participantIds: List<String>): Boolean {
        val recipients = participantIds.filter { it != senderId }
        return recipients.all { readBy.containsKey(it) }
    }
    
    /**
     * Check if message has been read by at least one recipient
     */
    fun isReadByAny(): Boolean = readBy.isNotEmpty()
    
    /**
     * Get reaction counts for this message
     */
    fun getReactionCounts(): Map<String, Int> {
        return reactions.mapValues { it.value.size }
    }
    
    /**
     * Get the reaction emoji that a specific user added, or null if none
     */
    fun getUserReaction(userId: String): String? {
        return reactions.entries.firstOrNull { it.value.contains(userId) }?.key
    }
    
    /**
     * Check if this message has any reactions
     */
    fun hasReactions(): Boolean = reactions.isNotEmpty()
}

enum class MessageType {
    TEXT,
    IMAGE,
    AUDIO,
    SYSTEM // For "User joined", "Group created", etc.
}

enum class MessageStatus {
    SENDING,    // Optimistic UI - message being sent
    SENT,       // Successfully sent to server
    DELIVERED,  // Delivered to recipient device
    READ,       // Read by recipient
    FAILED      // Failed to send
}

