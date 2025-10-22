package com.gchat.domain.usecase

import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.gchat.domain.model.MessageType
import com.gchat.domain.repository.AudioRepository
import com.gchat.domain.repository.AudioRecordingResult
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Use case for sending voice messages
 */
class SendVoiceMessageUseCase @Inject constructor(
    private val audioRepository: AudioRepository,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    /**
     * Send a voice message
     * 
     * @param conversationId Conversation ID
     * @param senderId Sender user ID
     * @param recordingResult Recording result with file and metadata
     * @return Result with sent message
     */
    suspend operator fun invoke(
        conversationId: String,
        senderId: String,
        recordingResult: AudioRecordingResult
    ): Result<Message> {
        return try {
            // 1. Upload audio file to Firebase Storage
            val uploadResult = audioRepository.uploadAudio(recordingResult.file, senderId)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }
            
            val audioUrl = uploadResult.getOrThrow()
            
            // 2. Create message
            val message = Message(
                id = generateMessageId(),
                conversationId = conversationId,
                senderId = senderId,
                type = MessageType.AUDIO,
                text = null, // Audio messages don't have text initially
                mediaUrl = audioUrl,
                audioDuration = recordingResult.durationSeconds,
                audioWaveform = recordingResult.waveformData,
                transcription = null, // Transcription will be added later
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING
            )
            
            // 3. Save message locally (optimistic UI)
            messageRepository.insertMessage(message)
            
            // 4. Sync to Firestore
            messageRepository.syncMessageToRemote(message)
            
            // 5. Update conversation last message
            conversationRepository.updateLastMessage(
                conversationId = conversationId,
                messageId = message.id,
                messageText = "🎤 Voice message",
                messageType = MessageType.AUDIO.name,
                mediaUrl = audioUrl,
                senderId = senderId,
                timestamp = message.timestamp
            )
            
            // 6. Request transcription in background (don't wait for it)
            // This will be handled by a separate use case
            
            // 7. Clean up temporary audio file
            try {
                recordingResult.file.delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

