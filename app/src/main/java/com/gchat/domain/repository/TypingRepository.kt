package com.gchat.domain.repository

import com.gchat.domain.model.TypingIndicator
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for typing indicator operations
 */
interface TypingRepository {
    suspend fun setTypingStatus(conversationId: String, userId: String, isTyping: Boolean): Result<Unit>
    fun observeTypingIndicators(conversationId: String): Flow<List<TypingIndicator>>
}

