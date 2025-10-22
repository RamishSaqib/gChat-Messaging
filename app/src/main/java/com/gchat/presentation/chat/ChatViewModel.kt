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
import com.gchat.domain.repository.TypingRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.domain.usecase.GetMessagesUseCase
import com.gchat.domain.usecase.MarkMessageAsReadUseCase
import com.gchat.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val typingRepository: TypingRepository,
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
    
    val currentUserId: StateFlow<String?> = flow {
        emit(authRepository.getCurrentUserId())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Get conversation details
    val conversation: StateFlow<com.gchat.domain.model.Conversation?> = flow {
        conversationRepository.getConversation(conversationId)
            .onSuccess { emit(it) }
            .onFailure { emit(null) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Filter messages to only show those after user's deletion timestamp (for fresh history)
    val messages: StateFlow<List<Message>> = combine(
        getMessagesUseCase(conversationId),
        conversation,
        currentUserId
    ) { allMessages, conv, userId ->
        if (conv == null || userId == null) {
            allMessages
        } else {
            // Get user's deletion timestamp (0 if never deleted)
            val userDeletedAt = conv.deletedAt[userId] ?: 0L
            // Only show messages sent after the deletion timestamp
            allMessages.filter { message -> message.timestamp > userDeletedAt }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )
    
    // Get participants for all chats (userId -> User)
    // Used for displaying sender names in group chats and typing indicators in all chats
    val participantUsers: StateFlow<Map<String, com.gchat.domain.model.User>> = flow {
        val conv = conversationRepository.getConversation(conversationId).getOrNull()
        if (conv != null) {
            val users = mutableMapOf<String, com.gchat.domain.model.User>()
            conv.participants.forEach { userId ->
                userRepository.getUser(userId).onSuccess { user ->
                    users[userId] = user
                }
            }
            emit(users)
        } else {
            emit(emptyMap())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    
    // Get display name for the TopBar (with nickname support)
    val otherUserName: StateFlow<String> = combine(
        conversation,
        currentUserId,
        participantUsers
    ) { conv, userId, participants ->
        if (conv != null && userId != null) {
            val otherUserId = conv.getOtherParticipantId(userId)
            if (otherUserId != null) {
                // DM chat - use nickname if set, otherwise real name
                val otherUser = participants[otherUserId]
                conv.getUserDisplayName(otherUserId, otherUser)
            } else {
                // Group chat - use conversation name
                conv.name ?: "Group Chat"
            }
        } else {
            "Chat"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Chat")
    
    // Typing indicators
    private var typingDebounceJob: Job? = null
    private val TYPING_TIMEOUT_MS = 3000L // Stop typing after 3 seconds of inactivity
    
    /**
     * Observe who is typing in this conversation
     * Returns a formatted string like "John is typing..." or "John, Sarah are typing..."
     */
    val typingIndicatorText: StateFlow<String> = combine(
        typingRepository.observeTypingIndicators(conversationId),
        participantUsers,
        currentUserId,
        conversation
    ) { typingIndicators, participants, userId, conv ->
        // Filter out current user (don't show own typing indicator)
        val otherTypers = typingIndicators.filter { it.userId != userId }
        
        if (otherTypers.isEmpty()) {
            ""
        } else {
            // Get user names for typers (using nicknames if set)
            val typerNames = otherTypers.mapNotNull { indicator ->
                conv?.getUserDisplayName(indicator.userId, participants[indicator.userId])
            }
            
            when (typerNames.size) {
                0 -> ""
                1 -> "${typerNames[0]} is typing..."
                2 -> "${typerNames[0]} and ${typerNames[1]} are typing..."
                else -> "${typerNames[0]}, ${typerNames[1]}, and ${typerNames.size - 2} others are typing..."
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    fun updateMessageText(text: String) {
        _messageText.value = text
        
        // Update typing status
        val currentUserId = currentUserId.value ?: return
        val isTyping = text.isNotBlank()
        
        // Cancel previous debounce job
        typingDebounceJob?.cancel()
        
        viewModelScope.launch {
            // Set typing status
            typingRepository.setTypingStatus(conversationId, currentUserId, isTyping)
            
            // If typing, start a debounce timer to clear typing status after inactivity
            if (isTyping) {
                typingDebounceJob = launch {
                    delay(TYPING_TIMEOUT_MS)
                    // Clear typing status after timeout
                    typingRepository.setTypingStatus(conversationId, currentUserId, false)
                }
            }
        }
    }
    
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank()) return
        
        val userId = currentUserId.value ?: return
        
        viewModelScope.launch {
            _sendingState.value = true
            
            // Clear typing status when sending message
            typingDebounceJob?.cancel()
            typingRepository.setTypingStatus(conversationId, userId, false)
            
            sendMessageUseCase(
                conversationId = conversationId,
                senderId = userId,
                text = text
            )
            
            _messageText.value = ""
            _sendingState.value = false
        }
    }
    
    /**
     * Mark a message as read by the current user
     */
    fun markMessageAsRead(messageId: String) {
        val userId = currentUserId.value ?: return
        
        viewModelScope.launch {
            markMessageAsReadUseCase(
                messageId = messageId,
                conversationId = conversationId,
                userId = userId
            )
        }
    }
    
    /**
     * Mark all unread messages in the conversation as read
     */
    fun markAllMessagesAsRead() {
        val userId = currentUserId.value ?: return
        val unreadMessages = messages.value.filter { message ->
            message.senderId != userId && !message.isReadBy(userId)
        }
        
        viewModelScope.launch {
            unreadMessages.forEach { message ->
                markMessageAsReadUseCase(
                    messageId = message.id,
                    conversationId = conversationId,
                    userId = userId
                )
            }
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
                    
                    // Clear the message text after sending
                    _messageText.value = ""
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

