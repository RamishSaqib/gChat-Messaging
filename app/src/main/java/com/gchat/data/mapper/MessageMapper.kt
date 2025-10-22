package com.gchat.data.mapper

import com.gchat.data.local.entity.MessageEntity
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.gchat.domain.model.MessageType
import com.gchat.domain.model.Translation
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Maps Message between different representations
 */
object MessageMapper {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            type = MessageType.valueOf(entity.type),
            text = entity.text,
            mediaUrl = entity.mediaUrl,
            timestamp = entity.timestamp,
            status = MessageStatus.valueOf(entity.status),
            readBy = try {
                // Parse JSON map of userId -> timestamp
                json.decodeFromString<Map<String, Long>>(entity.readBy)
            } catch (e: Exception) {
                emptyMap()
            },
            translation = null, // Translations are now cached separately in TranslationEntity
            culturalContext = entity.culturalContext
        )
    }
    
    fun toEntity(domain: Message): MessageEntity {
        return MessageEntity(
            id = domain.id,
            conversationId = domain.conversationId,
            senderId = domain.senderId,
            type = domain.type.name,
            text = domain.text,
            mediaUrl = domain.mediaUrl,
            timestamp = domain.timestamp,
            status = domain.status.name,
            readBy = json.encodeToString(domain.readBy),
            translatedText = domain.translation?.translatedText,
            translationSourceLang = domain.translation?.sourceLanguage,
            translationTargetLang = domain.translation?.targetLanguage,
            culturalContext = domain.culturalContext
        )
    }
    
    fun fromFirestore(document: DocumentSnapshot): Message? {
        return try {
            // Try to parse readBy as a map (new format: userId -> timestamp)
            val readByMap = try {
                (document.get("readBy") as? Map<*, *>)?.mapNotNull { (key, value) ->
                    val userId = key as? String
                    val timestamp = when (value) {
                        is Long -> value
                        is Number -> value.toLong()
                        else -> null
                    }
                    if (userId != null && timestamp != null) userId to timestamp else null
                }?.toMap() ?: emptyMap()
            } catch (e: Exception) {
                // Fallback: if it's a list (old format), convert to map with current timestamp
                val readByList = document.get("readBy") as? List<*>
                readByList?.mapNotNull { it as? String }
                    ?.associateWith { System.currentTimeMillis() } ?: emptyMap()
            }
            
            Message(
                id = document.id,
                conversationId = document.getString("conversationId") ?: return null,
                senderId = document.getString("senderId") ?: return null,
                type = MessageType.valueOf(document.getString("type") ?: "TEXT"),
                text = document.getString("text"),
                mediaUrl = document.getString("mediaUrl"),
                timestamp = document.getLong("timestamp") ?: System.currentTimeMillis(),
                status = MessageStatus.valueOf(document.getString("status") ?: "SENT"),
                readBy = readByMap
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun toFirestore(message: Message): Map<String, Any?> {
        return mapOf(
            "conversationId" to message.conversationId,
            "senderId" to message.senderId,
            "type" to message.type.name,
            "text" to message.text,
            "mediaUrl" to message.mediaUrl,
            "timestamp" to message.timestamp,
            "status" to message.status.name,
            "readBy" to message.readBy
        )
    }
}

