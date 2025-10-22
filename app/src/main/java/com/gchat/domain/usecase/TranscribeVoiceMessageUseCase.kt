package com.gchat.domain.usecase

import com.gchat.domain.repository.AudioRepository
import com.gchat.domain.repository.MessageRepository
import com.gchat.domain.repository.TranscriptionResult
import javax.inject.Inject

/**
 * Use case for transcribing voice messages
 */
class TranscribeVoiceMessageUseCase @Inject constructor(
    private val audioRepository: AudioRepository,
    private val messageRepository: MessageRepository
) {
    /**
     * Transcribe a voice message
     * 
     * @param messageId Message ID
     * @param audioUrl Audio file URL
     * @param conversationId Conversation ID
     * @return Transcription result
     */
    suspend operator fun invoke(
        messageId: String,
        audioUrl: String,
        conversationId: String
    ): Result<TranscriptionResult> {
        return try {
            // 1. Check cache first
            val cached = audioRepository.getCachedTranscription(messageId)
            if (cached != null) {
                return Result.success(cached)
            }
            
            // 2. Request transcription from backend
            val result = audioRepository.transcribeAudio(audioUrl, messageId)
            
            if (result.isSuccess) {
                val transcription = result.getOrThrow()
                
                // 3. Update message with transcription
                messageRepository.updateMessageTranscription(
                    conversationId = conversationId,
                    messageId = messageId,
                    transcription = transcription.text
                )
                
                Result.success(transcription)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Transcription failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

