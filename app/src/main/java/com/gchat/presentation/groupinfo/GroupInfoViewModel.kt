package com.gchat.presentation.groupinfo

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.MediaRepository
import com.gchat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for group info and management
 */
@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    
    private val _conversation = MutableStateFlow<Conversation?>(null)
    val conversation: StateFlow<Conversation?> = _conversation.asStateFlow()
    
    private val _participants = MutableStateFlow<List<User>>(emptyList())
    val participants: StateFlow<List<User>> = _participants.asStateFlow()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    
    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()
    
    private val _isEditingName = MutableStateFlow(false)
    val isEditingName: StateFlow<Boolean> = _isEditingName.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    init {
        loadGroupInfo()
    }
    
    private fun loadGroupInfo() {
        viewModelScope.launch {
            // Load current user ID
            _currentUserId.value = authRepository.getCurrentUserId()
            
            // Observe conversation updates
            conversationRepository.getConversationFlow(conversationId).collect { conv ->
                _conversation.value = conv
                conv?.let {
                    _groupName.value = it.name ?: "Group"
                    _isAdmin.value = it.isAdmin(_currentUserId.value ?: "")
                    
                    // Load participants
                    loadParticipants(it.participants)
                }
            }
        }
    }
    
    private suspend fun loadParticipants(participantIds: List<String>) {
        val users = mutableListOf<User>()
        participantIds.forEach { userId ->
            userRepository.getUser(userId).onSuccess { user ->
                users.add(user)
            }
        }
        _participants.value = users
    }
    
    fun startEditingName() {
        _isEditingName.value = true
    }
    
    fun updateGroupName(newName: String) {
        _groupName.value = newName
    }
    
    fun saveGroupName() {
        viewModelScope.launch {
            val name = _groupName.value.trim()
            if (name.isBlank()) {
                _error.value = "Group name cannot be empty"
                return@launch
            }
            
            conversationRepository.updateGroupName(conversationId, name).fold(
                onSuccess = {
                    _isEditingName.value = false
                    _success.value = "Group name updated"
                },
                onFailure = { _error.value = "Failed to update group name: ${it.message}" }
            )
        }
    }
    
    fun cancelEditingName() {
        _groupName.value = _conversation.value?.name ?: "Group"
        _isEditingName.value = false
    }
    
    fun selectGroupIcon(uri: Uri) {
        viewModelScope.launch {
            _uploadProgress.value = 0f
            
            val storagePath = "group_icons/${conversationId}_${System.currentTimeMillis()}.jpg"
            mediaRepository.uploadImage(uri, storagePath).fold(
                onSuccess = { downloadUrl ->
                    conversationRepository.updateGroupIcon(conversationId, downloadUrl).fold(
                        onSuccess = {
                            _uploadProgress.value = null
                            _success.value = "Group icon updated"
                        },
                        onFailure = {
                            _uploadProgress.value = null
                            _error.value = "Failed to save group icon: ${it.message}"
                        }
                    )
                },
                onFailure = {
                    _uploadProgress.value = null
                    _error.value = "Failed to upload image: ${it.message}"
                }
            )
        }
    }
    
    fun promoteToAdmin(userId: String) {
        viewModelScope.launch {
            conversationRepository.promoteToAdmin(conversationId, userId).fold(
                onSuccess = { _success.value = "Member promoted to admin" },
                onFailure = { _error.value = "Failed to promote member: ${it.message}" }
            )
        }
    }
    
    fun removeMember(userId: String) {
        viewModelScope.launch {
            conversationRepository.removeParticipant(conversationId, userId).fold(
                onSuccess = { _success.value = "Member removed from group" },
                onFailure = { _error.value = "Failed to remove member: ${it.message}" }
            )
        }
    }
    
    fun leaveGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            conversationRepository.leaveGroup(conversationId, userId).fold(
                onSuccess = {
                    _success.value = "You left the group"
                    onSuccess()
                },
                onFailure = { _error.value = "Failed to leave group: ${it.message}" }
            )
        }
    }
    
    fun setNickname(nickname: String?) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            conversationRepository.setNickname(conversationId, userId, nickname).fold(
                onSuccess = {
                    if (nickname.isNullOrBlank()) {
                        _success.value = "Nickname removed"
                    } else {
                        _success.value = "Nickname updated to \"$nickname\""
                    }
                },
                onFailure = { _error.value = "Failed to update nickname: ${it.message}" }
            )
        }
    }
    
    fun getCurrentNickname(): String? {
        val userId = _currentUserId.value ?: return null
        return _conversation.value?.getNickname(userId)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccess() {
        _success.value = null
    }
}

