package com.gchat.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.ExtractedData
import com.gchat.domain.model.ExtractedEntity
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageType
import com.gchat.domain.model.Translation
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.AudioRepository
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.MediaRepository
import com.gchat.domain.repository.PlaybackState
import com.gchat.domain.repository.RecordingState
import com.gchat.domain.repository.TranslationRepository
import com.gchat.domain.repository.TypingRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.domain.usecase.ExtractBatchDataUseCase
import com.gchat.domain.usecase.ExtractDataFromMessageUseCase
import com.gchat.domain.usecase.GetMessagesUseCase
import com.gchat.domain.usecase.MarkMessageAsReadUseCase
import com.gchat.domain.usecase.PlayVoiceMessageUseCase
import com.gchat.domain.usecase.RecordVoiceMessageUseCase
import com.gchat.domain.usecase.SendMessageUseCase
import com.gchat.domain.usecase.SendVoiceMessageUseCase
import com.gchat.domain.usecase.TranscribeVoiceMessageUseCase
import com.gchat.domain.usecase.TranslateMessageUseCase
import com.gchat.domain.usecase.GenerateSmartRepliesUseCase
import com.gchat.domain.usecase.AdjustFormalityUseCase
import com.gchat.domain.model.SmartReply
import com.gchat.domain.model.FormalityLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * ViewModel for chat screen
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val translateMessageUseCase: TranslateMessageUseCase,
    private val extractDataFromMessageUseCase: ExtractDataFromMessageUseCase,
    private val extractBatchDataUseCase: ExtractBatchDataUseCase,
    private val recordVoiceMessageUseCase: RecordVoiceMessageUseCase,
    private val sendVoiceMessageUseCase: SendVoiceMessageUseCase,
    private val playVoiceMessageUseCase: PlayVoiceMessageUseCase,
    private val transcribeVoiceMessageUseCase: TranscribeVoiceMessageUseCase,
    private val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    private val getCulturalContextUseCase: com.gchat.domain.usecase.GetCulturalContextUseCase,
    private val adjustFormalityUseCase: AdjustFormalityUseCase,
    private val authRepository: AuthRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository,
    private val audioRepository: AudioRepository,
    private val typingRepository: TypingRepository,
    private val translationRepository: TranslationRepository,
    private val autoTranslateRepository: com.gchat.domain.repository.AutoTranslateRepository,
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
    
    // Get conversation details (with real-time updates)
    val conversation: StateFlow<com.gchat.domain.model.Conversation?> = 
        conversationRepository.getConversationFlow(conversationId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
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
            // Emit empty map first so UI doesn't wait
            emit(emptyMap())
            
            // Then observe real-time updates from flows
            combine(conv.participants.map { userId ->
                userRepository.getUserFlow(userId).filterNotNull()
            }) { users ->
                users.mapIndexed { index, user -> 
                    conv.participants[index] to user 
                }.toMap()
            }.collect { emit(it) }
        } else {
            emit(emptyMap())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // Start loading immediately, not on first collection
        initialValue = emptyMap()
    )
    
    // Preload display name synchronously in init block for instant display
    private val preloadedDisplayName: String by lazy {
        val startTime = System.currentTimeMillis()
        
        runBlocking {
            val conv = conversationRepository.getConversation(conversationId).getOrNull()
            val userId = authRepository.getCurrentUserId()
            
            if (conv != null && userId != null) {
                val otherUserId = conv.getOtherParticipantId(userId)
                
                if (otherUserId != null) {
                    // DM chat - get name from cache
                    val cachedUser = userRepository.getUser(otherUserId).getOrNull()
                    val displayName = if (cachedUser != null) {
                        conv.getUserDisplayName(otherUserId, cachedUser)
                    } else {
                        "Chat"
                    }
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    android.util.Log.d("ChatViewModel", "⚡ Preloaded display name in ${elapsed}ms: $displayName")
                    displayName
                } else {
                    // Group chat
                    val groupName = conv.name ?: "Group Chat"
                    val elapsed = System.currentTimeMillis() - startTime
                    android.util.Log.d("ChatViewModel", "⚡ Preloaded group name in ${elapsed}ms: $groupName")
                    groupName
                }
            } else {
                android.util.Log.d("ChatViewModel", "⚠️ Failed to preload display name")
                "Chat"
            }
        }
    }
    
    // Get display name for the TopBar (with nickname support)
    // Use preloaded value as initial value, then observe real-time updates
    val otherUserName: StateFlow<String> = flow {
        android.util.Log.d("ChatViewModel", "otherUserName flow starting for conversationId: $conversationId")
        
        // STEP 1: Emit preloaded value immediately (should already be computed)
        emit(preloadedDisplayName)
        
        // STEP 2: Get conversation and subscribe to real-time updates
        val conv = conversationRepository.getConversation(conversationId).getOrNull()
        val userId = authRepository.getCurrentUserId()
        
        if (conv != null && userId != null) {
            val otherUserId = conv.getOtherParticipantId(userId)
            
            if (otherUserId != null) {
                // DM chat - observe real-time updates
                android.util.Log.d("ChatViewModel", "Subscribing to real-time user updates for: $otherUserId")
                userRepository.getUserFlow(otherUserId).filterNotNull().collect { user ->
                    val displayName = conv.getUserDisplayName(otherUserId, user)
                    android.util.Log.d("ChatViewModel", "📡 Real-time update: $displayName")
                    emit(displayName)
                }
            }
            // Group chat name doesn't change, so no need to observe
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, preloadedDisplayName)
    
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
    
    // ===== Translation State =====
    
    private val _translations = MutableStateFlow<Map<String, Translation>>(emptyMap())
    val translations: StateFlow<Map<String, Translation>> = _translations.asStateFlow()
    
    private val _translationLoading = MutableStateFlow<Set<String>>(emptySet())
    val translationLoading: StateFlow<Set<String>> = _translationLoading.asStateFlow()
    
    private val _translationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val translationErrors: StateFlow<Map<String, String>> = _translationErrors.asStateFlow()
    
    /**
     * Translate a message to the target language
     */
    fun translateMessage(message: Message, targetLanguage: String) {
        viewModelScope.launch {
            val messageId = message.id
            
            // Add to loading set
            _translationLoading.value = _translationLoading.value + messageId
            
            // Clear any previous error
            _translationErrors.value = _translationErrors.value - messageId
            
            android.util.Log.d("ChatViewModel", "Translating message $messageId to $targetLanguage")
            
            // Call translation use case
            translateMessageUseCase(
                messageId = messageId,
                text = message.text ?: "",
                targetLanguage = targetLanguage,
                sourceLanguage = null // Auto-detect
            ).fold(
                onSuccess = { translation ->
                    android.util.Log.d("ChatViewModel", "Translation success: ${translation.translatedText}")
                    // Add to translations map
                    _translations.value = _translations.value + (messageId to translation)
                    // Remove from loading
                    _translationLoading.value = _translationLoading.value - messageId
                },
                onFailure = { error ->
                    android.util.Log.e("ChatViewModel", "Translation failed", error)
                    // Add error
                    _translationErrors.value = _translationErrors.value + (messageId to (error.message ?: "Translation failed"))
                    // Remove from loading
                    _translationLoading.value = _translationLoading.value - messageId
                }
            )
        }
    }
    
    /**
     * Remove translation for a message (hide translation)
     */
    fun removeTranslation(messageId: String) {
        _translations.value = _translations.value - messageId
        _translationErrors.value = _translationErrors.value - messageId
    }
    
    /**
     * Retry failed translation
     */
    fun retryTranslation(message: Message, targetLanguage: String) {
        translateMessage(message, targetLanguage)
    }
    
    /**
     * Get translation for a specific message
     */
    fun getTranslation(messageId: String): Translation? {
        return _translations.value[messageId]
    }
    
    /**
     * Check if message is being translated
     */
    fun isTranslating(messageId: String): Boolean {
        return messageId in _translationLoading.value
    }
    
    /**
     * Get translation error for a message
     */
    fun getTranslationError(messageId: String): String? {
        return _translationErrors.value[messageId]
    }
    
    // ===== Auto-Translation State =====
    
    /**
     * Determine if auto-translate should be enabled for this conversation
     * Combines global user setting with per-conversation override
     */
    val shouldAutoTranslate: StateFlow<Boolean> = combine(
        currentUserId,
        conversation
    ) { userId, conv ->
        if (userId == null || conv == null) {
            false
        } else {
            autoTranslateRepository.shouldAutoTranslate(conversationId, userId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    /**
     * Auto-translate incoming messages
     * This observes new messages and automatically translates them if auto-translate is enabled
     */
    init {
        viewModelScope.launch {
            combine(
                messages,
                shouldAutoTranslate,
                currentUserId
            ) { messagesList, autoTranslate, userId ->
                Triple(messagesList, autoTranslate, userId)
            }.collect { (messagesList, autoTranslate, userId) ->
                if (!autoTranslate || userId == null) return@collect
                
                // Get user's preferred language
                val user = userRepository.getUser(userId).getOrNull()
                val targetLanguage = user?.preferredLanguage ?: "en"
                
                // Auto-translate new messages that:
                // 1. Are not from current user
                // 2. Are text messages
                // 3. Don't already have a translation
                messagesList
                    .filter { message ->
                        message.senderId != userId &&
                        message.type == MessageType.TEXT &&
                        !message.text.isNullOrBlank() &&
                        !_translations.value.containsKey(message.id)
                    }
                    .forEach { message ->
                        // Detect language first to avoid translating messages already in target language
                        launch {
                            // Get sender's preferred language as a hint for disambiguation
                            val senderLanguageHint = userRepository.getUser(message.senderId)
                                .getOrNull()
                                ?.preferredLanguage
                            
                            android.util.Log.d("ChatViewModel", "Detecting language for message ${message.id} (sender hint: $senderLanguageHint)")
                            
                            val detectedLanguage = translationRepository.detectLanguage(
                                text = message.text ?: "",
                                senderLanguageHint = senderLanguageHint
                            ).getOrNull()
                            
                            // Only translate if the detected language is different from target language
                            if (detectedLanguage != null && detectedLanguage != targetLanguage) {
                                android.util.Log.d("ChatViewModel", "Language detected: $detectedLanguage (target: $targetLanguage) - translating")
                                translateMessage(message, targetLanguage)
                            } else if (detectedLanguage == null) {
                                // If detection fails, still attempt translation (it will auto-detect)
                                android.util.Log.d("ChatViewModel", "Language detection failed - attempting translation anyway")
                                translateMessage(message, targetLanguage)
                            } else {
                                android.util.Log.d("ChatViewModel", "Language matches target ($targetLanguage) - skipping translation")
                            }
                            // If detectedLanguage == targetLanguage, skip translation
                        }
                    }
            }
        }
    }
    
    // ===== Smart Reply State =====
    
    private val _smartReplies = MutableStateFlow<List<SmartReply>>(emptyList())
    val smartReplies: StateFlow<List<SmartReply>> = _smartReplies.asStateFlow()
    
    private val _smartRepliesLoading = MutableStateFlow(false)
    val smartRepliesLoading: StateFlow<Boolean> = _smartRepliesLoading.asStateFlow()
    
    private val _smartRepliesError = MutableStateFlow<String?>(null)
    val smartRepliesError: StateFlow<String?> = _smartRepliesError.asStateFlow()
    
    // Track the last message we generated replies for (to avoid regenerating)
    private val _lastSmartReplyMessageId = MutableStateFlow<String?>(null)
    
    // Debounce job for smart reply generation
    private var smartReplyJob: Job? = null
    
    /**
     * Auto-generate smart replies for incoming messages
     * Debounced by 500ms to avoid excessive API calls
     * Only generates if smart replies are enabled (global or per-conversation)
     */
    init {
        viewModelScope.launch {
            combine(
                messages,
                currentUserId,
                conversation
            ) { messagesList, userId, conv ->
                Triple(messagesList, userId, conv)
            }.collect { (messagesList, userId, conv) ->
                if (userId == null || conv == null) return@collect
                
                // Check if smart replies are enabled
                val user = userRepository.getUser(userId).getOrNull()
                val smartRepliesEnabled = conv.smartRepliesEnabled ?: (user?.smartRepliesEnabled ?: true)
                
                if (!smartRepliesEnabled) {
                    // Clear any existing smart replies if feature is disabled
                    if (_smartReplies.value.isNotEmpty()) {
                        _smartReplies.value = emptyList()
                        _lastSmartReplyMessageId.value = null
                    }
                    return@collect
                }
                
                // Get the latest message overall
                val latestMessage = messagesList.maxByOrNull { it.timestamp }
                
                // Get the latest message from the other user
                val latestIncomingMessage = messagesList
                    .filter { it.senderId != userId && it.type == MessageType.TEXT && !it.text.isNullOrBlank() }
                    .maxByOrNull { it.timestamp }
                
                // Only generate if:
                // 1. We have a new incoming message
                // 2. The incoming message ID hasn't been processed yet
                // 3. The latest message in the conversation is from the other user (not from us)
                if (latestIncomingMessage != null && 
                    latestIncomingMessage.id != _lastSmartReplyMessageId.value &&
                    latestMessage?.id == latestIncomingMessage.id) {
                    
                    // Debounce: cancel previous job and start new one after delay
                    smartReplyJob?.cancel()
                    smartReplyJob = launch {
                        delay(500) // 500ms debounce
                        loadSmartReplies(latestIncomingMessage)
                    }
                }
            }
        }
    }
    
    /**
     * Load smart reply suggestions for a message
     */
    fun loadSmartReplies(incomingMessage: Message) {
        viewModelScope.launch {
            _smartRepliesLoading.value = true
            _smartRepliesError.value = null
            
            android.util.Log.d("ChatViewModel", "Loading smart replies for message: ${incomingMessage.id}")
            
            // Get target language from user preferences
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val user = userRepository.getUser(userId).getOrNull()
            val targetLanguage = user?.preferredLanguage ?: "en"
            
            val result = generateSmartRepliesUseCase(
                conversationId = conversationId,
                incomingMessageId = incomingMessage.id,
                targetLanguage = targetLanguage
            )
            
            result.onSuccess { replies ->
                _smartReplies.value = replies
                _lastSmartReplyMessageId.value = incomingMessage.id
                _smartRepliesError.value = null
                android.util.Log.d("ChatViewModel", "Smart replies loaded: ${replies.size}")
            }.onFailure { error ->
                _smartReplies.value = emptyList()
                _smartRepliesError.value = error.message ?: "Failed to generate smart replies"
                android.util.Log.e("ChatViewModel", "Smart reply error", error)
            }
            
            _smartRepliesLoading.value = false
        }
    }
    
    /**
     * Use a smart reply suggestion
     * Inserts the reply text into the message input field
     */
    fun useSmartReply(reply: SmartReply) {
        _messageText.value = reply.replyText
        android.util.Log.d("ChatViewModel", "Smart reply selected: ${reply.replyText}")
    }
    
    /**
     * Dismiss smart reply suggestions
     */
    fun dismissSmartReplies() {
        _smartReplies.value = emptyList()
        _lastSmartReplyMessageId.value = null
        android.util.Log.d("ChatViewModel", "Smart replies dismissed")
    }
    
    // ===== Data Extraction State =====
    
    private val _extractedData = MutableStateFlow<Map<String, ExtractedData>>(emptyMap())
    val extractedData: StateFlow<Map<String, ExtractedData>> = _extractedData.asStateFlow()
    
    private val _extractionLoading = MutableStateFlow<Set<String>>(emptySet())
    val extractionLoading: StateFlow<Set<String>> = _extractionLoading.asStateFlow()
    
    private val _extractionErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val extractionErrors: StateFlow<Map<String, String>> = _extractionErrors.asStateFlow()
    
    private val _batchExtractionLoading = MutableStateFlow(false)
    val batchExtractionLoading: StateFlow<Boolean> = _batchExtractionLoading.asStateFlow()
    
    /**
     * Extract intelligent data from a single message
     */
    fun extractFromMessage(message: Message) {
        if (message.text.isNullOrBlank()) {
            android.util.Log.d("ChatViewModel", "Cannot extract from empty message")
            return
        }
        
        viewModelScope.launch {
            val messageId = message.id
            
            // Add to loading set
            _extractionLoading.value = _extractionLoading.value + messageId
            
            // Clear any previous error
            _extractionErrors.value = _extractionErrors.value - messageId
            
            android.util.Log.d("ChatViewModel", "Extracting data from message $messageId")
            
            // Call extraction use case
            val result = extractDataFromMessageUseCase(
                messageId = messageId,
                text = message.text,
                conversationId = conversationId
            )
            
            result.onSuccess { extracted ->
                android.util.Log.d("ChatViewModel", "Extraction success: ${extracted.entities.size} entities")
                // Only store if entities were found
                if (extracted.hasEntities()) {
                    _extractedData.value = _extractedData.value + (messageId to extracted)
                }
                // Remove from loading
                _extractionLoading.value = _extractionLoading.value - messageId
            }.onFailure { error ->
                android.util.Log.e("ChatViewModel", "Extraction failed: ${error.message}")
                // Add error
                _extractionErrors.value = _extractionErrors.value + (messageId to (error.message ?: "Extraction failed"))
                // Remove from loading
                _extractionLoading.value = _extractionLoading.value - messageId
            }
        }
    }
    
    /**
     * Extract data from multiple messages (batch)
     */
    fun extractFromBatch(messages: List<Message>) {
        if (messages.isEmpty()) return
        
        viewModelScope.launch {
            _batchExtractionLoading.value = true
            
            android.util.Log.d("ChatViewModel", "Batch extracting from ${messages.size} messages")
            
            // Filter to only text messages and prepare pairs
            val messagePairs = messages
                .filter { !it.text.isNullOrBlank() }
                .map { it.id to it.text!! }
            
            if (messagePairs.isEmpty()) {
                _batchExtractionLoading.value = false
                return@launch
            }
            
            // Call batch extraction use case
            val result = extractBatchDataUseCase(
                messages = messagePairs,
                conversationId = conversationId
            )
            
            result.onSuccess { batchResult ->
                android.util.Log.d("ChatViewModel", "Batch extraction success: ${batchResult.totalEntities} total entities")
                
                // Add all extracted data to the map
                val newData = batchResult.results
                    .filter { it.hasEntities() }
                    .associateBy { it.messageId }
                
                _extractedData.value = _extractedData.value + newData
                
                _batchExtractionLoading.value = false
            }.onFailure { error ->
                android.util.Log.e("ChatViewModel", "Batch extraction failed: ${error.message}")
                _batchExtractionLoading.value = false
            }
        }
    }
    
    /**
     * Get extracted data for a specific message
     */
    fun getExtractedData(messageId: String): ExtractedData? {
        return _extractedData.value[messageId]
    }
    
    /**
     * Check if message has extracted data
     */
    fun hasExtractedData(messageId: String): Boolean {
        return _extractedData.value.containsKey(messageId)
    }
    
    /**
     * Check if message is being extracted
     */
    fun isExtracting(messageId: String): Boolean {
        return messageId in _extractionLoading.value
    }
    
    /**
     * Get extraction error for a message
     */
    fun getExtractionError(messageId: String): String? {
        return _extractionErrors.value[messageId]
    }
    
    /**
     * Get all extracted entities from all messages
     */
    fun getAllExtractedEntities(): List<ExtractedEntity> {
        return _extractedData.value.values.flatMap { it.entities }
    }
    
    /**
     * Get all entities by type
     */
    fun getEntitiesByType(type: com.gchat.domain.model.EntityType): List<ExtractedEntity> {
        return getAllExtractedEntities().filter { it.type == type }
    }
    
    // ===== Cultural Context State =====
    
    private val _culturalContexts = MutableStateFlow<Map<String, com.gchat.domain.model.CulturalContextResult>>(emptyMap())
    val culturalContexts: StateFlow<Map<String, com.gchat.domain.model.CulturalContextResult>> = _culturalContexts.asStateFlow()
    
    private val _culturalContextLoading = MutableStateFlow<Set<String>>(emptySet())
    val culturalContextLoading: StateFlow<Set<String>> = _culturalContextLoading.asStateFlow()
    
    private val _culturalContextErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val culturalContextErrors: StateFlow<Map<String, String>> = _culturalContextErrors.asStateFlow()
    
    /**
     * Load cultural context (idioms, slang, cultural references) for a message.
     * Typically triggered when user taps the cultural context icon or when translation is shown.
     */
    fun loadCulturalContext(message: Message, mode: String = "all") {
        if (message.text.isNullOrBlank()) {
            android.util.Log.d("ChatViewModel", "Cannot load cultural context for empty message")
            return
        }
        
        viewModelScope.launch {
            val messageId = message.id
            
            // Add to loading set
            _culturalContextLoading.value = _culturalContextLoading.value + messageId
            
            // Clear any previous error
            _culturalContextErrors.value = _culturalContextErrors.value - messageId
            
            android.util.Log.d("ChatViewModel", "Loading cultural context for message $messageId (mode: $mode)")
            
            // Detect language (use detected language from translation if available, otherwise detect with sender hint)
            val language = translations.value[messageId]?.sourceLanguage 
                ?: run {
                    // Get sender's preferred language as a hint for disambiguation
                    val senderLanguageHint = userRepository.getUser(message.senderId)
                        .getOrNull()
                        ?.preferredLanguage
                    
                    android.util.Log.d("ChatViewModel", "Detecting language for cultural context (sender hint: $senderLanguageHint)")
                    
                    translationRepository.detectLanguage(
                        text = message.text,
                        senderLanguageHint = senderLanguageHint
                    ).getOrNull() ?: "en"
                }
            
            android.util.Log.d("ChatViewModel", "Detected language for cultural context: $language")
            
            // Call use case
            val result = getCulturalContextUseCase(
                messageId = messageId,
                text = message.text,
                language = language,
                mode = mode
            )
            
            result.onSuccess { contextResult ->
                android.util.Log.d("ChatViewModel", "Cultural context loaded: ${contextResult.contexts.size} items found")
                // Add to contexts map
                _culturalContexts.value = _culturalContexts.value + (messageId to contextResult)
                // Remove from loading
                _culturalContextLoading.value = _culturalContextLoading.value - messageId
            }.onFailure { error ->
                android.util.Log.e("ChatViewModel", "Cultural context failed", error)
                // Add error
                _culturalContextErrors.value = _culturalContextErrors.value + (messageId to (error.message ?: "Failed to load cultural context"))
                // Remove from loading
                _culturalContextLoading.value = _culturalContextLoading.value - messageId
            }
        }
    }
    
    /**
     * Explain slang/idioms in a message (shortcut for cultural context with slang mode)
     */
    fun explainSlang(message: Message) {
        loadCulturalContext(message, mode = "slang")
    }
    
    /**
     * Get cultural context for a specific message
     */
    fun getCulturalContext(messageId: String): com.gchat.domain.model.CulturalContextResult? {
        return _culturalContexts.value[messageId]
    }
    
    /**
     * Check if message has cultural context loaded
     */
    fun hasCulturalContext(messageId: String): Boolean {
        return _culturalContexts.value.containsKey(messageId)
    }
    
    /**
     * Check if cultural context is being loaded for a message
     */
    fun isCulturalContextLoading(messageId: String): Boolean {
        return messageId in _culturalContextLoading.value
    }
    
    /**
     * Get cultural context error for a message
     */
    fun getCulturalContextError(messageId: String): String? {
        return _culturalContextErrors.value[messageId]
    }
    
    // ============ Formality Adjustment State ============
    
    private val _selectedFormality = MutableStateFlow(FormalityLevel.NEUTRAL)
    val selectedFormality: StateFlow<FormalityLevel> = _selectedFormality.asStateFlow()
    
    private val _formalityLoading = MutableStateFlow(false)
    val formalityLoading: StateFlow<Boolean> = _formalityLoading.asStateFlow()
    
    private val _formalityError = MutableStateFlow<String?>(null)
    val formalityError: StateFlow<String?> = _formalityError.asStateFlow()
    
    /**
     * Set the selected formality level.
     * The message text will be adjusted when user manually triggers adjustment.
     */
    fun setSelectedFormality(formality: FormalityLevel) {
        android.util.Log.d("ChatViewModel", "Formality level selected: $formality")
        _selectedFormality.value = formality
    }
    
    /**
     * Adjust the current message text to the selected formality level.
     * Only works if text is longer than 10 characters.
     */
    fun adjustCurrentMessageFormality() {
        val text = _messageText.value
        if (text.length <= 10) {
            android.util.Log.d("ChatViewModel", "Text too short for formality adjustment (${text.length} chars)")
            return
        }
        
        val formality = _selectedFormality.value
        android.util.Log.d("ChatViewModel", "Adjusting message formality to $formality")
        
        viewModelScope.launch {
            _formalityLoading.value = true
            _formalityError.value = null
            
            // Get user's preferred language
            val userId = authRepository.getCurrentUserId() ?: run {
                _formalityError.value = "User not authenticated"
                _formalityLoading.value = false
                return@launch
            }
            
            val user = userRepository.getUser(userId).getOrNull()
            val language = user?.preferredLanguage ?: "en"
            
            val result = adjustFormalityUseCase(
                text = text,
                language = language,
                targetFormality = formality
            )
            
            result.onSuccess { adjustedText ->
                _messageText.value = adjustedText
                _formalityError.value = null
                android.util.Log.d("ChatViewModel", "Formality adjustment successful: ${text.length} -> ${adjustedText.length} chars")
            }.onFailure { error ->
                _formalityError.value = error.message ?: "Failed to adjust formality"
                android.util.Log.e("ChatViewModel", "Formality adjustment failed", error)
            }
            
            _formalityLoading.value = false
        }
    }
    
    // ============ Voice Message State ============
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _playbackStates = MutableStateFlow<Map<String, PlaybackState>>(emptyMap())
    val playbackStates: StateFlow<Map<String, PlaybackState>> = _playbackStates.asStateFlow()
    
    private val _currentlyPlayingMessageId = MutableStateFlow<String?>(null)
    val currentlyPlayingMessageId: StateFlow<String?> = _currentlyPlayingMessageId.asStateFlow()
    
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    
    private val _transcriptionLoading = MutableStateFlow<Set<String>>(emptySet())
    val transcriptionLoading: StateFlow<Set<String>> = _transcriptionLoading.asStateFlow()
    
    // ============ Voice Message Methods ============
    
    /**
     * Start recording voice message
     */
    fun startVoiceRecording() {
        viewModelScope.launch {
            recordVoiceMessageUseCase().collect { state ->
                _recordingState.value = state
            }
        }
    }
    
    /**
     * Stop recording and send voice message
     */
    fun stopAndSendVoiceMessage() {
        val userId = currentUserId.value ?: return
        
        viewModelScope.launch {
            // Stop recording
            val result = audioRepository.stopRecording()
            
            result.onSuccess { recordingResult ->
                _recordingState.value = RecordingState.Idle
                
                // Send voice message
                val sendResult = sendVoiceMessageUseCase(
                    conversationId = conversationId,
                    senderId = userId,
                    recordingResult = recordingResult
                )
                
                sendResult.onSuccess { message ->
                    android.util.Log.d("ChatViewModel", "Voice message sent: ${message.id}")
                    // Note: Transcription is now manual via long-press menu
                }.onFailure { error ->
                    android.util.Log.e("ChatViewModel", "Failed to send voice message", error)
                    _recordingState.value = RecordingState.Error(error.message ?: "Failed to send")
                }
            }.onFailure { error ->
                android.util.Log.e("ChatViewModel", "Failed to stop recording", error)
                _recordingState.value = RecordingState.Error(error.message ?: "Failed to stop recording")
            }
        }
    }
    
    /**
     * Cancel voice recording
     */
    fun cancelVoiceRecording() {
        viewModelScope.launch {
            audioRepository.cancelRecording()
            _recordingState.value = RecordingState.Idle
        }
    }
    
    /**
     * Play voice message
     */
    fun playVoiceMessage(messageId: String, audioUrl: String) {
        viewModelScope.launch {
            // Stop currently playing message if different
            val currentlyPlaying = _currentlyPlayingMessageId.value
            if (currentlyPlaying != null && currentlyPlaying != messageId) {
                stopVoiceMessage(currentlyPlaying)
            }
            
            _currentlyPlayingMessageId.value = messageId
            
            android.util.Log.d("ChatViewModel", "Starting to collect playback state for message: $messageId")
            
            playVoiceMessageUseCase(audioUrl, _playbackSpeed.value).collect { state ->
                android.util.Log.d("ChatViewModel", "Received playback state: $state for message: $messageId")
                
                // Create a new map to trigger StateFlow emission
                _playbackStates.value = _playbackStates.value.toMutableMap().apply {
                    put(messageId, state)
                }
                
                android.util.Log.d("ChatViewModel", "Updated _playbackStates map, new size: ${_playbackStates.value.size}")
                
                // Clear currently playing when completed
                if (state is PlaybackState.Completed) {
                    _currentlyPlayingMessageId.value = null
                }
            }
        }
    }
    
    /**
     * Pause voice message playback
     */
    fun pauseVoiceMessage(messageId: String) {
        viewModelScope.launch {
            playVoiceMessageUseCase.pause()
            // Update state will be handled by the flow
        }
    }
    
    /**
     * Resume voice message playback
     */
    fun resumeVoiceMessage(messageId: String) {
        viewModelScope.launch {
            playVoiceMessageUseCase.resume()
            // Update state will be handled by the flow
        }
    }
    
    /**
     * Stop voice message playback
     */
    fun stopVoiceMessage(messageId: String) {
        viewModelScope.launch {
            playVoiceMessageUseCase.stop()
            _playbackStates.value = _playbackStates.value - messageId
            if (_currentlyPlayingMessageId.value == messageId) {
                _currentlyPlayingMessageId.value = null
            }
        }
    }
    
    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        // Speed change is handled by the AudioPlayer
    }
    
    /**
     * Request transcription for a voice message
     */
    fun requestTranscription(messageId: String, audioUrl: String) {
        viewModelScope.launch {
            // Add to loading set (create new set to trigger StateFlow)
            _transcriptionLoading.value = _transcriptionLoading.value + messageId
            
            android.util.Log.d("ChatViewModel", "Requesting transcription for message $messageId")
            
            val result = transcribeVoiceMessageUseCase(messageId, audioUrl, conversationId)
            
            result.onSuccess { transcriptionResult ->
                android.util.Log.d("ChatViewModel", "Transcription success: ${transcriptionResult.text}")
                // Transcription is automatically updated in the message by the use case
            }.onFailure { error ->
                android.util.Log.e("ChatViewModel", "Transcription failed", error)
            }
            
            // Remove from loading set (create new set to trigger StateFlow)
            _transcriptionLoading.value = _transcriptionLoading.value - messageId
        }
    }
    
    /**
     * Get playback state for a message
     */
    fun getPlaybackState(messageId: String): PlaybackState {
        return _playbackStates.value[messageId] ?: PlaybackState.Idle
    }
    
    /**
     * Check if message is transcribing
     */
    fun isTranscribing(messageId: String): Boolean {
        return _transcriptionLoading.value.contains(messageId)
    }
    
    /**
     * Toggle play/pause for a voice message
     */
    fun togglePlayPause(messageId: String, audioUrl: String) {
        val currentState = getPlaybackState(messageId)
        
        when (currentState) {
            is PlaybackState.Playing -> pauseVoiceMessage(messageId)
            is PlaybackState.Paused -> resumeVoiceMessage(messageId)
            else -> playVoiceMessage(messageId, audioUrl)
        }
    }
    
    /**
     * Set or update the user's nickname in this conversation
     */
    fun setNickname(nickname: String?) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: return@launch
            
            conversationRepository.setNickname(conversationId, userId, nickname).fold(
                onSuccess = {
                    // Success - conversation flow will update automatically
                    android.util.Log.d("ChatViewModel", "Nickname updated")
                },
                onFailure = { error ->
                    android.util.Log.e("ChatViewModel", "Failed to set nickname", error)
                }
            )
        }
    }
    
    /**
     * Toggle auto-translate for this conversation
     */
    fun toggleAutoTranslate() {
        viewModelScope.launch {
            val currentlyEnabled = conversation.value?.autoTranslateEnabled ?: false
            conversationRepository.updateAutoTranslate(conversationId, !currentlyEnabled).fold(
                onSuccess = {
                    android.util.Log.d("ChatViewModel", "Auto-translate toggled: ${!currentlyEnabled} - SUCCESS")
                },
                onFailure = { error ->
                    android.util.Log.e("ChatViewModel", "Failed to toggle auto-translate", error)
                }
            )
        }
    }
    
    fun toggleSmartReplies() {
        viewModelScope.launch {
            val conv = conversation.value ?: run {
                android.util.Log.e("ChatViewModel", "Cannot toggle smart replies: conversation is null")
                return@launch
            }
            
            val userId = currentUserId.value ?: run {
                android.util.Log.e("ChatViewModel", "Cannot toggle smart replies: userId is null")
                return@launch
            }
            
            val user = userRepository.getUser(userId).getOrNull()
            val globalEnabled = user?.smartRepliesEnabled ?: true
            
            android.util.Log.d("ChatViewModel", "Toggling smart replies - current per-chat: ${conv.smartRepliesEnabled}, global: $globalEnabled")
            
            // Toggle logic:
            // - If currently using global (null):
            //   - If global is ON, set per-chat to OFF
            //   - If global is OFF, set per-chat to ON
            // - If currently explicitly enabled (true), set to false
            // - If currently explicitly disabled (false), reset to null (use global)
            val newValue = when (conv.smartRepliesEnabled) {
                null -> if (globalEnabled) false else true // Override global setting
                true -> false // Turn off
                false -> null // Reset to global
            }
            
            android.util.Log.d("ChatViewModel", "Toggling smart replies from ${conv.smartRepliesEnabled} to $newValue (global=$globalEnabled)")
            
            conversationRepository.updateSmartReplies(conversationId, newValue).fold(
                onSuccess = {
                    android.util.Log.d("ChatViewModel", "✅ Smart replies per-chat updated to: $newValue (does NOT affect global setting)")
                },
                onFailure = { error ->
                    android.util.Log.e("ChatViewModel", "❌ Failed to toggle smart replies per-chat", error)
                }
            )
        }
    }
}

