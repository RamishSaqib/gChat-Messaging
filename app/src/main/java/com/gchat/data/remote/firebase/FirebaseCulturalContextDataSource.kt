package com.gchat.data.remote.firebase

import com.gchat.domain.model.CulturalContext
import com.gchat.domain.model.CulturalContextResult
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for cultural context operations.
 * Calls Cloud Functions for AI-powered cultural context analysis.
 */
@Singleton
class FirebaseCulturalContextDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {

    /**
     * Calls the Cloud Function to get cultural context for a message.
     * 
     * @param messageId The ID of the message
     * @param text The text to analyze
     * @param language The language of the text
     * @return Result containing CulturalContextResult
     */
    suspend fun getCulturalContext(
        messageId: String,
        text: String,
        language: String
    ): Result<CulturalContextResult> {
        return try {
            val data = hashMapOf(
                "messageId" to messageId,
                "text" to text,
                "language" to language
            )

            val result = functions
                .getHttpsCallable("getCulturalContext")
                .call(data)
                .await()

            val resultData = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from cultural context service"))

            val returnedMessageId = resultData["messageId"] as? String ?: messageId
            val contextsList = resultData["contexts"] as? List<Map<*, *>>
                ?: return Result.failure(Exception("Cultural contexts list missing or invalid"))
            val returnedLanguage = resultData["language"] as? String ?: language
            val cached = resultData["cached"] as? Boolean ?: false

            val culturalContexts = contextsList.mapNotNull { contextMap ->
                val phrase = contextMap["phrase"] as? String
                val literalTranslation = contextMap["literalTranslation"] as? String
                val actualMeaning = contextMap["actualMeaning"] as? String
                val culturalContext = contextMap["culturalContext"] as? String
                val examples = (contextMap["examples"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                if (phrase != null && actualMeaning != null && culturalContext != null) {
                    CulturalContext(
                        phrase = phrase,
                        literalTranslation = literalTranslation,
                        actualMeaning = actualMeaning,
                        culturalContext = culturalContext,
                        examples = examples
                    )
                } else {
                    null
                }
            }

            Result.success(
                CulturalContextResult(
                    messageId = returnedMessageId,
                    contexts = culturalContexts,
                    language = returnedLanguage,
                    cached = cached
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCulturalContext", "Cultural context error", e)
            Result.failure(e)
        }
    }
}

