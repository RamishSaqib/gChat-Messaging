package com.gchat.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageType
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.MediaRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.domain.usecase.GetMessagesUseCase
import com.gchat.domain.usecase.MarkMessageAsReadUseCase
import com.gchat.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for chat screen
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    private val _sendingState = MutableStateFlow(false)
    val sendingState: StateFlow<Boolean> = _sendingState.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()
    
    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()
    
    val messages: StateFlow<List<Message>> = getMessagesUseCase(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), // Keep alive for 5s during recompositions
            initialValue = emptyList()
        )
    
    val currentUserId: StateFlow<String?> = flow {
        emit(authRepository.getCurrentUserId())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Get other user's display name for the TopBar
    val otherUserName: StateFlow<String> = flow {
        val currentUserId = authRepository.getCurrentUserId() ?: ""
        val conversation = conversationRepository.getConversation(conversationId).getOrNull()
        
        if (conversation != null) {
            val otherUserId = conversation.getOtherParticipantId(currentUserId)
            if (otherUserId != null) {
                val otherUser = userRepository.getUser(otherUserId).getOrNull()
                emit(otherUser?.displayName ?: "Chat")
            } else {
                // Group chat - use conversation name
                emit(conversation.name ?: "Group Chat")
            }
        } else {
            emit("Chat")
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Chat")
    
    fun updateMessageText(text: String) {
        _messageText.value = text
    }
    
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank()) return
        
        val userId = currentUserId.value ?: return
        
        viewModelScope.launch {
            _sendingState.value = true
            
            sendMessageUseCase(
                conversationId = conversationId,
                senderId = userId,
                text = text
            )
            
            _messageText.value = ""
            _sendingState.value = false
        }
    }
    
    fun markMessageAsRead(messageId: String) {
        val userId = currentUserId.value ?: return
        
        viewModelScope.launch {
            markMessageAsReadUseCase(conversationId, messageId, userId)
        }
    }
    
    fun sendImageMessage(imageUri: Uri, caption: String? = null) {
        val userId = currentUserId.value ?: return
        
        viewModelScope.launch {
            _sendingState.value = true
            _uploadProgress.value = 0f
            _uploadError.value = null
            
            // Upload image to Firebase Storage
            val storagePath = "chat_images/$userId/${System.currentTimeMillis()}.jpg"
            mediaRepository.uploadImage(imageUri, storagePath)
                .onSuccess { imageUrl ->
                    _uploadProgress.value = 0.8f
                    
                    // Send message with image URL
                    sendMessageUseCase(
                        conversationId = conversationId,
                        senderId = userId,
                        text = caption ?: "",
                        type = MessageType.IMAGE,
                        mediaUrl = imageUrl
                    )
                    
                    _uploadProgress.value = null
                    _sendingState.value = false
                }
                .onFailure { error ->
                    _uploadError.value = "Failed to upload image: ${error.message}"
                    _uploadProgress.value = null
                    _sendingState.value = false
                }
        }
    }
    
    fun clearUploadError() {
        _uploadError.value = null
    }
}

