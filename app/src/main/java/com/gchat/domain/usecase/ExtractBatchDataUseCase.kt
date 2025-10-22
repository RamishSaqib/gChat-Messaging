package com.gchat.domain.usecase

import com.gchat.domain.model.BatchExtractionResult
import com.gchat.domain.repository.DataExtractionRepository
import com.gchat.util.Resource
import javax.inject.Inject

/**
 * Use case for extracting intelligent data from multiple messages
 */
class ExtractBatchDataUseCase @Inject constructor(
    private val dataExtractionRepository: DataExtractionRepository
) {
    suspend operator fun invoke(
        messages: List<Pair<String, String>>, // Pair<messageId, text>
        conversationId: String
    ): Resource<BatchExtractionResult> {
        return dataExtractionRepository.extractFromBatch(messages, conversationId)
    }
}

