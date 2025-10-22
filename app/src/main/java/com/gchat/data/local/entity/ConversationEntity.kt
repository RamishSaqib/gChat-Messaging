package com.gchat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Conversation
 */
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["id"], unique = true)]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String, // ONE_ON_ONE, GROUP
    val participants: String, // JSON array of user IDs
    val name: String?,
    val iconUrl: String?,
    val groupAdmins: String = "[]", // JSON array of admin user IDs
    val nicknames: String = "{}", // JSON object of user ID -> nickname
    val lastMessageId: String?,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long,
    val lastMessageSenderId: String?,
    val unreadCount: Int,
    val updatedAt: Long,
    val createdAt: Long,
    val autoTranslateEnabled: Boolean,
    val creatorId: String? = null, // ID of user who created the conversation
    val deletedAt: String = "{}" // JSON map of userId -> deletion timestamp
)

