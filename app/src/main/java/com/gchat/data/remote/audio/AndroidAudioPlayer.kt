package com.gchat.data.remote.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.gchat.domain.repository.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AndroidAudioPlayer - Play audio using MediaPlayer
 * 
 * Supports playback speed control (0.5x, 1x, 1.5x, 2x)
 * Emits playback progress
 */
@Singleton
class AndroidAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    private var currentSpeed: Float = 1.0f
    
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()
    
    /**
     * Play audio from URL
     */
    fun playAudio(url: String, speed: Float = 1.0f): Result<Unit> {
        return try {
            // Stop current playback if any
            stop()
            
            currentUrl = url
            currentSpeed = speed
            
            // Create and configure MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                
                // Set playback speed (API 23+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(speed)
                }
                
                setOnPreparedListener { mp ->
                    mp.start()
                    updatePlaybackState()
                    Log.d("AndroidAudioPlayer", "Playback started: $url at ${speed}x speed")
                }
                
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.Completed
                    Log.d("AndroidAudioPlayer", "Playback completed")
                }
                
                setOnErrorListener { _, what, extra ->
                    val error = "MediaPlayer error: what=$what, extra=$extra"
                    Log.e("AndroidAudioPlayer", error)
                    _playbackState.value = PlaybackState.Error(error)
                    true
                }
                
                prepareAsync()
            }
            
            Result.success(Unit)
        } catch (e: IOException) {
            Log.e("AndroidAudioPlayer", "Failed to play audio", e)
            _playbackState.value = PlaybackState.Error("Failed to load audio: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Unexpected error playing audio", e)
            _playbackState.value = PlaybackState.Error("Unexpected error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    
                    val currentPosition = mp.currentPosition / 1000 // Convert to seconds
                    val duration = mp.duration / 1000 // Convert to seconds
                    val progress = currentPosition.toFloat() / duration.toFloat()
                    
                    _playbackState.value = PlaybackState.Paused(
                        currentPosition = currentPosition,
                        duration = duration,
                        progress = progress
                    )
                    
                    Log.d("AndroidAudioPlayer", "Playback paused at ${currentPosition}s")
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Error pausing playback", e)
        }
    }
    
    /**
     * Resume playback
     */
    fun resume() {
        try {
            mediaPlayer?.let { mp ->
                if (!mp.isPlaying) {
                    mp.start()
                    updatePlaybackState()
                    Log.d("AndroidAudioPlayer", "Playback resumed")
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Error resuming playback", e)
        }
    }
    
    /**
     * Stop playback and release resources
     */
    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Error stopping playback", e)
        }
        
        mediaPlayer = null
        currentUrl = null
        _playbackState.value = PlaybackState.Idle
        
        Log.d("AndroidAudioPlayer", "Playback stopped")
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionSeconds: Int) {
        try {
            mediaPlayer?.let { mp ->
                val positionMs = positionSeconds * 1000
                mp.seekTo(positionMs)
                updatePlaybackState()
                Log.d("AndroidAudioPlayer", "Seeked to ${positionSeconds}s")
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Error seeking", e)
        }
    }
    
    /**
     * Set playback speed
     */
    fun setSpeed(speed: Float) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                mediaPlayer?.let { mp ->
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    currentSpeed = speed
                    Log.d("AndroidAudioPlayer", "Speed set to ${speed}x")
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Error setting speed", e)
        }
    }
    
    /**
     * Update playback state (call periodically from ViewModel)
     */
    fun updatePlaybackState() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    val currentPosition = mp.currentPosition / 1000 // Convert to seconds
                    val duration = mp.duration / 1000 // Convert to seconds
                    val progress = if (duration > 0) {
                        currentPosition.toFloat() / duration.toFloat()
                    } else {
                        0f
                    }
                    
                    val newState = PlaybackState.Playing(
                        currentPosition = currentPosition,
                        duration = duration,
                        progress = progress
                    )
                    
                    Log.d("AndroidAudioPlayer", "Updating state to: $newState")
                    _playbackState.value = newState
                }
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioPlayer", "Error updating playback state", e)
        }
    }
    
    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get current playback position in seconds
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition?.div(1000) ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get duration in seconds
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration?.div(1000) ?: 0
        } catch (e: Exception) {
            0
        }
    }
}

