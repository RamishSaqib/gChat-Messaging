package com.gchat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.UserRepository
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
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    // Cache for user data flows to observe real-time status changes
    private val userFlowCache = mutableMapOf<String, StateFlow<User?>>()
    
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
    val conversationsWithUsers: StateFlow<List<ConversationWithUser>> = flow {
        val currentUserId = authRepository.getCurrentUserId() ?: ""
        
        getConversationsUseCase().collect { conversations ->
            // Create a combined flow that updates when any user status changes
            val conversationFlows = conversations.map { conversation ->
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
            combine(conversationFlows) { it.toList() }.collect { conversationsWithUsers ->
                emit(conversationsWithUsers)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = emptyList()
    )
    
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

