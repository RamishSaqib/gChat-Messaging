package com.gchat.data.repository

import com.gchat.data.remote.firestore.FirestoreTypingDataSource
import com.gchat.domain.model.TypingIndicator
import com.gchat.domain.repository.TypingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TypingRepository
 */
@Singleton
class TypingRepositoryImpl @Inject constructor(
    private val firestoreTypingDataSource: FirestoreTypingDataSource
) : TypingRepository {
    
    override suspend fun setTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return firestoreTypingDataSource.setTypingStatus(conversationId, userId, isTyping)
    }
    
    override fun observeTypingIndicators(conversationId: String): Flow<List<TypingIndicator>> {
        return firestoreTypingDataSource.observeTypingIndicators(conversationId)
    }
}

