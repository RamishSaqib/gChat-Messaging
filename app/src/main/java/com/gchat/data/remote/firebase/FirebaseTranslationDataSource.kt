package com.gchat.data.remote.firebase

import com.gchat.domain.model.Translation
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for translation operations
 * 
 * Calls Cloud Functions for AI-powered translation
 */
@Singleton
class FirebaseTranslationDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    /**
     * Translate text using Cloud Function
     */
    suspend fun translateMessage(
        messageId: String,
        text: String,
        sourceLanguage: String?,
        targetLanguage: String
    ): Result<Translation> {
        return try {
            val data = hashMapOf(
                "text" to text,
                "targetLanguage" to targetLanguage
            )
            
            // Add source language if provided
            sourceLanguage?.let {
                data["sourceLanguage"] = it
            }
            
            val result = functions
                .getHttpsCallable("translateMessage")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from translation service"))
            
            val translatedText = resultData["translatedText"] as? String
                ?: return Result.failure(Exception("Translation text missing"))
            
            val detectedSourceLang = resultData["sourceLanguage"] as? String ?: sourceLanguage ?: "auto"
            val cached = resultData["cached"] as? Boolean ?: false
            
            val translation = Translation(
                id = generateTranslationId(messageId, targetLanguage),
                messageId = messageId,
                originalText = text,
                translatedText = translatedText,
                sourceLanguage = detectedSourceLang,
                targetLanguage = targetLanguage,
                timestamp = System.currentTimeMillis(),
                cached = cached
            )
            
            Result.success(translation)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseTranslation", "Translation error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Detect language of text using Cloud Function
     * 
     * @param text The text to detect language for
     * @param senderLanguageHint Optional sender's preferred language for disambiguation
     */
    suspend fun detectLanguage(text: String, senderLanguageHint: String? = null): Result<String> {
        return try {
            val data = hashMapOf(
                "text" to text
            )
            
            // Add sender language hint if provided
            senderLanguageHint?.let {
                data["senderLanguageHint"] = it
            }
            
            val result = functions
                .getHttpsCallable("detectLanguage")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from language detection"))
            
            val languageCode = resultData["languageCode"] as? String
                ?: return Result.failure(Exception("Language code missing"))
            
            Result.success(languageCode)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseTranslation", "Language detection error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate deterministic translation ID for caching
     */
    private fun generateTranslationId(messageId: String, targetLanguage: String): String {
        return "${messageId}_$targetLanguage"
    }
}

