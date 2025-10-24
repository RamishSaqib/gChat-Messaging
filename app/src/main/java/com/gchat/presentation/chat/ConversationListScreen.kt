package com.gchat.presentation.chat

import android.Manifest
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gchat.domain.model.Conversation
import com.gchat.domain.model.User
import com.gchat.presentation.components.ProfilePicture
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationListScreen(
    pendingConversationId: String? = null,
    onConversationClick: (String) -> Unit,
    onNewConversationClick: () -> Unit,
    onNewGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val conversationsWithUsers by viewModel.conversationsWithUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isInitialLoad by viewModel.isInitialLoad.collectAsState()
    var showFabMenu by remember { mutableStateOf(false) }
    
    // Handle navigation from notification
    LaunchedEffect(pendingConversationId) {
        if (pendingConversationId != null) {
            onConversationClick(pendingConversationId)
        }
    }
    
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp
            ) {
                Column {
                    // Custom top bar with centered title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Profile picture button (left)
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            ProfilePicture(
                                url = currentUser?.profilePictureUrl,
                                displayName = currentUser?.displayName ?: "User",
                                size = 44.dp,
                                showOnlineIndicator = false
                            )
                        }
                        
                        // Centered title
                        Text(
                            text = "Chats",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        
                        // Logout button (right)
                        IconButton(
                            onClick = {
                                viewModel.logout {
                                    onLogout()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Divider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        },
        floatingActionButton = {
            Box {
                // iOS-style rounded square FAB
                Surface(
                    onClick = { showFabMenu = !showFabMenu },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New conversation",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isInitialLoad) {
                // Show loading indicator during initial load
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (conversationsWithUsers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a new conversation to connect with others",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNewConversationClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Get Started")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        conversationsWithUsers,
                        key = { "${it.conversation.id}_${it.conversation.updatedAt}" }
                    ) { conversationWithUser ->
                        // Use lastMessageTimestamp as key to reset dismiss state when conversation reappears with new message
                        val dismissState = rememberDismissState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    // Trigger deletion - item will be filtered from UI immediately
                                    viewModel.deleteConversation(conversationWithUser.conversation.id)
                                    true
                                } else {
                                    false
                                }
                            },
                            positionalThreshold = { distance -> distance * 0.25f }
                        )
                        
                        // Reset dismiss state when conversation reappears (new message after deletion)
                        LaunchedEffect(conversationWithUser.conversation.lastMessage?.timestamp) {
                            if (dismissState.currentValue != DismissValue.Default) {
                                dismissState.reset()
                            }
                        }
                        
                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                val color = when (dismissState.targetValue) {
                                    DismissValue.DismissedToStart -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.targetValue == DismissValue.DismissedToStart) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            },
                            dismissContent = {
                                ConversationItem(
                                    conversationWithUser = conversationWithUser,
                                    currentUserId = viewModel.currentUser.value?.id,
                                    onClick = { onConversationClick(conversationWithUser.conversation.id) }
                                )
                            }
                        )
                    }
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
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture with fallback to initials
            ProfilePicture(
                url = profilePictureUrl,
                displayName = displayName,
                size = 60.dp,
                showOnlineIndicator = true,
                isOnline = otherUser?.isActuallyOnline() ?: false
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTimestamp(conversation.updatedAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (conversation.unreadCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = buildLastMessageText(
                        conversation = conversation,
                        lastMessageSender = lastMessageSender,
                        currentUserId = currentUserId
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    android.util.Log.d("ConversationPreview", "lastMessage - text: '${lastMessage.text}', mediaUrl: '${lastMessage.mediaUrl}', type: ${lastMessage.type}")
    
    // Handle SYSTEM messages (like reactions)
    // Only show reaction preview if current user is the owner of the original message that was reacted to
    if (lastMessage.type == com.gchat.domain.model.MessageType.SYSTEM) {
        // Check if current user is the message owner (originalMessageSenderId)
        // If not, or if originalMessageSenderId is null, don't show the reaction preview
        if (lastMessage.originalMessageSenderId != currentUserId) {
            // User is not the message owner, don't show reaction preview
            return "New message"
        }
        // Show reaction preview only for the message owner
        return lastMessage.text ?: "System message"
    }
    
    val messageContent = when {
        lastMessage.text != null && lastMessage.text.isNotBlank() -> lastMessage.text
        lastMessage.mediaUrl != null -> "📷 Image"
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

