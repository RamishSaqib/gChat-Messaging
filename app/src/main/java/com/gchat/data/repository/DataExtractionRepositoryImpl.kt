package com.gchat.data.repository

import com.gchat.data.remote.firebase.FirebaseDataExtractionDataSource
import com.gchat.domain.model.BatchExtractionResult
import com.gchat.domain.model.ExtractedData
import com.gchat.domain.repository.DataExtractionRepository
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
    ): Result<ExtractedData> {
        return try {
            val extractedData = firebaseDataSource.extractFromMessage(messageId, text, conversationId)
            Result.success(extractedData)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun extractFromBatch(
        messages: List<Pair<String, String>>,
        conversationId: String
    ): Result<BatchExtractionResult> {
        return try {
            val result = firebaseDataSource.extractFromBatch(messages, conversationId)
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

