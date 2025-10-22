package com.gchat.data.repository

import android.util.Log
import com.gchat.data.remote.audio.AndroidAudioPlayer
import com.gchat.data.remote.audio.AndroidAudioRecorder
import com.gchat.data.remote.firebase.FirebaseTranscriptionDataSource
import com.gchat.domain.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AudioRepository
 * 
 * Coordinates audio recording, playback, and transcription
 */
@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val audioRecorder: AndroidAudioRecorder,
    private val audioPlayer: AndroidAudioPlayer,
    private val transcriptionDataSource: FirebaseTranscriptionDataSource,
    private val mediaRepository: MediaRepository
) : AudioRepository {
    
    private var recordingUpdateJob: Job? = null
    private var playbackUpdateJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun startRecording(): Flow<RecordingState> {
        // Start recording
        val result = audioRecorder.startRecording()
        
        if (result.isSuccess) {
            // Start periodic updates for recording state
            recordingUpdateJob?.cancel()
            recordingUpdateJob = coroutineScope.launch {
                while (isActive) {
                    audioRecorder.updateRecordingState()
                    delay(100) // Update every 100ms
                }
            }
        }
        
        return audioRecorder.recordingState
    }
    
    override suspend fun stopRecording(): Result<AudioRecordingResult> {
        recordingUpdateJob?.cancel()
        recordingUpdateJob = null
        
        return audioRecorder.stopRecording()
    }
    
    override suspend fun cancelRecording() {
        recordingUpdateJob?.cancel()
        recordingUpdateJob = null
        
        audioRecorder.cancelRecording()
    }
    
    override fun playAudio(url: String, speed: Float): Flow<PlaybackState> {
        // Stop any existing playback
        playbackUpdateJob?.cancel()
        
        Log.d("AudioRepositoryImpl", "Starting playback for: $url")
        
        // Start playback (will begin update loop in MediaPlayer's OnPreparedListener)
        audioPlayer.playAudio(url, speed)
        
        // Start periodic updates for playback progress
        playbackUpdateJob = coroutineScope.launch {
            Log.d("AudioRepositoryImpl", "Update loop started")
            
            // Wait a bit for MediaPlayer to start
            delay(200)
            
            var loopCount = 0
            while (isActive) {
                val isPlaying = audioPlayer.isPlaying()
                loopCount++
                
                if (loopCount % 10 == 0) { // Log every 10th iteration (every second)
                    Log.d("AudioRepositoryImpl", "Update loop iteration $loopCount, isPlaying: $isPlaying")
                }
                
                if (isPlaying) {
                    audioPlayer.updatePlaybackState()
                }
                delay(100) // Update every 100ms
            }
            
            Log.d("AudioRepositoryImpl", "Update loop ended")
        }
        
        return audioPlayer.playbackState
    }
    
    override suspend fun pauseAudio() {
        playbackUpdateJob?.cancel()
        audioPlayer.pause()
    }
    
    override suspend fun resumeAudio() {
        audioPlayer.resume()
        
        // Restart playback updates
        playbackUpdateJob?.cancel()
        playbackUpdateJob = coroutineScope.launch {
            while (isActive) {
                if (audioPlayer.isPlaying()) {
                    audioPlayer.updatePlaybackState()
                }
                delay(100)
            }
        }
    }
    
    override suspend fun stopAudio() {
        playbackUpdateJob?.cancel()
        playbackUpdateJob = null
        
        audioPlayer.stop()
    }
    
    override suspend fun seekTo(position: Int) {
        audioPlayer.seekTo(position)
    }
    
    override suspend fun uploadAudio(file: File, userId: String): Result<String> {
        return try {
            // Generate unique filename
            val fileName = "audio_${System.currentTimeMillis()}.m4a"
            val path = "voice_messages/$userId/$fileName"
            
            Log.d("AudioRepository", "Uploading audio: $path")
            
            // Upload to Firebase Storage
            val uploadResult = mediaRepository.uploadAudio(file, path)
            
            if (uploadResult.isSuccess) {
                Log.d("AudioRepository", "Audio uploaded successfully")
            } else {
                Log.e("AudioRepository", "Audio upload failed", uploadResult.exceptionOrNull())
            }
            
            uploadResult
        } catch (e: Exception) {
            Log.e("AudioRepository", "Error uploading audio", e)
            Result.failure(e)
        }
    }
    
    override suspend fun transcribeAudio(audioUrl: String, messageId: String): Result<TranscriptionResult> {
        return try {
            Log.d("AudioRepository", "Requesting transcription for message: $messageId")
            
            transcriptionDataSource.transcribeAudio(audioUrl, messageId)
        } catch (e: Exception) {
            Log.e("AudioRepository", "Error requesting transcription", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCachedTranscription(messageId: String): TranscriptionResult? {
        return try {
            val result = transcriptionDataSource.getCachedTranscription(messageId)
            result.getOrNull()
        } catch (e: Exception) {
            Log.e("AudioRepository", "Error getting cached transcription", e)
            null
        }
    }
}

