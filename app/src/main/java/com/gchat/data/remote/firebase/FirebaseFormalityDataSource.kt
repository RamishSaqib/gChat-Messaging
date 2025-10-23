package com.gchat.data.remote.firebase

import com.gchat.domain.model.FormalityLevel
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for formality adjustment operations.
 * 
 * Calls Cloud Functions to adjust message formality using AI (GPT-4).
 */
@Singleton
class FirebaseFormalityDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    /**
     * Calls the Cloud Function to adjust message formality.
     * 
     * @param text The original text to adjust
     * @param language The language code (ISO 639-1)
     * @param targetFormality The desired formality level
     * @return Result containing the adjusted text or an error
     */
    suspend fun adjustFormality(
        text: String,
        language: String,
        targetFormality: FormalityLevel
    ): Result<String> {
        return try {
            val data = hashMapOf(
                "text" to text,
                "language" to language,
                "targetFormality" to targetFormality.name.lowercase() // casual, neutral, formal
            )
            
            android.util.Log.d("FirebaseFormality", "Calling adjustFormality function: text length=${text.length}, language=$language, formality=${targetFormality.name}")
            
            val result = functions
                .getHttpsCallable("adjustFormality")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from formality service"))
            
            val adjustedText = resultData["adjustedText"] as? String
                ?: return Result.failure(Exception("Adjusted text missing from response"))
            
            android.util.Log.d("FirebaseFormality", "Formality adjustment successful: original=${text.length} chars, adjusted=${adjustedText.length} chars")
            
            Result.success(adjustedText)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseFormality", "Formality adjustment error", e)
            Result.failure(e)
        }
    }
}

