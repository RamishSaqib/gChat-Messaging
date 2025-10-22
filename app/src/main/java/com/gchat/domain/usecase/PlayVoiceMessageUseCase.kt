package com.gchat.domain.usecase

import com.gchat.domain.repository.AudioRepository
import com.gchat.domain.repository.PlaybackState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for playing voice messages
 */
class PlayVoiceMessageUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    /**
     * Play a voice message
     * 
     * @param audioUrl URL of the audio file
     * @param speed Playback speed (0.5x, 1x, 1.5x, 2x)
     * @return Flow of playback state
     */
    operator fun invoke(audioUrl: String, speed: Float = 1.0f): Flow<PlaybackState> {
        return audioRepository.playAudio(audioUrl, speed)
    }
    
    /**
     * Pause playback
     */
    suspend fun pause() {
        audioRepository.pauseAudio()
    }
    
    /**
     * Resume playback
     */
    suspend fun resume() {
        audioRepository.resumeAudio()
    }
    
    /**
     * Stop playback
     */
    suspend fun stop() {
        audioRepository.stopAudio()
    }
    
    /**
     * Seek to position
     */
    suspend fun seekTo(position: Int) {
        audioRepository.seekTo(position)
    }
}

