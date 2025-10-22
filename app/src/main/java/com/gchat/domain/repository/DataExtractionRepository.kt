package com.gchat.domain.repository

import com.gchat.domain.model.BatchExtractionResult
import com.gchat.domain.model.ExtractedData
import com.gchat.util.Resource

/**
 * Repository for extracting intelligent data from messages
 */
interface DataExtractionRepository {
    
    /**
     * Extract entities from a single message
     * 
     * @param messageId ID of the message
     * @param text Message text to analyze
     * @param conversationId Optional conversation ID
     * @return Resource containing extracted data
     */
    suspend fun extractFromMessage(
        messageId: String,
        text: String,
        conversationId: String? = null
    ): Resource<ExtractedData>
    
    /**
     * Extract entities from multiple messages in a conversation
     * 
     * @param messages List of message IDs and texts
     * @param conversationId Conversation ID
     * @return Resource containing batch extraction result
     */
    suspend fun extractFromBatch(
        messages: List<Pair<String, String>>, // Pair<messageId, text>
        conversationId: String
    ): Resource<BatchExtractionResult>
}

