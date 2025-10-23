package com.gchat.presentation.dminfo

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gchat.domain.model.User
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.ConversationRepository
import com.gchat.domain.repository.UserRepository
import com.gchat.presentation.components.ProfilePicture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for DM info screen
 */
@HiltViewModel
class DMInfoViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    
    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser.asStateFlow()
    
    private val _conversation = MutableStateFlow<com.gchat.domain.model.Conversation?>(null)
    val conversation: StateFlow<com.gchat.domain.model.Conversation?> = _conversation.asStateFlow()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadOtherUser()
        loadConversation()
    }
    
    private fun loadOtherUser() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            _currentUserId.value = currentUserId
            
            if (currentUserId == null) return@launch
            val conversation = conversationRepository.getConversation(conversationId).getOrNull() ?: return@launch
            
            val otherUserId = conversation.getOtherParticipantId(currentUserId)
            if (otherUserId != null) {
                userRepository.getUserFlow(otherUserId).collect { user ->
                    _otherUser.value = user
                }
            }
        }
    }
    
    private fun loadConversation() {
        viewModelScope.launch {
            conversationRepository.getConversationFlow(conversationId).collect { conv ->
                _conversation.value = conv
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DMInfoViewModel = hiltViewModel()
) {
    val otherUser by viewModel.otherUser.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val success by viewModel.success.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show success/error messages
    LaunchedEffect(success) {
        success?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chat Info") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // User Profile Picture
            ProfilePicture(
                url = otherUser?.profilePictureUrl,
                displayName = otherUser?.displayName ?: "User",
                size = 120.dp,
                showOnlineIndicator = true,
                isOnline = otherUser?.isActuallyOnline() ?: false
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // User Name
            Text(
                text = otherUser?.displayName ?: "Loading...",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email
            otherUser?.email?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Online Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(end = 4.dp)
                ) {
                    if (otherUser?.isActuallyOnline() == true) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }
                Text(
                    text = if (otherUser?.isActuallyOnline() == true) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Nickname Dialog
    if (showNicknameDialog) {
        var nicknameText by remember { mutableStateOf(viewModel.getCurrentNickname() ?: "") }
        
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Change My Nickname") },
            text = {
                Column {
                    Text(
                        text = "Set a custom nickname that only shows in this chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nicknameText,
                        onValueChange = { nicknameText = it },
                        label = { Text("Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNickname(nicknameText.ifBlank { null })
                    showNicknameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                if (nicknameText.isNotBlank()) {
                    TextButton(onClick = {
                        viewModel.setNickname(null)
                        showNicknameDialog = false
                    }) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

