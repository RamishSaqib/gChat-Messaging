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
        language: String,
        mode: String
    ): Result<CulturalContextResult> {
        android.util.Log.d(
            "CulturalContextRepo",
            "Getting cultural context for message: $messageId (language: $language, mode: $mode)"
        )

        return firebaseCulturalContextDataSource.getCulturalContext(
            messageId = messageId,
            text = text,
            language = language,
            mode = mode
        ).onSuccess { result ->
            android.util.Log.d(
                "CulturalContextRepo",
                "Cultural context fetched: ${result.contexts.size} items (cached: ${result.cached})"
            )
        }.onFailure { error ->
            android.util.Log.e("CulturalContextRepo", "Failed to get cultural context", error)
        }
    }
}

