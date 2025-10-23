package com.gchat.presentation.groupinfo

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gchat.domain.model.User
import com.gchat.presentation.components.ProfilePicture
import com.gchat.util.rememberImagePickerLaunchers
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GroupInfoScreen(
    onNavigateBack: () -> Unit,
    onAddMembers: () -> Unit,
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val conversation by viewModel.conversation.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val isEditingName by viewModel.isEditingName.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<User?>(null) }
    var showMemberOptions by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val imagePickerLaunchers = rememberImagePickerLaunchers(
        onImageSelected = { uri ->
            viewModel.selectGroupIcon(uri)
            showImagePicker = false
        }
    )
    
    GroupInfoContent(
        groupName = groupName,
        isEditingName = isEditingName,
        uploadProgress = uploadProgress,
        conversation = conversation,
        participants = participants,
        currentUserId = currentUserId,
        isAdmin = isAdmin,
        error = error,
        success = success,
        showMenu = showMenu,
        showImagePicker = showImagePicker,
        selectedMember = selectedMember,
        showMemberOptions = showMemberOptions,
        showLeaveConfirmation = showLeaveConfirmation,
        showNicknameDialog = showNicknameDialog,
        cameraPermissionState = cameraPermissionState,
        imagePickerLaunchers = imagePickerLaunchers,
        onNavigateBack = onNavigateBack,
        onAddMembers = onAddMembers,
        onShowMenuChange = { showMenu = it },
        onShowImagePickerChange = { showImagePicker = it },
        onSelectedMemberChange = { selectedMember = it },
        onShowMemberOptionsChange = { showMemberOptions = it },
        onShowLeaveConfirmationChange = { showLeaveConfirmation = it },
        onShowNicknameDialogChange = { showNicknameDialog = it },
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun GroupInfoContent(
    groupName: String,
    isEditingName: Boolean,
    uploadProgress: Float?,
    conversation: com.gchat.domain.model.Conversation?,
    participants: List<User>,
    currentUserId: String?,
    isAdmin: Boolean,
    error: String?,
    success: String?,
    showMenu: Boolean,
    showImagePicker: Boolean,
    selectedMember: User?,
    showMemberOptions: Boolean,
    showLeaveConfirmation: Boolean,
    showNicknameDialog: Boolean,
    cameraPermissionState: com.google.accompanist.permissions.PermissionState,
    imagePickerLaunchers: com.gchat.util.ImagePickerLaunchers,
    onNavigateBack: () -> Unit,
    onAddMembers: () -> Unit,
    onShowMenuChange: (Boolean) -> Unit,
    onShowImagePickerChange: (Boolean) -> Unit,
    onSelectedMemberChange: (User?) -> Unit,
    onShowMemberOptionsChange: (Boolean) -> Unit,
    onShowLeaveConfirmationChange: (Boolean) -> Unit,
    onShowNicknameDialogChange: (Boolean) -> Unit,
    viewModel: GroupInfoViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(success) {
        success?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GroupInfoTopBar(
                onNavigateBack = onNavigateBack,
                showMenu = showMenu,
                onShowMenuChange = onShowMenuChange,
                onShowNicknameDialog = { onShowNicknameDialogChange(true) },
                onShowLeaveConfirmation = { onShowLeaveConfirmationChange(true) },
                conversation = conversation,
                onToggleAutoTranslate = { viewModel.toggleAutoTranslate() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                GroupIconSection(
                    uploadProgress = uploadProgress,
                    conversation = conversation,
                    groupName = groupName,
                    onShowImagePicker = { onShowImagePickerChange(true) }
                )
            }
            
            item {
                GroupNameSection(
                    groupName = groupName,
                    isEditingName = isEditingName,
                    participantsCount = participants.size,
                    onUpdateGroupName = viewModel::updateGroupName,
                    onSaveGroupName = viewModel::saveGroupName,
                    onCancelEditingName = viewModel::cancelEditingName,
                    onStartEditingName = viewModel::startEditingName
                )
            }
            
            item {
                MembersSectionHeader(
                    isAdmin = isAdmin,
                    onAddMembers = onAddMembers
                )
            }
            
            items(participants) { member ->
                MemberItem(
                    member = member,
                    displayName = conversation?.getUserDisplayName(member.id, member) ?: member.displayName,
                    isAdmin = conversation?.isAdmin(member.id) == true,
                    isCurrentUser = member.id == currentUserId,
                    canManage = isAdmin && member.id != currentUserId,
                    onClick = {
                        if (isAdmin && member.id != currentUserId) {
                            onSelectedMemberChange(member)
                            onShowMemberOptionsChange(true)
                        }
                    }
                )
            }
        }
    }
    
    // Image Picker Bottom Sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { onShowImagePickerChange(false) }
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
                onDismiss = { onShowImagePickerChange(false) }
                )
        }
    }
    
    // Member Options Dialog
    if (showMemberOptions && selectedMember != null) {
        AlertDialog(
            onDismissRequest = { onShowMemberOptionsChange(false) },
            title = { Text(selectedMember!!.displayName) },
            text = {
                Column {
                    if (conversation?.isAdmin(selectedMember!!.id) != true) {
                        Text(
                            text = "Promote to Admin",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.promoteToAdmin(selectedMember!!.id)
                                    onShowMemberOptionsChange(false)
                                }
                                .padding(16.dp)
                        )
                    }
                    Text(
                        text = "Remove from Group",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.removeMember(selectedMember!!.id)
                                onShowMemberOptionsChange(false)
                            }
                            .padding(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onShowMemberOptionsChange(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Leave Group Confirmation
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { onShowLeaveConfirmationChange(false) },
            title = { Text("Leave Group?") },
            text = { Text("Are you sure you want to leave this group?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onShowLeaveConfirmationChange(false)
                        viewModel.leaveGroup {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowLeaveConfirmationChange(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Nickname Dialog
    if (showNicknameDialog) {
        var nicknameText by remember { mutableStateOf(viewModel.getCurrentNickname() ?: "") }
        
        AlertDialog(
            onDismissRequest = { onShowNicknameDialogChange(false) },
            title = { Text("Change My Nickname") },
            text = {
                Column {
                    Text(
                        text = "Set a custom nickname that only shows in this group.",
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
                    onShowNicknameDialogChange(false)
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                if (nicknameText.isNotBlank()) {
                    TextButton(onClick = {
                        viewModel.setNickname(null)
                        onShowNicknameDialogChange(false)
                    }) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = { onShowNicknameDialogChange(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupInfoTopBar(
    onNavigateBack: () -> Unit,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onShowNicknameDialog: () -> Unit,
    onShowLeaveConfirmation: () -> Unit,
    conversation: com.gchat.domain.model.Conversation?,
    onToggleAutoTranslate: () -> Unit
) {
    TopAppBar(
        title = { Text("Group Info") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = { onShowMenuChange(true) }) {
                Icon(Icons.Default.MoreVert, "Menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onShowMenuChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Change My Nickname") },
                    onClick = {
                        onShowMenuChange(false)
                        onShowNicknameDialog()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { 
                        Text(
                            if (conversation?.autoTranslateEnabled == true) 
                                "Disable Auto-translate" 
                            else 
                                "Enable Auto-translate"
                        )
                    },
                    onClick = {
                        onShowMenuChange(false)
                        onToggleAutoTranslate()
                    },
                    leadingIcon = { 
                        Icon(
                            if (conversation?.autoTranslateEnabled == true)
                                Icons.Default.Check
                            else
                                Icons.Default.Add,
                            null
                        ) 
                    }
                )
                DropdownMenuItem(
                    text = { Text("Leave Group") },
                    onClick = {
                        onShowMenuChange(false)
                        onShowLeaveConfirmation()
                    },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, null) }
                )
            }
        }
    )
}

@Composable
private fun GroupIconSection(
    uploadProgress: Float?,
    conversation: com.gchat.domain.model.Conversation?,
    groupName: String,
    onShowImagePicker: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { onShowImagePicker() },
            contentAlignment = Alignment.BottomEnd
        ) {
            if (uploadProgress != null) {
                CircularProgressIndicator(
                    progress = uploadProgress!!,
                    modifier = Modifier.size(120.dp)
                )
            } else {
                ProfilePicture(
                    url = conversation?.iconUrl,
                    displayName = groupName,
                    size = 120.dp,
                    showOnlineIndicator = false
                )
            }
            
            // Edit icon
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change icon",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun GroupNameSection(
    groupName: String,
    isEditingName: Boolean,
    participantsCount: Int,
    onUpdateGroupName: (String) -> Unit,
    onSaveGroupName: () -> Unit,
    onCancelEditingName: () -> Unit,
    onStartEditingName: () -> Unit
) {
    if (isEditingName) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = onUpdateGroupName,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = onSaveGroupName) {
                Icon(Icons.Default.Check, "Save")
            }
            IconButton(onClick = onCancelEditingName) {
                Icon(Icons.Default.Close, "Cancel")
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartEditingName() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Edit, "Edit name", modifier = Modifier.size(20.dp))
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "$participantsCount members",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    Divider()
}

@Composable
private fun MembersSectionHeader(
    isAdmin: Boolean,
    onAddMembers: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Members",
            style = MaterialTheme.typography.titleMedium
        )
        if (isAdmin) {
            TextButton(onClick = onAddMembers) {
                Icon(Icons.Default.Add, "Add members", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

@Composable
fun MemberItem(
    member: User,
    displayName: String,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    canManage: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName)
                if (isCurrentUser) {
                    Text(
                        text = " (You)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        supportingContent = {
            if (isAdmin) {
                Text(
                    text = "(Admin)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        leadingContent = {
            ProfilePicture(
                url = member.profilePictureUrl,
                displayName = member.displayName,
                size = 40.dp,
                showOnlineIndicator = true,
                isOnline = member.isActuallyOnline()
            )
        },
        modifier = Modifier.clickable(enabled = canManage, onClick = onClick)
    )
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
            text = "Change Group Icon",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        ListItem(
            headlineContent = { Text("Camera") },
            supportingContent = { Text("Take a new photo") },
            leadingContent = { Icon(Icons.Default.CameraAlt, null) },
            modifier = Modifier.clickable { onCameraClick() }
        )
        
        ListItem(
            headlineContent = { Text("Gallery") },
            supportingContent = { Text("Choose from gallery") },
            leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
            modifier = Modifier.clickable { onGalleryClick() }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

