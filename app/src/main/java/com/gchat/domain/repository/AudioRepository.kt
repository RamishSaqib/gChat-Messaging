package com.gchat.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Audio Repository
 * 
 * Handles audio recording, playback, and transcription
 */
interface AudioRepository {
    
    /**
     * Start audio recording
     * 
     * @return Flow of recording state (duration in seconds)
     */
    fun startRecording(): Flow<RecordingState>
    
    /**
     * Stop recording and return the audio file
     * 
     * @return Pair of File and waveform data
     */
    suspend fun stopRecording(): Result<AudioRecordingResult>
    
    /**
     * Cancel recording and clean up
     */
    suspend fun cancelRecording()
    
    /**
     * Play audio from URL
     * 
     * @param url Audio file URL
     * @param speed Playback speed (0.5x, 1x, 1.5x, 2x)
     * @return Flow of playback progress (0.0 to 1.0)
     */
    fun playAudio(url: String, speed: Float = 1.0f): Flow<PlaybackState>
    
    /**
     * Pause audio playback
     */
    suspend fun pauseAudio()
    
    /**
     * Resume audio playback
     */
    suspend fun resumeAudio()
    
    /**
     * Stop audio playback and clean up
     */
    suspend fun stopAudio()
    
    /**
     * Seek to position in audio
     * 
     * @param position Position in seconds
     */
    suspend fun seekTo(position: Int)
    
    /**
     * Upload audio file to Firebase Storage
     * 
     * @param file Audio file to upload
     * @param userId User ID for storage path
     * @return URL of uploaded audio
     */
    suspend fun uploadAudio(file: File, userId: String): Result<String>
    
    /**
     * Request transcription of audio file
     * 
     * @param audioUrl URL of audio file in Firebase Storage
     * @param messageId Message ID for caching
     * @return Transcription result
     */
    suspend fun transcribeAudio(audioUrl: String, messageId: String): Result<TranscriptionResult>
    
    /**
     * Get cached transcription if available
     * 
     * @param messageId Message ID
     * @return Cached transcription or null
     */
    suspend fun getCachedTranscription(messageId: String): TranscriptionResult?
}

/**
 * Recording state
 */
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val durationSeconds: Int, val amplitude: Float) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

/**
 * Audio recording result
 */
data class AudioRecordingResult(
    val file: File,
    val durationSeconds: Int,
    val waveformData: List<Float>
)

/**
 * Playback state
 */
sealed class PlaybackState {
    object Idle : PlaybackState()
    data class Playing(
        val currentPosition: Int, // seconds
        val duration: Int, // seconds
        val progress: Float // 0.0 to 1.0
    ) : PlaybackState()
    data class Paused(
        val currentPosition: Int,
        val duration: Int,
        val progress: Float
    ) : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

/**
 * Transcription result
 */
data class TranscriptionResult(
    val text: String,
    val language: String,
    val messageId: String
)

