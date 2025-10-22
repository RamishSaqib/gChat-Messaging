package com.gchat.domain.usecase

import com.gchat.domain.model.ExtractedData
import com.gchat.domain.repository.DataExtractionRepository
import javax.inject.Inject

/**
 * Use case for extracting intelligent data from a single message
 */
class ExtractDataFromMessageUseCase @Inject constructor(
    private val dataExtractionRepository: DataExtractionRepository
) {
    suspend operator fun invoke(
        messageId: String,
        text: String,
        conversationId: String? = null
    ): Result<ExtractedData> {
        return dataExtractionRepository.extractFromMessage(messageId, text, conversationId)
    }
}

