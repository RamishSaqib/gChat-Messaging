package com.gchat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.Conversation
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.usecase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for conversation list screen
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    getConversationsUseCase: GetConversationsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val conversations: StateFlow<List<Conversation>> = getConversationsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), // Stop immediately when UI unsubscribes
            initialValue = emptyList()
        )
    
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}

