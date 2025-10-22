package com.gchat.data.repository

import com.gchat.data.remote.firebase.FirebaseDataExtractionDataSource
import com.gchat.domain.model.BatchExtractionResult
import com.gchat.domain.model.ExtractedData
import com.gchat.domain.repository.DataExtractionRepository
import com.gchat.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExtractionRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataExtractionDataSource
) : DataExtractionRepository {
    
    override suspend fun extractFromMessage(
        messageId: String,
        text: String,
        conversationId: String?
    ): Resource<ExtractedData> {
        return try {
            val extractedData = firebaseDataSource.extractFromMessage(messageId, text, conversationId)
            Resource.Success(extractedData)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Failed to extract data from message")
        }
    }
    
    override suspend fun extractFromBatch(
        messages: List<Pair<String, String>>,
        conversationId: String
    ): Resource<BatchExtractionResult> {
        return try {
            val result = firebaseDataSource.extractFromBatch(messages, conversationId)
            Resource.Success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Failed to extract data from messages")
        }
    }
}

