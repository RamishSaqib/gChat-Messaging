package com.gchat.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.MediaRepository
import com.gchat.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for user profile editing
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()
    
    private val _profilePictureUrl = MutableStateFlow<String?>(null)
    val profilePictureUrl: StateFlow<String?> = _profilePictureUrl.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                userRepository.getUserFlow(userId).collect { user ->
                    _currentUser.value = user
                    user?.let {
                        _displayName.value = it.displayName
                        _profilePictureUrl.value = it.profilePictureUrl
                    }
                }
            }
        }
    }
    
    fun updateDisplayName(name: String) {
        _displayName.value = name
    }
    
    fun selectProfilePicture(uri: Uri) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            _uploadProgress.value = 0f
            
            // Upload image to Firebase Storage (must match storage rules path structure)
            val storagePath = "profile_pictures/${userId}/${System.currentTimeMillis()}.jpg"
            mediaRepository.uploadImage(uri, storagePath).fold(
                onSuccess = { downloadUrl ->
                    _profilePictureUrl.value = downloadUrl
                    _uploadProgress.value = null
                },
                onFailure = { exception ->
                    _error.value = "Failed to upload image: ${exception.message}"
                    _uploadProgress.value = null
                }
            )
        }
    }
    
    fun saveProfile() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val name = _displayName.value.trim()
            
            if (name.isBlank()) {
                _error.value = "Display name cannot be empty"
                return@launch
            }
            
            _isSaving.value = true
            _error.value = null
            
            val updates = mutableMapOf<String, Any?>(
                "displayName" to name
            )
            
            // Only update profile picture if it changed
            if (_profilePictureUrl.value != _currentUser.value?.profilePictureUrl) {
                updates["profilePictureUrl"] = _profilePictureUrl.value
            }
            
            authRepository.updateUserProfile(userId, updates).fold(
                onSuccess = {
                    _saveSuccess.value = true
                    _isSaving.value = false
                },
                onFailure = { exception ->
                    _error.value = "Failed to save profile: ${exception.message}"
                    _isSaving.value = false
                }
            )
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

