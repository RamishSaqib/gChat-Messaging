package com.gchat.presentation.chat

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.User
import com.gchat.presentation.components.ProfilePicture
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (String) -> Unit,
    onNewConversationClick: () -> Unit,
    onNewGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val conversationsWithUsers by viewModel.conversationsWithUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var showFabMenu by remember { mutableStateOf(false) }
    
    // Request notification permission for Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("gChat") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    // Profile picture button (on the left)
                    IconButton(onClick = onProfileClick) {
                        ProfilePicture(
                            url = currentUser?.profilePictureUrl,
                            displayName = currentUser?.displayName ?: "User",
                            size = 40.dp,
                            showOnlineIndicator = false
                        )
                    }
                },
                actions = {
                    // Logout button (on the right)
                    IconButton(onClick = {
                        viewModel.logout {
                            onLogout()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(Icons.Default.Add, contentDescription = "New conversation")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Conversation") },
                        onClick = {
                            showFabMenu = false
                            onNewConversationClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Group") },
                        onClick = {
                            showFabMenu = false
                            onNewGroupClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Group, contentDescription = null)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (conversationsWithUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to start chatting",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(conversationsWithUsers, key = { it.conversation.id }) { conversationWithUser ->
                    ConversationItem(
                        conversationWithUser = conversationWithUser,
                        currentUserId = viewModel.currentUser.value?.id,
                        onClick = { onConversationClick(conversationWithUser.conversation.id) }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversationWithUser: ConversationWithUser,
    currentUserId: String?,
    onClick: () -> Unit
) {
    val conversation = conversationWithUser.conversation
    val otherUser = conversationWithUser.otherUser
    val lastMessageSender = conversationWithUser.lastMessageSender
    
    // Display name: use nickname if set, otherwise real name
    val displayName = when {
        otherUser != null -> conversation.getUserDisplayName(otherUser.id, otherUser)
        conversation.name != null -> conversation.name
        else -> "Unknown User"
    }
    
    // Profile picture URL: use other user's picture for 1-on-1, group icon for groups
    val profilePictureUrl = when {
        otherUser != null -> otherUser.profilePictureUrl
        else -> conversation.iconUrl
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile picture with fallback to initials
        ProfilePicture(
            url = profilePictureUrl,
            displayName = displayName,
            size = 56.dp,
            showOnlineIndicator = true,
            isOnline = otherUser?.isOnline ?: false
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = formatTimestamp(conversation.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = buildLastMessageText(
                    conversation = conversation,
                    lastMessageSender = lastMessageSender,
                    currentUserId = currentUserId
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (conversation.unreadCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private fun buildLastMessageText(
    conversation: Conversation,
    lastMessageSender: User?,
    currentUserId: String?
): String {
    val lastMessage = conversation.lastMessage
    
    if (lastMessage == null) {
        return "No messages yet"
    }
    
    // Get the message content
    val messageContent = when {
        lastMessage.text != null -> lastMessage.text
        lastMessage.mediaUrl != null -> "📷 Photo"
        else -> "New message"
    }
    
    // For group chats, always show sender name prefix
    if (conversation.isGroup()) {
        val senderPrefix = when {
            lastMessage.senderId == currentUserId -> "You: "
            else -> {
                // Use nickname if set, otherwise use display name
                val senderName = conversation.getUserDisplayName(
                    lastMessage.senderId,
                    lastMessageSender
                )
                "$senderName: "
            }
        }
        return "$senderPrefix$messageContent"
    }
    
    // For DMs (1-on-1), only show "You: " if it's your message
    // The other person's name is already in the conversation title
    if (lastMessage.senderId == currentUserId) {
        return "You: $messageContent"
    }
    
    return messageContent
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

