package com.gchat.domain.usecase

import com.gchat.domain.repository.AudioRepository
import com.gchat.domain.repository.RecordingState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for recording voice messages
 */
class RecordVoiceMessageUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    /**
     * Start recording a voice message
     * 
     * @return Flow of recording state
     */
    operator fun invoke(): Flow<RecordingState> {
        return audioRepository.startRecording()
    }
}

