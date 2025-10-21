package com.gchat.presentation.creategroup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.MediaRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.domain.usecase.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val createGroupUseCase: CreateGroupUseCase,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _selectedParticipants = MutableStateFlow<List<User>>(emptyList())
    val selectedParticipants: StateFlow<List<User>> = _selectedParticipants.asStateFlow()

    private val _groupIconUri = MutableStateFlow<Uri?>(null)
    val groupIconUri: StateFlow<Uri?> = _groupIconUri.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val currentUserId = authRepository.getCurrentUserId()

    init {
        _searchQuery
            .debounce(300L)
            .filter { it.length >= 2 || it.isEmpty() }
            .onEach { query ->
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                    return@onEach
                }
                _isSearching.value = true
                searchUsers(query)
            }
            .launchIn(viewModelScope)
    }

    fun updateGroupName(name: String) {
        _groupName.value = name
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGroupIcon(uri: Uri?) {
        _groupIconUri.value = uri
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                userRepository.searchUsers(query, userId)
                    .onSuccess { users ->
                        // Filter out already selected participants
                        val selectedIds = _selectedParticipants.value.map { it.id }.toSet()
                        _searchResults.value = users.filter { it.id !in selectedIds }
                        _isSearching.value = false
                    }
                    .onFailure { e ->
                        _error.value = "Failed to search users: ${e.message}"
                        _isSearching.value = false
                    }
            } ?: run {
                _error.value = "User not authenticated."
                _isSearching.value = false
            }
        }
    }

    fun addParticipant(user: User) {
        _selectedParticipants.value = _selectedParticipants.value + user
        // Remove from search results
        _searchResults.value = _searchResults.value.filter { it.id != user.id }
    }

    fun removeParticipant(user: User) {
        _selectedParticipants.value = _selectedParticipants.value.filter { it.id != user.id }
    }

    fun createGroup(onComplete: (String) -> Unit) {
        val name = _groupName.value.trim()
        val participants = _selectedParticipants.value

        if (name.isBlank()) {
            _error.value = "Please enter a group name"
            return
        }

        if (participants.size < 2) {
            _error.value = "Please select at least 2 participants"
            return
        }

        currentUserId?.let { userId ->
            viewModelScope.launch {
                _isCreating.value = true

                // Upload group icon if provided
                val iconUrl = _groupIconUri.value?.let { uri ->
                    val storagePath = "group_icons/${System.currentTimeMillis()}.jpg"
                    mediaRepository.uploadImage(uri, storagePath)
                        .getOrNull()
                }

                // Create group
                createGroupUseCase(
                    creatorUserId = userId,
                    participantIds = participants.map { it.id },
                    groupName = name,
                    groupIconUrl = iconUrl
                ).onSuccess { conversation ->
                    _isCreating.value = false
                    onComplete(conversation.id)
                }.onFailure { e ->
                    _error.value = "Failed to create group: ${e.message}"
                    _isCreating.value = false
                }
            }
        } ?: run {
            _error.value = "User not authenticated."
        }
    }

    fun clearError() {
        _error.value = null
    }
}

