package com.gchat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Message
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["id"], unique = true)
    ]
    // Removed foreign key constraint to prevent cascade deletion when conversation is updated
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val type: String, // TEXT, IMAGE, SYSTEM
    val text: String?,
    val mediaUrl: String?,
    val timestamp: Long,
    val status: String, // SENDING, SENT, DELIVERED, READ, FAILED
    val readBy: String, // JSON array of user IDs
    val translatedText: String?,
    val translationSourceLang: String?,
    val translationTargetLang: String?,
    val culturalContext: String?
)

