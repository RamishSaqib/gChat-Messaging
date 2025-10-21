package com.gchat.presentation.chat

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.gchat.domain.model.MessageType
import com.gchat.presentation.components.ImageMessageBubble
import com.gchat.presentation.components.ReadByAvatars
import com.gchat.presentation.components.ReadReceiptCheckmarks
import com.gchat.util.rememberImagePickerLaunchers
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val participantUsers by viewModel.participantUsers.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val listState = rememberLazyListState()
    
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera permission
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Image picker
    val imagePickerLaunchers = rememberImagePickerLaunchers(
        onImageSelected = { uri ->
            selectedImageUri = uri
            showImagePicker = false
        }
    )
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Mark messages as read when screen is viewed
    LaunchedEffect(Unit) {
        viewModel.markAllMessagesAsRead()
    }
    
    // Also mark messages as read when new messages arrive
    LaunchedEffect(messages.size) {
        viewModel.markAllMessagesAsRead()
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error snackbar
    uploadError?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUploadError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(otherUserName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                messageText = messageText,
                onMessageChange = viewModel::updateMessageText,
                onSendClick = {
                    selectedImageUri?.let { uri ->
                        // Send image with caption
                        viewModel.sendImageMessage(uri, messageText.ifBlank { null })
                        selectedImageUri = null
                    } ?: run {
                        // Send text only
                        viewModel.sendMessage()
                    }
                },
                onAttachmentClick = { showImagePicker = true },
                isUploading = uploadProgress != null,
                selectedImageUri = selectedImageUri,
                onClearImage = { selectedImageUri = null }
            )
        }
    ) { paddingValues ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No messages yet\nSay hi! 👋",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                state = listState
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isOwnMessage = message.senderId == currentUserId,
                        currentUserId = currentUserId ?: "",
                        isGroupChat = conversation?.isGroup() == true,
                        senderName = participantUsers[message.senderId]?.displayName,
                        participantUsers = participantUsers,
                        onImageClick = { imageUrl ->
                            // Navigate to image viewer (will add this shortly)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    // Image picker bottom sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false }
        ) {
            ImagePickerBottomSheet(
                onCameraClick = {
                    if (cameraPermissionState.status.isGranted) {
                        imagePickerLaunchers.launchCamera()
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                onGalleryClick = {
                    imagePickerLaunchers.launchGallery()
                },
                onDismiss = { showImagePicker = false }
            )
        }
    }
    
    // Launch camera when permission is granted
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && showImagePicker) {
            // Permission was just granted, now launch camera
            // (This will trigger if user grants permission from the dialog)
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    currentUserId: String,
    isGroupChat: Boolean = false,
    senderName: String? = null,
    participantUsers: Map<String, com.gchat.domain.model.User> = emptyMap(),
    onImageClick: (String) -> Unit
) {
    // Get users who have read this message (excluding the sender)
    val readByUsers = message.readBy.keys
        .filter { it != message.senderId }
        .mapNotNull { userId -> participantUsers[userId] }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Show sender name for group chats (only for others' messages)
        if (isGroupChat && !isOwnMessage && senderName != null) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            // Use ImageMessageBubble for image messages
            if (message.type == MessageType.IMAGE && message.mediaUrl != null) {
                ImageMessageBubble(
                    message = message,
                    isCurrentUser = isOwnMessage,
                    onImageClick = onImageClick
                )
            } else {
                // Regular text message bubble
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOwnMessage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = message.text ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOwnMessage) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Timestamp and read receipts
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOwnMessage) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                }
                            )
                            
                            // Show read receipts only for own messages
                            if (isOwnMessage) {
                                if (isGroupChat) {
                                    // Show avatar bubbles for group chats
                                    if (readByUsers.isNotEmpty()) {
                                        ReadByAvatars(
                                            readByUsers = readByUsers,
                                            maxVisible = 3
                                        )
                                    }
                                } else {
                                    // Show checkmarks for 1-on-1 chats
                                    ReadReceiptCheckmarks(
                                        isRead = message.isReadByAny(),
                                        isSent = message.status == MessageStatus.SENT || message.status == MessageStatus.READ
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    isUploading: Boolean = false,
    selectedImageUri: Uri? = null,
    onClearImage: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column {
            // Image preview (if image selected)
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Remove button
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachmentClick,
                    enabled = !isUploading && selectedImageUri == null
                ) {
                    Icon(Icons.Filled.AttachFile, "Attach image")
                }
                
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (selectedImageUri != null) "Add a caption..." else "Type a message...") },
                    maxLines = 4,
                    enabled = !isUploading
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    FilledIconButton(
                        onClick = onSendClick,
                        enabled = messageText.isNotBlank() || selectedImageUri != null
                    ) {
                        Icon(Icons.Filled.Send, "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePickerBottomSheet(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Image",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Camera option
        Surface(
            onClick = {
                onCameraClick()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Take photo",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Camera",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Take a new photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Gallery option
        Surface(
            onClick = {
                onGalleryClick()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = "Choose from gallery",
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Choose from your photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

