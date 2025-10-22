package com.gchat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.domain.usecase.DeleteConversationUseCase
import com.gchat.domain.usecase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for conversation list screen
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    getConversationsUseCase: GetConversationsUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    // Cache for user data flows to observe real-time status changes
    private val userFlowCache = mutableMapOf<String, StateFlow<User?>>()
    
    // Track conversations being dismissed for immediate UI removal
    private val _dismissedConversationIds = MutableStateFlow<Set<String>>(emptySet())
    
    // Current user information
    val currentUser: StateFlow<User?> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            userRepository.getUserFlow(userId).collect { user ->
                emit(user)
            }
        } else {
            emit(null)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Combine conversations with real-time user data
    val conversationsWithUsers: StateFlow<List<ConversationWithUser>> = getConversationsUseCase()
        .flatMapLatest { conversations ->
            val currentUserId = authRepository.getCurrentUserId() ?: ""
            android.util.Log.d("ConversationListVM", "UseCase emitted ${conversations.size} conversations")
            
            // Filter: only show conversations that are not deleted by current user
            // Conversation is "deleted" if deletion timestamp > last message timestamp
            // This allows chat to automatically reappear when new message arrives
            val visibleConversations = conversations.filter { conversation ->
                val deletionTimestamp = conversation.deletedAt[currentUserId]
                val lastMessageTimestamp = conversation.lastMessage?.timestamp ?: 0L
                
                // If user deleted the chat, only show if there's a newer message
                val isDeleted = deletionTimestamp != null && lastMessageTimestamp <= deletionTimestamp
                
                !isDeleted && (conversation.lastMessage != null || conversation.creatorId == currentUserId)
            }
            android.util.Log.d("ConversationListVM", "Filtered to ${visibleConversations.size} visible conversations")
            
            // Create a combined flow that updates when any user status changes
            val conversationFlows = visibleConversations.map { conversation ->
                val otherUserId = conversation.getOtherParticipantId(currentUserId)
                val lastMessageSenderId = conversation.lastMessage?.senderId
                
                if (otherUserId != null) {
                    // 1-on-1 chat: Get other user's info
                    val userFlow = userFlowCache.getOrPut(otherUserId) {
                        userRepository.getUserFlow(otherUserId)
                            .stateIn(
                                scope = viewModelScope,
                                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                                initialValue = null
                            )
                    }
                    
                    // Also get last message sender (could be either user)
                    if (lastMessageSenderId != null && lastMessageSenderId != currentUserId) {
                        val senderFlow = userFlowCache.getOrPut(lastMessageSenderId) {
                            userRepository.getUserFlow(lastMessageSenderId)
                                .stateIn(
                                    scope = viewModelScope,
                                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                                    initialValue = null
                                )
                        }
                        combine(userFlow, senderFlow) { user, sender ->
                            ConversationWithUser(
                                conversation = conversation,
                                otherUser = user,
                                lastMessageSender = sender
                            )
                        }
                    } else {
                        userFlow.map { user ->
                            ConversationWithUser(
                                conversation = conversation,
                                otherUser = user,
                                lastMessageSender = null
                            )
                        }
                    }
                } else {
                    // Group conversation: get last message sender
                    if (lastMessageSenderId != null) {
                        val senderFlow = userFlowCache.getOrPut(lastMessageSenderId) {
                            userRepository.getUserFlow(lastMessageSenderId)
                                .stateIn(
                                    scope = viewModelScope,
                                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                                    initialValue = null
                                )
                        }
                        senderFlow.map { sender ->
                            ConversationWithUser(
                                conversation = conversation,
                                otherUser = null,
                                lastMessageSender = sender
                            )
                        }
                    } else {
                        flowOf(ConversationWithUser(conversation = conversation, otherUser = null, lastMessageSender = null))
                    }
                }
            }
            
            // Combine all conversation flows and emit as a list
            combine(conversationFlows) { it.toList() }
                .combine(_dismissedConversationIds) { conversations, dismissedIds ->
                    // Clear dismissed IDs for conversations that have reappeared (new message after deletion)
                    val visibleConversationIds = conversations.map { it.conversation.id }.toSet()
                    val staleDismissedIds = dismissedIds - visibleConversationIds
                    if (staleDismissedIds != dismissedIds) {
                        _dismissedConversationIds.value = staleDismissedIds
                    }
                    
                    // Filter out dismissed conversations for immediate UI update
                    conversations.filter { it.conversation.id !in dismissedIds }
                }
                .onEach { conversationsWithUsers ->
                    android.util.Log.d("ConversationListVM", "Emitting ${conversationsWithUsers.size} conversations with users to UI")
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Changed to Eagerly to keep collecting even when screen not visible
            initialValue = emptyList()
        )
    
    fun deleteConversation(conversationId: String, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            android.util.Log.d("ConversationListVM", "Deleting conversation: $conversationId")
            
            // Immediately add to dismissed set for instant UI removal
            _dismissedConversationIds.value = _dismissedConversationIds.value + conversationId
            
            val result = deleteConversationUseCase(conversationId)
            result.fold(
                onSuccess = {
                    android.util.Log.d("ConversationListVM", "Conversation deleted successfully")
                    // Keep it in dismissed set - it will be gone from data source anyway
                    onComplete(Result.success(Unit))
                },
                onFailure = { error ->
                    android.util.Log.e("ConversationListVM", "Failed to delete conversation: ${error.message}")
                    // Remove from dismissed set to show it again if deletion failed
                    _dismissedConversationIds.value = _dismissedConversationIds.value - conversationId
                    onComplete(Result.failure(error))
                }
            )
        }
    }
    
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}

/**
 * Data class combining conversation with other participant's user data
 */
data class ConversationWithUser(
    val conversation: Conversation,
    val otherUser: User?,
    val lastMessageSender: User? = null
)

