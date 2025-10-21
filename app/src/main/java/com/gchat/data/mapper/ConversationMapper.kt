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
            groupAdmins = try {
                json.decodeFromString(entity.groupAdmins)
            } catch (e: Exception) {
                emptyList()
            },
            nicknames = try {
                json.decodeFromString(entity.nicknames)
            } catch (e: Exception) {
                emptyMap()
            },
            lastMessage = lastMessage,
            unreadCount = entity.unreadCount,
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt,
            autoTranslateEnabled = entity.autoTranslateEnabled,
            creatorId = entity.creatorId,
            deletedBy = try {
                json.decodeFromString(entity.deletedBy)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
    
    fun toEntity(domain: Conversation): ConversationEntity {
        return ConversationEntity(
            id = domain.id,
            type = domain.type.name,
            participants = json.encodeToString(domain.participants),
            name = domain.name,
            iconUrl = domain.iconUrl,
            groupAdmins = json.encodeToString(domain.groupAdmins),
            nicknames = json.encodeToString(domain.nicknames),
            lastMessageId = domain.lastMessage?.id,
            lastMessageText = domain.lastMessage?.text,
            lastMessageTimestamp = domain.lastMessage?.timestamp ?: domain.updatedAt,
            lastMessageSenderId = domain.lastMessage?.senderId,
            unreadCount = domain.unreadCount,
            updatedAt = domain.updatedAt,
            createdAt = domain.createdAt,
            autoTranslateEnabled = domain.autoTranslateEnabled,
            creatorId = domain.creatorId,
            deletedBy = json.encodeToString(domain.deletedBy)
        )
    }
    
    fun fromFirestore(document: DocumentSnapshot): Conversation? {
        return try {
            val participantsList = document.get("participants") as? List<*>
            val groupAdminsList = document.get("groupAdmins") as? List<*>
            val nicknamesMap = document.get("nicknames") as? Map<*, *>
            val lastMessageMap = document.get("lastMessage") as? Map<*, *>
            
            // Parse last message if it exists
            val lastMessage = lastMessageMap?.let {
                try {
                    com.gchat.domain.model.Message(
                        id = it["id"] as? String ?: "",
                        conversationId = document.id,
                        senderId = it["senderId"] as? String ?: "",
                        type = com.gchat.domain.model.MessageType.TEXT, // Default type for preview
                        text = it["text"] as? String,
                        mediaUrl = null,
                        timestamp = (it["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        status = com.gchat.domain.model.MessageStatus.SENT,
                        readBy = emptyMap()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val deletedByList = document.get("deletedBy") as? List<*>
            
            Conversation(
                id = document.id,
                type = ConversationType.valueOf(document.getString("type") ?: "ONE_ON_ONE"),
                participants = participantsList?.mapNotNull { it as? String } ?: emptyList(),
                name = document.getString("name"),
                iconUrl = document.getString("iconUrl"),
                groupAdmins = groupAdminsList?.mapNotNull { it as? String } ?: emptyList(),
                nicknames = nicknamesMap?.mapNotNull { (k, v) ->
                    (k as? String)?.let { key -> (v as? String)?.let { value -> key to value } }
                }?.toMap() ?: emptyMap(),
                lastMessage = lastMessage,
                unreadCount = 0, // Calculated client-side
                updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis(),
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                autoTranslateEnabled = false, // User preference, stored locally
                creatorId = document.getString("creatorId"),
                deletedBy = deletedByList?.mapNotNull { it as? String } ?: emptyList()
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
            "groupAdmins" to conversation.groupAdmins,
            "nicknames" to conversation.nicknames,
            "lastMessage" to lastMessageMap,
            "updatedAt" to conversation.updatedAt,
            "createdAt" to conversation.createdAt,
            "creatorId" to conversation.creatorId,
            "deletedBy" to conversation.deletedBy
        )
    }
}

