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
            smartRepliesEnabled = entity.smartRepliesEnabled,
            creatorId = entity.creatorId,
            deletedAt = try {
                json.decodeFromString<Map<String, Long>>(entity.deletedAt)
            } catch (e: Exception) {
                emptyMap()
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
            lastMessageType = domain.lastMessage?.type?.name,
            lastMessageMediaUrl = domain.lastMessage?.mediaUrl,
            lastMessageTimestamp = domain.lastMessage?.timestamp ?: domain.updatedAt,
            lastMessageSenderId = domain.lastMessage?.senderId,
            unreadCount = domain.unreadCount,
            updatedAt = domain.updatedAt,
            createdAt = domain.createdAt,
            autoTranslateEnabled = domain.autoTranslateEnabled,
            smartRepliesEnabled = domain.smartRepliesEnabled,
            creatorId = domain.creatorId,
            deletedAt = json.encodeToString(domain.deletedAt)
        )
    }
    
    fun fromFirestore(document: DocumentSnapshot): Conversation? {
        return try {
            val participantsList = document.get("participants") as? List<*>
            val groupAdminsList = document.get("groupAdmins") as? List<*>
            val nicknamesMap = document.get("nicknames") as? Map<*, *>
            val lastMessageMap = document.get("lastMessage") as? Map<*, *>
            val reactionNotificationsMap = document.get("reactionNotifications") as? Map<*, *>
            
            android.util.Log.d("ConversationMapper", "fromFirestore - reactionNotificationsMap: $reactionNotificationsMap")
            
            // Parse last message if it exists
            val lastMessage = lastMessageMap?.let {
                try {
                    val typeString = it["type"] as? String
                    val mediaUrlString = it["mediaUrl"] as? String
                    val originalSenderId = it["originalMessageSenderId"] as? String
                    android.util.Log.d("ConversationMapper", "fromFirestore lastMessage - type: $typeString, mediaUrl: $mediaUrlString, text: ${it["text"]}, originalMessageSenderId: $originalSenderId")
                    val messageType = try {
                        if (typeString != null) {
                            com.gchat.domain.model.MessageType.valueOf(typeString)
                        } else {
                            com.gchat.domain.model.MessageType.TEXT
                        }
                    } catch (e: Exception) {
                        com.gchat.domain.model.MessageType.TEXT
                    }
                    
                    com.gchat.domain.model.Message(
                        id = it["id"] as? String ?: "",
                        conversationId = document.id,
                        senderId = it["senderId"] as? String ?: "",
                        type = messageType,
                        text = it["text"] as? String,
                        mediaUrl = mediaUrlString,
                        timestamp = (it["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        status = com.gchat.domain.model.MessageStatus.SENT,
                        readBy = emptyMap(),
                        originalMessageSenderId = it["originalMessageSenderId"] as? String
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val deletedAtMap = document.get("deletedAt") as? Map<*, *>
            
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
                updatedAt = try {
                    (document.get("updatedAt") as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: document.getLong("updatedAt")
                        ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    document.getLong("updatedAt") ?: System.currentTimeMillis()
                },
                createdAt = try {
                    (document.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: document.getLong("createdAt")
                        ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    document.getLong("createdAt") ?: System.currentTimeMillis()
                },
                autoTranslateEnabled = document.getBoolean("autoTranslateEnabled") ?: false,
                smartRepliesEnabled = document.get("smartRepliesEnabled") as? Boolean, // null = use global
                creatorId = document.getString("creatorId"),
                deletedAt = deletedAtMap?.mapNotNull { (k, v) ->
                    (k as? String)?.let { key -> (v as? Number)?.toLong()?.let { value -> key to value } }
                }?.toMap() ?: emptyMap(),
                reactionNotifications = reactionNotificationsMap?.mapNotNull { (userIdKey, notificationData) ->
                    try {
                        android.util.Log.d("ConversationMapper", "Parsing reaction notification - userIdKey: $userIdKey, notificationData: $notificationData")
                        val userId = userIdKey as? String
                        android.util.Log.d("ConversationMapper", "userId cast: $userId")
                        if (userId == null) {
                            android.util.Log.e("ConversationMapper", "userId is null, returning null")
                            return@mapNotNull null
                        }
                        val dataMap = notificationData as? Map<*, *>
                        android.util.Log.d("ConversationMapper", "dataMap cast: $dataMap")
                        if (dataMap == null) {
                            android.util.Log.e("ConversationMapper", "dataMap is null, returning null")
                            return@mapNotNull null
                        }
                        val notification = com.gchat.domain.model.ReactionNotification(
                            text = dataMap["text"] as? String ?: return@mapNotNull null,
                            timestamp = (dataMap["timestamp"] as? Number)?.toLong() ?: return@mapNotNull null,
                            messageId = dataMap["messageId"] as? String ?: return@mapNotNull null,
                            reactorId = dataMap["reactorId"] as? String ?: return@mapNotNull null
                        )
                        android.util.Log.d("ConversationMapper", "Successfully created notification: $notification")
                        val result = userId to notification
                        android.util.Log.d("ConversationMapper", "Returning pair: $result")
                        result
                    } catch (e: Exception) {
                        android.util.Log.e("ConversationMapper", "Error parsing reaction notification", e)
                        null
                    }
                }?.toMap() ?: emptyMap()
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
                "type" to it.type.name,
                "mediaUrl" to it.mediaUrl,
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
            "autoTranslateEnabled" to conversation.autoTranslateEnabled,
            "smartRepliesEnabled" to conversation.smartRepliesEnabled,
            "creatorId" to conversation.creatorId,
            "deletedAt" to conversation.deletedAt
        )
    }
}

