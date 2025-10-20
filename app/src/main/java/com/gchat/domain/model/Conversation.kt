package com.gchat.domain.model

/**
 * Conversation domain model
 * 
 * Represents a chat conversation (one-on-one or group)
 */
data class Conversation(
    val id: String,
    val type: ConversationType,
    val participants: List<String>, // User IDs
    val name: String? = null, // For groups
    val iconUrl: String? = null, // For groups
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val autoTranslateEnabled: Boolean = false
) {
    /**
     * Get display name for the conversation
     * For one-on-one chats, returns the other participant's name
     * For groups, returns the group name
     */
    fun getDisplayName(currentUserId: String, users: Map<String, User>): String {
        return when (type) {
            ConversationType.GROUP -> name ?: "Group Chat"
            ConversationType.ONE_ON_ONE -> {
                val otherUserId = participants.firstOrNull { it != currentUserId }
                users[otherUserId]?.displayName ?: "Unknown User"
            }
        }
    }
    
    /**
     * Get the other participant in a one-on-one conversation
     */
    fun getOtherParticipantId(currentUserId: String): String? {
        if (type != ConversationType.ONE_ON_ONE) return null
        return participants.firstOrNull { it != currentUserId }
    }
}

enum class ConversationType {
    ONE_ON_ONE,
    GROUP
}

