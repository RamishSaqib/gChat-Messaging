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
    
    // Combine conversations with user data
    val conversationsWithUsers: StateFlow<List<ConversationWithUser>> = flow {
        val currentUserId = authRepository.getCurrentUserId() ?: ""
        
        getConversationsUseCase().collect { conversations ->
            val conversationsWithUsers = conversations.map { conversation ->
                val otherUserId = conversation.getOtherParticipantId(currentUserId)
                val otherUser = if (otherUserId != null) {
                    userRepository.getUser(otherUserId).getOrNull()
                } else null
                
                ConversationWithUser(
                    conversation = conversation,
                    otherUser = otherUser
                )
            }
            emit(conversationsWithUsers)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
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
    val otherUser: User?
)

