package com.gchat.domain.usecase

import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.gchat.domain.model.MessageType
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.MessageRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for sending a message
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        senderId: String,
        text: String,
        type: MessageType = MessageType.TEXT
    ): Result<Message> {
        return try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = senderId,
                type = type,
                text = text,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING
            )
            
            // Send message
            val result = messageRepository.sendMessage(message)
            
            result.fold(
                onSuccess = {
                    android.util.Log.d("SendMessageUseCase", "Message sent successfully, updating lastMessage: '$text'")
                    // Update conversation's last message
                    val updateResult = conversationRepository.updateLastMessage(
                        conversationId = conversationId,
                        messageId = message.id,
                        messageText = text,
                        senderId = senderId,
                        timestamp = message.timestamp
                    )
                    updateResult.onFailure { e ->
                        android.util.Log.e("SendMessageUseCase", "Failed to update lastMessage", e)
                    }
                    
                    Result.success(message)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

