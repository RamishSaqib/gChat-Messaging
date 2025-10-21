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
    
    init {
        loadOtherUser()
    }
    
    private fun loadOtherUser() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val conversation = conversationRepository.getConversation(conversationId).getOrNull() ?: return@launch
            
            val otherUserId = conversation.getOtherParticipantId(currentUserId)
            if (otherUserId != null) {
                userRepository.getUserFlow(otherUserId).collect { user ->
                    _otherUser.value = user
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: DMInfoViewModel = hiltViewModel()
) {
    val otherUser by viewModel.otherUser.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Info") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change My Nickname") },
                            onClick = {
                                showMenu = false
                                showNicknameDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
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
                isOnline = otherUser?.isOnline ?: false
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
                    if (otherUser?.isOnline == true) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }
                Text(
                    text = if (otherUser?.isOnline == true) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Nickname Dialog (placeholder)
    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Change Nickname") },
            text = { Text("Nickname feature coming soon!") },
            confirmButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

