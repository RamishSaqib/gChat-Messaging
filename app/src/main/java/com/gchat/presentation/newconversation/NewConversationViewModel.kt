package com.gchat.presentation.newconversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.domain.usecase.CreateConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for new conversation screen
 */
@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val createConversationUseCase: CreateConversationUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchUsers(query)
        } else {
            _searchResults.value = emptyList()
        }
    }
    
    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _error.value = "Not authenticated"
                _isSearching.value = false
                return@launch
            }
            
            userRepository.searchUsers(query, currentUserId).fold(
                onSuccess = { users ->
                    _searchResults.value = users
                    _isSearching.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to search users"
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                }
            )
        }
    }
    
    fun createConversation(otherUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _error.value = null
            
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _error.value = "Not authenticated"
                return@launch
            }
            
            createConversationUseCase(currentUserId, otherUserId).fold(
                onSuccess = { conversation ->
                    onSuccess(conversation.id)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to create conversation"
                }
            )
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}

