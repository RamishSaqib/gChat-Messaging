package com.gchat.data.mapper

import com.gchat.data.local.entity.ConversationEntity
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.ConversationType
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Maps Conversation between different representations
 */
object ConversationMapper {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    fun toDomain(entity: ConversationEntity, lastMessage: com.gchat.domain.model.Message? = null): Conversation {
        return Conversation(
            id = entity.id,
            type = ConversationType.valueOf(entity.type),
            participants = try {
                json.decodeFromString(entity.participants)
            } catch (e: Exception) {
                emptyList()
            },
            name = entity.name,
            iconUrl = entity.iconUrl,
            lastMessage = lastMessage,
            unreadCount = entity.unreadCount,
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt,
            autoTranslateEnabled = entity.autoTranslateEnabled
        )
    }
    
    fun toEntity(domain: Conversation): ConversationEntity {
        return ConversationEntity(
            id = domain.id,
            type = domain.type.name,
            participants = json.encodeToString(domain.participants),
            name = domain.name,
            iconUrl = domain.iconUrl,
            lastMessageId = domain.lastMessage?.id,
            lastMessageText = domain.lastMessage?.text,
            lastMessageTimestamp = domain.lastMessage?.timestamp ?: domain.updatedAt,
            lastMessageSenderId = domain.lastMessage?.senderId,
            unreadCount = domain.unreadCount,
            updatedAt = domain.updatedAt,
            createdAt = domain.createdAt,
            autoTranslateEnabled = domain.autoTranslateEnabled
        )
    }
    
    fun fromFirestore(document: DocumentSnapshot): Conversation? {
        return try {
            val participantsList = document.get("participants") as? List<*>
            val lastMessageMap = document.get("lastMessage") as? Map<*, *>
            
            Conversation(
                id = document.id,
                type = ConversationType.valueOf(document.getString("type") ?: "ONE_ON_ONE"),
                participants = participantsList?.mapNotNull { it as? String } ?: emptyList(),
                name = document.getString("name"),
                iconUrl = document.getString("iconUrl"),
                lastMessage = null, // Populated separately
                unreadCount = 0, // Calculated client-side
                updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis(),
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                autoTranslateEnabled = false // User preference, stored locally
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun toFirestore(conversation: Conversation): Map<String, Any?> {
        val lastMessageMap = conversation.lastMessage?.let {
            mapOf(
                "id" to it.id,
                "senderId" to it.senderId,
                "text" to it.text,
                "timestamp" to it.timestamp
            )
        }
        
        return mapOf(
            "type" to conversation.type.name,
            "participants" to conversation.participants,
            "name" to conversation.name,
            "iconUrl" to conversation.iconUrl,
            "lastMessage" to lastMessageMap,
            "updatedAt" to conversation.updatedAt,
            "createdAt" to conversation.createdAt
        )
    }
}

