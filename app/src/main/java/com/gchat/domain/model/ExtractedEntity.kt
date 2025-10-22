package com.gchat.domain.model

/**
 * Types of entities that can be extracted from messages
 */
enum class EntityType {
    ACTION_ITEM,
    DATE_TIME,
    CONTACT,
    LOCATION
}

/**
 * Base sealed class for extracted entities
 */
sealed class ExtractedEntity {
    abstract val type: EntityType
    abstract val text: String
    abstract val confidence: Float
    abstract val messageId: String
    
    /**
     * Action item entity (tasks, todos)
     */
    data class ActionItem(
        override val text: String,
        override val confidence: Float,
        override val messageId: String,
        val task: String,
        val priority: Priority = Priority.MEDIUM,
        val assignedTo: String? = null,
        val dueDate: Long? = null
    ) : ExtractedEntity() {
        override val type = EntityType.ACTION_ITEM
        
        enum class Priority {
            LOW, MEDIUM, HIGH
        }
    }
    
    /**
     * Date/time entity (meetings, events, deadlines)
     */
    data class DateTime(
        override val text: String,
        override val confidence: Float,
        override val messageId: String,
        val dateTime: Long, // Unix timestamp in milliseconds
        val isRange: Boolean = false,
        val endDateTime: Long? = null,
        val description: String? = null
    ) : ExtractedEntity() {
        override val type = EntityType.DATE_TIME
    }
    
    /**
     * Contact entity (names with email/phone)
     */
    data class Contact(
        override val text: String,
        override val confidence: Float,
        override val messageId: String,
        val name: String? = null,
        val email: String? = null,
        val phone: String? = null
    ) : ExtractedEntity() {
        override val type = EntityType.CONTACT
    }
    
    /**
     * Location entity (addresses, places)
     */
    data class Location(
        override val text: String,
        override val confidence: Float,
        override val messageId: String,
        val address: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val placeName: String? = null
    ) : ExtractedEntity() {
        override val type = EntityType.LOCATION
    }
}

/**
 * Container for extracted data from a message
 */
data class ExtractedData(
    val messageId: String,
    val conversationId: String?,
    val entities: List<ExtractedEntity>,
    val extractedAt: Long
) {
    /**
     * Get entities by type
     */
    fun getEntitiesByType(type: EntityType): List<ExtractedEntity> {
        return entities.filter { it.type == type }
    }
    
    /**
     * Get action items
     */
    fun getActionItems(): List<ExtractedEntity.ActionItem> {
        return entities.filterIsInstance<ExtractedEntity.ActionItem>()
    }
    
    /**
     * Get date/time entities
     */
    fun getDateTimes(): List<ExtractedEntity.DateTime> {
        return entities.filterIsInstance<ExtractedEntity.DateTime>()
    }
    
    /**
     * Get contacts
     */
    fun getContacts(): List<ExtractedEntity.Contact> {
        return entities.filterIsInstance<ExtractedEntity.Contact>()
    }
    
    /**
     * Get locations
     */
    fun getLocations(): List<ExtractedEntity.Location> {
        return entities.filterIsInstance<ExtractedEntity.Location>()
    }
    
    /**
     * Check if any entities were extracted
     */
    fun hasEntities(): Boolean = entities.isNotEmpty()
    
    /**
     * Get count by type
     */
    fun getCountByType(type: EntityType): Int {
        return entities.count { it.type == type }
    }
}

/**
 * Result of batch extraction from multiple messages
 */
data class BatchExtractionResult(
    val conversationId: String,
    val results: List<ExtractedData>,
    val totalEntities: Int,
    val extractedAt: Long
) {
    /**
     * Get all entities across all messages
     */
    fun getAllEntities(): List<ExtractedEntity> {
        return results.flatMap { it.entities }
    }
    
    /**
     * Get all entities by type
     */
    fun getAllEntitiesByType(type: EntityType): List<ExtractedEntity> {
        return getAllEntities().filter { it.type == type }
    }
    
    /**
     * Get count by type
     */
    fun getCountByType(type: EntityType): Int {
        return getAllEntities().count { it.type == type }
    }
}

