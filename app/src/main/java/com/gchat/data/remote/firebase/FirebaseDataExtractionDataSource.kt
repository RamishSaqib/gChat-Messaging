package com.gchat.data.remote.firebase

import com.gchat.domain.model.BatchExtractionResult
import com.gchat.domain.model.EntityType
import com.gchat.domain.model.ExtractedData
import com.gchat.domain.model.ExtractedEntity
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataExtractionDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    /**
     * Extract entities from a single message
     */
    suspend fun extractFromMessage(
        messageId: String,
        text: String,
        conversationId: String?
    ): ExtractedData {
        val data = hashMapOf(
            "text" to text,
            "messageId" to messageId
        )
        if (conversationId != null) {
            data["conversationId"] = conversationId
        }
        
        val result = functions
            .getHttpsCallable("extractIntelligentData")
            .call(data)
            .await()
        
        val responseData = result.data as? Map<String, Any>
            ?: throw Exception("Invalid extraction response")
        
        return parseExtractedData(responseData, messageId, conversationId)
    }
    
    /**
     * Extract entities from multiple messages
     */
    suspend fun extractFromBatch(
        messages: List<Pair<String, String>>,
        conversationId: String
    ): BatchExtractionResult {
        val messagesData = messages.map { (id, text) ->
            mapOf(
                "id" to id,
                "text" to text
            )
        }
        
        val data = hashMapOf(
            "messages" to messagesData,
            "conversationId" to conversationId
        )
        
        val result = functions
            .getHttpsCallable("extractBatchData")
            .call(data)
            .await()
        
        val responseData = result.data as? Map<String, Any>
            ?: throw Exception("Invalid batch extraction response")
        
        return parseBatchExtractionResult(responseData, conversationId)
    }
    
    /**
     * Parse extracted data from response
     */
    private fun parseExtractedData(
        data: Map<String, Any>,
        messageId: String,
        conversationId: String?
    ): ExtractedData {
        val entitiesData = data["entities"] as? List<Map<String, Any>> ?: emptyList()
        val extractedAt = (data["extractedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        
        val entities = entitiesData.mapNotNull { entityData ->
            parseEntity(entityData, messageId)
        }
        
        return ExtractedData(
            messageId = messageId,
            conversationId = conversationId,
            entities = entities,
            extractedAt = extractedAt
        )
    }
    
    /**
     * Parse batch extraction result
     */
    private fun parseBatchExtractionResult(
        data: Map<String, Any>,
        conversationId: String
    ): BatchExtractionResult {
        val results = data["results"] as? List<Map<String, Any>> ?: emptyList()
        val totalEntities = (data["totalEntities"] as? Number)?.toInt() ?: 0
        val extractedAt = (data["extractedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        
        val extractedDataList = results.map { resultData ->
            val messageId = resultData["messageId"] as? String ?: ""
            val entitiesData = resultData["entities"] as? List<Map<String, Any>> ?: emptyList()
            
            val entities = entitiesData.mapNotNull { entityData ->
                parseEntity(entityData, messageId)
            }
            
            ExtractedData(
                messageId = messageId,
                conversationId = conversationId,
                entities = entities,
                extractedAt = extractedAt
            )
        }
        
        return BatchExtractionResult(
            conversationId = conversationId,
            results = extractedDataList,
            totalEntities = totalEntities,
            extractedAt = extractedAt
        )
    }
    
    /**
     * Parse individual entity from response
     */
    private fun parseEntity(
        data: Map<String, Any>,
        messageId: String
    ): ExtractedEntity? {
        val typeString = data["type"] as? String ?: return null
        val text = data["text"] as? String ?: return null
        val confidence = (data["confidence"] as? Number)?.toFloat() ?: 0f
        val metadata = data["metadata"] as? Map<String, Any> ?: emptyMap()
        
        return when (typeString) {
            "ACTION_ITEM" -> parseActionItem(text, confidence, messageId, metadata)
            "DATE_TIME" -> parseDateTime(text, confidence, messageId, metadata)
            "CONTACT" -> parseContact(text, confidence, messageId, metadata)
            "LOCATION" -> parseLocation(text, confidence, messageId, metadata)
            else -> null
        }
    }
    
    /**
     * Parse action item entity
     */
    private fun parseActionItem(
        text: String,
        confidence: Float,
        messageId: String,
        metadata: Map<String, Any>
    ): ExtractedEntity.ActionItem {
        val task = metadata["task"] as? String ?: text
        val priorityString = metadata["priority"] as? String
        val priority = when (priorityString?.lowercase()) {
            "low" -> ExtractedEntity.ActionItem.Priority.LOW
            "high" -> ExtractedEntity.ActionItem.Priority.HIGH
            else -> ExtractedEntity.ActionItem.Priority.MEDIUM
        }
        val assignedTo = metadata["assignedTo"] as? String
        val dueDateString = metadata["dueDate"] as? String
        val dueDate = dueDateString?.let { parseISODate(it) }
        
        return ExtractedEntity.ActionItem(
            text = text,
            confidence = confidence,
            messageId = messageId,
            task = task,
            priority = priority,
            assignedTo = assignedTo,
            dueDate = dueDate
        )
    }
    
    /**
     * Parse date/time entity
     */
    private fun parseDateTime(
        text: String,
        confidence: Float,
        messageId: String,
        metadata: Map<String, Any>
    ): ExtractedEntity.DateTime {
        val dateTimeString = metadata["dateTime"] as? String
            ?: return ExtractedEntity.DateTime(
                text = text,
                confidence = confidence,
                messageId = messageId,
                dateTime = System.currentTimeMillis()
            )
        
        val dateTime = parseISODate(dateTimeString) ?: System.currentTimeMillis()
        val isRange = metadata["isRange"] as? Boolean ?: false
        val endDateTimeString = metadata["endDateTime"] as? String
        val endDateTime = endDateTimeString?.let { parseISODate(it) }
        val description = metadata["description"] as? String
        
        return ExtractedEntity.DateTime(
            text = text,
            confidence = confidence,
            messageId = messageId,
            dateTime = dateTime,
            isRange = isRange,
            endDateTime = endDateTime,
            description = description
        )
    }
    
    /**
     * Parse contact entity
     */
    private fun parseContact(
        text: String,
        confidence: Float,
        messageId: String,
        metadata: Map<String, Any>
    ): ExtractedEntity.Contact {
        val name = metadata["name"] as? String
        val email = metadata["email"] as? String
        val phone = metadata["phone"] as? String
        
        return ExtractedEntity.Contact(
            text = text,
            confidence = confidence,
            messageId = messageId,
            name = name,
            email = email,
            phone = phone
        )
    }
    
    /**
     * Parse location entity
     */
    private fun parseLocation(
        text: String,
        confidence: Float,
        messageId: String,
        metadata: Map<String, Any>
    ): ExtractedEntity.Location {
        val address = metadata["address"] as? String ?: text
        val latitude = (metadata["latitude"] as? Number)?.toDouble()
        val longitude = (metadata["longitude"] as? Number)?.toDouble()
        val placeName = metadata["placeName"] as? String
        
        return ExtractedEntity.Location(
            text = text,
            confidence = confidence,
            messageId = messageId,
            address = address,
            latitude = latitude,
            longitude = longitude,
            placeName = placeName
        )
    }
    
    /**
     * Parse ISO 8601 date string to Unix timestamp
     */
    private fun parseISODate(isoString: String): Long? {
        return try {
            // Try parsing with various formats
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd"
            )
            
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.parse(isoString)?.time
                } catch (e: Exception) {
                    // Try next format
                }
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.e("FirebaseDataExtraction", "Failed to parse date: $isoString", e)
            null
        }
    }
}

