package com.gchat.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.Message
import com.gchat.domain.repository.AuthRepository
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    private val _sendingState = MutableStateFlow(false)
    val sendingState: StateFlow<Boolean> = _sendingState.asStateFlow()
    
    val messages: StateFlow<List<Message>> = getMessagesUseCase(conversationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), // Stop immediately when UI unsubscribes
            initialValue = emptyList()
        )
    
    val currentUserId: StateFlow<String?> = flow {
        emit(authRepository.getCurrentUserId())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
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
}

