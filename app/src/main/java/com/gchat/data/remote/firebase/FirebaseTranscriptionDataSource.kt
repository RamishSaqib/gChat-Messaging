package com.gchat.data.remote.firebase

import android.util.Log
import com.gchat.domain.repository.TranscriptionResult
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for audio transcription
 * 
 * Calls Cloud Functions to transcribe voice messages using OpenAI Whisper API
 */
@Singleton
class FirebaseTranscriptionDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    /**
     * Request transcription from Cloud Function
     */
    suspend fun transcribeAudio(audioUrl: String, messageId: String): Result<TranscriptionResult> {
        return try {
            Log.d("FirebaseTranscription", "Requesting transcription for message: $messageId")
            Log.d("FirebaseTranscription", "Audio URL: $audioUrl")
            
            // Check if user is authenticated
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("FirebaseTranscription", "User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }
            
            Log.d("FirebaseTranscription", "User authenticated: ${currentUser.uid}")
            
            val data = hashMapOf(
                "audioUrl" to audioUrl,
                "messageId" to messageId
            )
            
            val result = functions
                .getHttpsCallable("transcribeVoiceMessage")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from transcription function"))
            
            val text = resultData["text"] as? String
                ?: return Result.failure(Exception("No transcription text in response"))
            
            val language = resultData["language"] as? String ?: "unknown"
            
            val transcriptionResult = TranscriptionResult(
                text = text,
                language = language,
                messageId = messageId
            )
            
            Log.d("FirebaseTranscription", "Transcription successful: $text")
            
            Result.success(transcriptionResult)
        } catch (e: Exception) {
            Log.e("FirebaseTranscription", "Transcription failed", e)
            Log.e("FirebaseTranscription", "Error type: ${e.javaClass.simpleName}")
            Log.e("FirebaseTranscription", "Error message: ${e.message}")
            
            // Parse error message if available
            val errorMessage = e.message ?: "Unknown error"
            
            // Check for auth errors
            if (errorMessage.contains("UNAUTHENTICATED", ignoreCase = true)) {
                Result.failure(Exception("Authentication failed. Please sign out and sign back in."))
            } else if (errorMessage.contains("resource-exhausted") || errorMessage.contains("rate limit")) {
                Result.failure(Exception("Transcription rate limit exceeded. Please try again later."))
            } else if (errorMessage.contains("deadline-exceeded") || errorMessage.contains("timeout")) {
                Result.failure(Exception("Transcription timeout. The audio file may be too large."))
            } else {
                Result.failure(Exception("Transcription failed: $errorMessage"))
            }
        }
    }
    
    /**
     * Get cached transcription from Cloud Function
     */
    suspend fun getCachedTranscription(messageId: String): Result<TranscriptionResult?> {
        return try {
            Log.d("FirebaseTranscription", "Getting cached transcription for message: $messageId")
            
            val data = hashMapOf(
                "messageId" to messageId
            )
            
            val result = functions
                .getHttpsCallable("getTranscription")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            
            if (resultData == null) {
                // No cached transcription found
                return Result.success(null)
            }
            
            val text = resultData["text"] as? String
            val language = resultData["language"] as? String ?: "unknown"
            
            if (text != null) {
                val transcriptionResult = TranscriptionResult(
                    text = text,
                    language = language,
                    messageId = messageId
                )
                
                Log.d("FirebaseTranscription", "Cached transcription found")
                
                Result.success(transcriptionResult)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirebaseTranscription", "Failed to get cached transcription", e)
            
            // If not found, return null (not an error)
            if (e.message?.contains("not-found") == true) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }
}

