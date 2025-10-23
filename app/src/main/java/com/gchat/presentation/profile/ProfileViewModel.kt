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
    
    private val _autoTranslateEnabled = MutableStateFlow(false)
    val autoTranslateEnabled: StateFlow<Boolean> = _autoTranslateEnabled.asStateFlow()
    
    private val _smartRepliesEnabled = MutableStateFlow(true)
    val smartRepliesEnabled: StateFlow<Boolean> = _smartRepliesEnabled.asStateFlow()
    
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
            android.util.Log.d("ProfileViewModel", "🔄 Loading profile for user: $userId")
            if (userId != null) {
                try {
                    userRepository.getUserFlow(userId).collect { user ->
                        android.util.Log.d("ProfileViewModel", "📥 User data collected: displayName=${user?.displayName}, email=${user?.email}, smartReplies=${user?.smartRepliesEnabled}, autoTranslate=${user?.autoTranslateEnabled}")
                        _currentUser.value = user
                        user?.let {
                            _displayName.value = it.displayName
                            _autoTranslateEnabled.value = it.autoTranslateEnabled
                            _smartRepliesEnabled.value = it.smartRepliesEnabled
                            _profilePictureUrl.value = it.profilePictureUrl
                            android.util.Log.d("ProfileViewModel", "✅ Updated UI state - displayName: ${it.displayName}, autoTranslate: ${it.autoTranslateEnabled}, smartReplies: ${it.smartRepliesEnabled}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileViewModel", "❌ Error loading user profile", e)
                    _error.value = "Failed to load profile: ${e.message}"
                }
            } else {
                android.util.Log.e("ProfileViewModel", "❌ No user ID found!")
                _error.value = "No user session found. Please log in again."
            }
        }
    }
    
    fun updateDisplayName(name: String) {
        _displayName.value = name
    }
    
    fun updateAutoTranslate(enabled: Boolean) {
        _autoTranslateEnabled.value = enabled
    }
    
    fun updateSmartReplies(enabled: Boolean) {
        android.util.Log.d("ProfileViewModel", "📝 Local state update: smartRepliesEnabled = $enabled (NOT saved to Firestore yet)")
        _smartRepliesEnabled.value = enabled
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
                "displayName" to name,
                "autoTranslateEnabled" to _autoTranslateEnabled.value,
                "smartRepliesEnabled" to _smartRepliesEnabled.value
            )
            
            android.util.Log.d("ProfileViewModel", "💾 Saving GLOBAL settings to Firestore: $updates")
            
            // Only update profile picture if it changed
            if (_profilePictureUrl.value != _currentUser.value?.profilePictureUrl) {
                updates["profilePictureUrl"] = _profilePictureUrl.value
            }
            
            authRepository.updateUserProfile(userId, updates).fold(
                onSuccess = {
                    android.util.Log.d("ProfileViewModel", "✅ Profile saved successfully (GLOBAL settings updated)")
                    _saveSuccess.value = true
                    _isSaving.value = false
                },
                onFailure = { exception ->
                    android.util.Log.e("ProfileViewModel", "❌ Failed to save profile", exception)
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

