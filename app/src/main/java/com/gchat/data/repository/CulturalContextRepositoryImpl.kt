package com.gchat.data.repository

import com.gchat.data.remote.firebase.FirebaseCulturalContextDataSource
import com.gchat.domain.model.CulturalContextResult
import com.gchat.domain.repository.CulturalContextRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CulturalContextRepository.
 * 
 * Fetches cultural context from Firebase Cloud Functions.
 * Results are cached server-side in Firestore for 30 days.
 */
@Singleton
class CulturalContextRepositoryImpl @Inject constructor(
    private val firebaseCulturalContextDataSource: FirebaseCulturalContextDataSource
) : CulturalContextRepository {

    override suspend fun getCulturalContext(
        messageId: String,
        text: String,
        language: String
    ): Result<CulturalContextResult> {
        return firebaseCulturalContextDataSource.getCulturalContext(
            messageId = messageId,
            text = text,
            language = language
        ).onFailure { error ->
            android.util.Log.e("CulturalContextRepo", "Failed to get cultural context", error)
        }
    }
}

