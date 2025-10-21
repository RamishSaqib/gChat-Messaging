package com.gchat.presentation.profile

import android.Manifest
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.gchat.presentation.components.ProfilePicture
import com.gchat.utils.rememberImagePickerLaunchers
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showImagePicker by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    val imagePickerLaunchers = rememberImagePickerLaunchers(
        onImageSelected = { uri ->
            viewModel.selectProfilePicture(uri)
            showImagePicker = false
        }
    )
    
    // Navigate back on successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }
    
    // Show error as snackbar
    val snackbarHostState = remember { SnackbarHostState() }
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
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = !isSaving && displayName.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
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
            
            // Profile Picture
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (uploadProgress != null) {
                    CircularProgressIndicator(
                        progress = uploadProgress!!,
                        modifier = Modifier.size(120.dp)
                    )
                } else {
                    ProfilePicture(
                        url = profilePictureUrl,
                        displayName = displayName.ifBlank { "User" },
                        size = 120.dp,
                        showOnlineIndicator = false
                    )
                }
                
                // Edit icon button
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { showImagePicker = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change profile picture",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Display Name Field
            OutlinedTextField(
                value = displayName,
                onValueChange = { viewModel.updateDisplayName(it) },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email (read-only)
            OutlinedTextField(
                value = currentUser?.email ?: "",
                onValueChange = { },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
    
    // Image Picker Bottom Sheet
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
            text = "Change Profile Picture",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Camera option
        ListItem(
            headlineContent = { Text("Camera") },
            supportingContent = { Text("Take a new photo") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera"
                )
            },
            modifier = Modifier.clickable {
                onCameraClick()
                onDismiss()
            }
        )
        
        // Gallery option
        ListItem(
            headlineContent = { Text("Gallery") },
            supportingContent = { Text("Choose from gallery") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery"
                )
            },
            modifier = Modifier.clickable {
                onGalleryClick()
                onDismiss()
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

