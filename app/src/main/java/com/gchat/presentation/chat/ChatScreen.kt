package com.gchat.presentation.chat

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.gchat.domain.model.Message
import com.gchat.domain.model.MessageStatus
import com.gchat.domain.model.MessageType
import com.gchat.domain.model.Translation
import com.gchat.presentation.components.ImageMessageBubble
import com.gchat.presentation.components.ProfilePicture
import com.gchat.presentation.components.ReadByAvatars
import com.gchat.presentation.components.ReadReceiptCheckmarks
import com.gchat.presentation.components.VoiceMessageBubble
import com.gchat.presentation.components.VoiceRecordingSheet
import com.gchat.presentation.theme.MessageBubbleShapeReceived
import com.gchat.presentation.theme.MessageBubbleShapeSent
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
    onNavigateToGroupInfo: ((String) -> Unit)? = null,
    onNavigateToDMInfo: ((String) -> Unit)? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val participantUsers by viewModel.participantUsers.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    
    // Smart Reply state
    val smartReplies by viewModel.smartReplies.collectAsState()
    val smartRepliesLoading by viewModel.smartRepliesLoading.collectAsState()
    val smartRepliesError by viewModel.smartRepliesError.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val typingIndicatorText by viewModel.typingIndicatorText.collectAsState()
    
    // Translation state
    val translations by viewModel.translations.collectAsState()
    val translationLoading by viewModel.translationLoading.collectAsState()
    val translationErrors by viewModel.translationErrors.collectAsState()
    
    // Data extraction state
    val extractedData by viewModel.extractedData.collectAsState()
    
    // Cultural context state
    val culturalContexts by viewModel.culturalContexts.collectAsState()
    
    // Formality adjustment state
    val selectedFormality by viewModel.selectedFormality.collectAsState()
    val formalityLoading by viewModel.formalityLoading.collectAsState()
    
    // Voice message state
    val recordingState by viewModel.recordingState.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val playbackStates by viewModel.playbackStates.collectAsState()
    val transcriptionLoading by viewModel.transcriptionLoading.collectAsState()
    
    var showRecordingSheet by remember { mutableStateOf(false) }
    var showChatMenu by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    
    // Track active conversation for notification suppression
    val application = LocalContext.current.applicationContext as? com.gchat.GChatApplication
    LaunchedEffect(conversationId) {
        application?.currentConversationId = conversationId
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Clear when leaving chat screen
            application?.currentConversationId = null
        }
    }
    
    // Audio permission
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    val listState = rememberLazyListState()
    
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCulturalContextSheet by remember { mutableStateOf(false) }
    var selectedMessageForContext by remember { mutableStateOf<String?>(null) }
    
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
    
    // Mark new messages as read when they arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            viewModel.markAllMessagesAsRead()
        }
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 0.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        conversation?.let { conv ->
                                            if (conv.isGroup()) {
                                                onNavigateToGroupInfo?.invoke(conversationId)
                                            } else {
                                                onNavigateToDMInfo?.invoke(conversationId)
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isGroup = conversation?.isGroup() == true
                                val otherUserId = conversation?.getOtherParticipantId(currentUserId ?: "")
                                
                                // Profile picture or group icon
                                val imageUrl = if (isGroup) {
                                    conversation?.iconUrl
                                } else {
                                    // Get other user's profile picture for 1-on-1 chat
                                    otherUserId?.let { participantUsers[it]?.profilePictureUrl }
                                }
                                
                                // Online status (only for 1-on-1 chats)
                                val isOnline = if (!isGroup && otherUserId != null) {
                                    participantUsers[otherUserId]?.isActuallyOnline() ?: false
                                } else {
                                    false
                                }
                                
                                ProfilePicture(
                                    url = imageUrl,
                                    displayName = otherUserName,
                                    size = 40.dp,
                                    showOnlineIndicator = !isGroup,
                                    isOnline = isOnline
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = otherUserName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            // Show menu for all chats
                            IconButton(onClick = { showChatMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Menu")
                            }
                            DropdownMenu(
                                expanded = showChatMenu,
                                onDismissRequest = { showChatMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Change My Nickname") },
                                    onClick = {
                                        showChatMenu = false
                                        showNicknameDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text("Auto-translate")
                                            Text(
                                                text = if (conversation?.autoTranslateEnabled == true) "Enabled" else "Disabled",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        // Don't close menu, just toggle
                                        viewModel.toggleAutoTranslate()
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = conversation?.autoTranslateEnabled == true,
                                            onCheckedChange = { 
                                                viewModel.toggleAutoTranslate()
                                            }
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Translate, null) }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text("Smart Replies")
                                            Text(
                                                text = when (conversation?.smartRepliesEnabled) {
                                                    true -> "Enabled"
                                                    false -> "Disabled"
                                                    null -> "Using Global"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        // Don't close menu, just toggle
                                        viewModel.toggleSmartReplies()
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = conversation?.smartRepliesEnabled != false,
                                            onCheckedChange = { 
                                                viewModel.toggleSmartReplies()
                                            }
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    Divider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        },
        bottomBar = {
            Column {
                // Smart Reply Suggestions (appears above message input)
                SmartReplySuggestions(
                    replies = smartReplies,
                    isLoading = smartRepliesLoading,
                    error = smartRepliesError,
                    onReplySelected = { reply ->
                        viewModel.useSmartReply(reply)
                    },
                    onRetry = {
                        // Retry with the last message
                        messages.filter { it.senderId != currentUserId }
                            .maxByOrNull { it.timestamp }
                            ?.let { viewModel.loadSmartReplies(it) }
                    },
                    onDismiss = { viewModel.dismissSmartReplies() }
                )
                
                MessageInput(
                    messageText = messageText,
                    onMessageChange = {
                        viewModel.updateMessageText(it)
                        // Dismiss smart replies when user starts typing
                        if (it.isNotBlank() && smartReplies.isNotEmpty()) {
                            viewModel.dismissSmartReplies()
                        }
                    },
                    onSendClick = {
                        selectedImageUri?.let { uri ->
                            // Send image with caption
                            viewModel.sendImageMessage(uri, messageText.ifBlank { null })
                            selectedImageUri = null
                        } ?: run {
                            // Send text only
                            viewModel.sendMessage()
                        }
                        // Dismiss smart replies after sending
                        viewModel.dismissSmartReplies()
                    },
                    onAttachmentClick = { showImagePicker = true },
                    onMicClick = {
                        // Check and request audio permission
                        if (audioPermissionState.status.isGranted) {
                            showRecordingSheet = true
                            viewModel.startVoiceRecording()
                        } else {
                            audioPermissionState.launchPermissionRequest()
                        }
                    },
                    isUploading = uploadProgress != null,
                    selectedImageUri = selectedImageUri,
                    onClearImage = { selectedImageUri = null },
                    // Formality adjustment
                    selectedFormality = selectedFormality,
                    onFormalitySelected = { viewModel.setSelectedFormality(it) },
                    onAdjustFormalityClick = { viewModel.adjustCurrentMessageFormality() },
                    isFormalityLoading = formalityLoading
                )
            }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
                    state = listState
                ) {
                    items(messages, key = { it.id }) { message ->
                        val messageIndex = messages.indexOf(message)
                        val previousMessage = if (messageIndex > 0) messages[messageIndex - 1] else null
                        val isGroupedWithPrevious = previousMessage?.let {
                            it.senderId == message.senderId && 
                            (message.timestamp - it.timestamp) < 120000 // 2 minutes
                        } ?: false
                        
                        MessageBubble(
                            message = message,
                            isOwnMessage = message.senderId == currentUserId,
                            currentUserId = currentUserId ?: "",
                            isGroupChat = conversation?.isGroup() == true,
                            senderName = conversation?.getUserDisplayName(
                                message.senderId,
                                participantUsers[message.senderId]
                            ),
                            participantUsers = participantUsers,
                            isGroupedWithPrevious = isGroupedWithPrevious,
                            translation = translations[message.id],
                            isTranslating = message.id in translationLoading,
                            translationError = translationErrors[message.id],
                            onTranslateClick = { targetLanguage ->
                                viewModel.translateMessage(message, targetLanguage)
                            },
                            onRemoveTranslation = {
                                viewModel.removeTranslation(message.id)
                            },
                            onRetryTranslation = { targetLanguage ->
                                viewModel.retryTranslation(message, targetLanguage)
                            },
                            extractedData = extractedData[message.id],
                            onExtractClick = {
                                viewModel.extractFromMessage(message)
                            },
                            onEntityAction = { entity ->
                                com.gchat.util.EntityIntentHandler.handleEntityAction(context, entity)
                            },
                            onImageClick = { imageUrl ->
                                // Navigate to image viewer (will add this shortly)
                            },
                            // Voice message parameters
                            playbackState = playbackStates[message.id] ?: com.gchat.domain.repository.PlaybackState.Idle,
                            isTranscribing = transcriptionLoading.contains(message.id),
                            playbackSpeed = playbackSpeed,
                            onPlayPause = { messageId, audioUrl ->
                                viewModel.togglePlayPause(messageId, audioUrl)
                            },
                            onSpeedChange = { speed ->
                                viewModel.setPlaybackSpeed(speed)
                            },
                            onRequestTranscription = { messageId, audioUrl ->
                                viewModel.requestTranscription(messageId, audioUrl)
                            },
                            // Cultural context parameters
                            hasCulturalContext = viewModel.hasCulturalContext(message.id),
                            isCulturalContextLoading = viewModel.isCulturalContextLoading(message.id),
                            culturalContextError = viewModel.getCulturalContextError(message.id),
                            onCulturalContextClick = {
                                viewModel.loadCulturalContext(message)
                                selectedMessageForContext = message.id
                                showCulturalContextSheet = true
                            },
                            // Reaction parameters
                            onAddReaction = { emoji ->
                                viewModel.addReaction(message, emoji)
                            },
                            onRemoveReaction = {
                                viewModel.removeReaction(message)
                            },
                            onReactionClick = { emoji ->
                                // Show reaction viewer
                                selectedMessageForContext = message.id
                                // Handled in MessageBubble
                            }
                        )
                        Spacer(modifier = Modifier.height(if (isGroupedWithPrevious) 2.dp else 12.dp))
                    }
                    
                    // Show typing indicator at the bottom
                    if (typingIndicatorText.isNotBlank()) {
                        item {
                            TypingIndicator(text = typingIndicatorText)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
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
    
    // Nickname dialog
    if (showNicknameDialog) {
        var nicknameText by remember { 
            mutableStateOf(conversation?.getNickname(currentUserId ?: "") ?: "") 
        }
        
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
    
    // Voice recording sheet
    if (showRecordingSheet) {
        VoiceRecordingSheet(
            recordingState = recordingState,
            onCancel = {
                viewModel.cancelVoiceRecording()
                showRecordingSheet = false
            },
            onSend = {
                viewModel.stopAndSendVoiceMessage()
                showRecordingSheet = false
            }
        )
    }
    
    // Cultural Context Bottom Sheet
    if (showCulturalContextSheet && selectedMessageForContext != null) {
        val contextResult = culturalContexts[selectedMessageForContext]
        if (contextResult != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showCulturalContextSheet = false
                    selectedMessageForContext = null
                }
            ) {
                CulturalContextBottomSheet(
                    culturalContextResult = contextResult,
                    onDismiss = {
                        showCulturalContextSheet = false
                        selectedMessageForContext = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    currentUserId: String,
    isGroupChat: Boolean = false,
    senderName: String? = null,
    participantUsers: Map<String, com.gchat.domain.model.User> = emptyMap(),
    isGroupedWithPrevious: Boolean = false,
    translation: Translation? = null,
    isTranslating: Boolean = false,
    translationError: String? = null,
    onTranslateClick: (String) -> Unit = {},
    onRemoveTranslation: () -> Unit = {},
    onRetryTranslation: (String) -> Unit = {},
    extractedData: com.gchat.domain.model.ExtractedData? = null,
    onExtractClick: () -> Unit = {},
    onEntityAction: (com.gchat.domain.model.ExtractedEntity) -> Unit = {},
    onImageClick: (String) -> Unit,
    // Voice message parameters
    playbackState: com.gchat.domain.repository.PlaybackState = com.gchat.domain.repository.PlaybackState.Idle,
    isTranscribing: Boolean = false,
    playbackSpeed: Float = 1.0f,
    onPlayPause: (String, String) -> Unit = { _, _ -> },
    onSpeedChange: (Float) -> Unit = {},
    onRequestTranscription: (String, String) -> Unit = { _, _ -> }, // messageId, audioUrl
    // Cultural context parameters
    hasCulturalContext: Boolean = false,
    isCulturalContextLoading: Boolean = false,
    culturalContextError: String? = null,
    onCulturalContextClick: () -> Unit = {},
    // Reaction parameters
    onAddReaction: (String) -> Unit = {},
    onRemoveReaction: () -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showMessageActions by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var showReactionViewer by remember { mutableStateOf(false) }
    // Get users who have read this message (excluding the sender)
    val readByUsers = message.readBy.keys
        .filter { it != message.senderId }
        .mapNotNull { userId -> participantUsers[userId] }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Show sender name for group chats (only for others' messages and when not grouped)
        if (isGroupChat && !isOwnMessage && senderName != null && !isGroupedWithPrevious) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = if (isOwnMessage) 0.dp else 12.dp, bottom = 4.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
            ) {
                // Use ImageMessageBubble for image messages
                if (message.type == MessageType.IMAGE && message.mediaUrl != null) {
                    ImageMessageBubble(
                        message = message,
                        isCurrentUser = isOwnMessage,
                        onImageClick = onImageClick
                    )
                } else if (message.type == MessageType.AUDIO && message.mediaUrl != null) {
                    // Voice message bubble
                    var showVoiceMessageActions by remember { mutableStateOf(false) }
                    
                    Surface(
                        shape = if (isOwnMessage) MessageBubbleShapeSent else MessageBubbleShapeReceived,
                        color = if (isOwnMessage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                showVoiceMessageActions = true
                            }
                        )
                    ) {
                        VoiceMessageBubble(
                            audioDuration = message.audioDuration ?: 0,
                            audioWaveform = message.audioWaveform,
                            transcription = message.transcription,
                            isTranscribing = isTranscribing,
                            playbackState = playbackState,
                            onPlayPause = {
                                onPlayPause(message.id, message.mediaUrl)
                            },
                            onSeek = { position ->
                                // TODO: Implement seek if needed
                            },
                            onSpeedChange = onSpeedChange,
                            currentSpeed = playbackSpeed,
                            isOwnMessage = isOwnMessage
                        )
                    }
                    
                    // Voice message actions dialog
                    if (showVoiceMessageActions) {
                        AlertDialog(
                            onDismissRequest = { showVoiceMessageActions = false },
                            title = { Text("Voice Message") },
                            text = {
                                Column {
                                    // Transcribe option
                                    if (message.transcription == null && !isTranscribing) {
                                        TextButton(
                                            onClick = {
                                                message.mediaUrl?.let { audioUrl ->
                                                    onRequestTranscription(message.id, audioUrl)
                                                }
                                                showVoiceMessageActions = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Subtitles,
                                                contentDescription = "Transcribe"
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Transcribe Audio")
                                        }
                                    }
                                    
                                    // Show existing transcription
                                    if (message.transcription != null) {
                                        Text(
                                            text = "Transcription: ${message.transcription}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    // Show transcribing status
                                    if (isTranscribing) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Transcribing...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = { showVoiceMessageActions = false }
                                ) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                } else {
                    // Regular text message bubble with long-press for reactions
                    Surface(
                        shape = if (isOwnMessage) MessageBubbleShapeSent else MessageBubbleShapeReceived,
                        color = if (isOwnMessage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    showReactionPicker = true
                                }
                            )
                    ) {
                        Text(
                            text = message.text ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOwnMessage) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                
                // Show translation loading, error, or result
                when {
                    isTranslating -> {
                        TranslationLoading(isOwnMessage = isOwnMessage)
                    }
                    translationError != null -> {
                        TranslationError(
                            error = translationError,
                            isOwnMessage = isOwnMessage,
                            onRetry = {
                                showLanguageSelector = true
                            }
                        )
                    }
                    translation != null -> {
                        TranslationDisplay(
                            translation = translation,
                            isOwnMessage = isOwnMessage,
                            onRemove = onRemoveTranslation
                        )
                    }
                }
                
                // Show cultural context icon if available or loading
                if (hasCulturalContext || isCulturalContextLoading || culturalContextError != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .padding(start = if (isOwnMessage) 0.dp else 12.dp)
                    ) {
                        when {
                            isCulturalContextLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Loading context...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            culturalContextError != null -> {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Cultural context error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Context unavailable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            hasCulturalContext -> {
                                IconButton(
                                    onClick = onCulturalContextClick,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Cultural context available",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Cultural context",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onCulturalContextClick() }
                                )
                            }
                        }
                    }
                }
                
                // Show extracted smart chips
                extractedData?.let { data ->
                    if (data.hasEntities()) {
                        SmartChipGroup(
                            entities = data.entities,
                            onActionClick = { entity ->
                                onEntityAction(entity)
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Show reactions
                if (message.hasReactions()) {
                    ReactionsDisplay(
                        message = message,
                        currentUserId = currentUserId,
                        onReactionClick = { emoji ->
                            // Toggle user's reaction or show viewer
                            val userReaction = message.getUserReaction(currentUserId)
                            if (userReaction == emoji) {
                                // Remove reaction
                                onRemoveReaction()
                            } else {
                                // Show viewer
                                showReactionViewer = true
                            }
                        },
                        modifier = Modifier.padding(
                            start = if (isOwnMessage) 0.dp else 12.dp,
                            end = if (isOwnMessage) 12.dp else 0.dp
                        )
                    )
                }
                
                // Timestamp and read receipts OUTSIDE the bubble
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        start = if (isOwnMessage) 0.dp else 12.dp,
                        end = if (isOwnMessage) 12.dp else 0.dp,
                        top = 4.dp
                    )
                ) {
                    if (isOwnMessage) {
                        // Show read receipts for own messages (before timestamp)
                        if (isGroupChat) {
                            // Show avatar bubbles for group chats
                            if (readByUsers.isNotEmpty()) {
                                ReadByAvatars(
                                    readByUsers = readByUsers,
                                    maxVisible = 3
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        } else {
                            // Show checkmarks for 1-on-1 chats
                            ReadReceiptCheckmarks(
                                isRead = message.isReadByAny(),
                                isSent = message.status == MessageStatus.SENT || message.status == MessageStatus.READ
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
    
    // Message actions dialog (Translate or Extract)
    if (showMessageActions) {
        AlertDialog(
            onDismissRequest = { showMessageActions = false },
            title = { Text("Message Actions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose an action for this message:")
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showMessageActions = false
                            showLanguageSelector = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate")
                    }
                    Button(
                        onClick = {
                            showMessageActions = false
                            onCulturalContextClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cultural Context")
                    }
                    Button(
                        onClick = {
                            showMessageActions = false
                            onExtractClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extract Data")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMessageActions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Language selector dialog
    if (showLanguageSelector) {
        LanguageSelectorDialog(
            currentLanguage = "en", // TODO: Get from user preferences
            onLanguageSelected = { languageCode ->
                onTranslateClick(languageCode)
            },
            onDismiss = { showLanguageSelector = false }
        )
    }
    
    // Reaction picker
    if (showReactionPicker) {
        ReactionPicker(
            onEmojiSelected = { emoji ->
                onAddReaction(emoji)
            },
            onDismiss = { showReactionPicker = false }
        )
    }
    
    // Reaction viewer sheet
    if (showReactionViewer) {
        ReactionViewerSheet(
            message = message,
            participantUsers = participantUsers,
            onDismiss = { showReactionViewer = false }
        )
    }
}

@Composable
fun MessageInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onMicClick: () -> Unit = {},
    isUploading: Boolean = false,
    selectedImageUri: Uri? = null,
    onClearImage: () -> Unit = {},
    // Formality adjustment
    selectedFormality: com.gchat.domain.model.FormalityLevel = com.gchat.domain.model.FormalityLevel.NEUTRAL,
    onFormalitySelected: (com.gchat.domain.model.FormalityLevel) -> Unit = {},
    onAdjustFormalityClick: () -> Unit = {},
    isFormalityLoading: Boolean = false
) {
    val canSend = messageText.isNotBlank() || selectedImageUri != null
    val showFormalityButton = messageText.length > 10 && selectedImageUri == null
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Column {
            // Top divider
            Divider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // Image preview (if image selected)
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Remove button
                    Surface(
                        onClick = onClearImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachmentClick,
                    enabled = !isUploading && selectedImageUri == null,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Formality Selector (only when text is long enough)
                if (showFormalityButton) {
                    FormalitySelector(
                        selectedFormality = selectedFormality,
                        onFormalitySelected = onFormalitySelected,
                        onAdjustClick = onAdjustFormalityClick,
                        isLoading = isFormalityLoading,
                        enabled = !isUploading,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Text field with iOS-style pill shape
                TextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(
                            if (selectedImageUri != null) "Add a caption..." else "Message",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    enabled = !isUploading,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send or Microphone button with animation
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp)
                            .padding(bottom = 4.dp),
                        strokeWidth = 2.dp
                    )
                } else if (canSend) {
                    // Send button when there's text or image
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                        exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Surface(
                            onClick = onSendClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Microphone button when no text
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                        exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        IconButton(
                            onClick = onMicClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Record voice message",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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

@Composable
fun TypingIndicator(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Animated typing dots
                TypingDots()
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TypingDot(alpha = alpha1)
        TypingDot(alpha = alpha2)
        TypingDot(alpha = alpha3)
    }
}

@Composable
private fun TypingDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = alpha))
    )
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

