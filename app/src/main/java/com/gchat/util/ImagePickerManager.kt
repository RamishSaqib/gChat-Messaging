package com.gchat.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manager for handling image selection from camera or gallery
 */
class ImagePickerManager(
    private val context: Context,
    private val onImageSelected: (Uri) -> Unit
) {
    
    private var tempPhotoUri: Uri? = null
    
    /**
     * Create a temporary file URI for camera capture
     */
    fun createTempImageUri(): Uri {
        val tempFile = File.createTempFile(
            "camera_image_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        ).apply {
            deleteOnExit()
        }
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        ).also {
            tempPhotoUri = it
        }
    }
    
    /**
     * Handle the result from camera capture
     */
    fun handleCameraResult(success: Boolean) {
        if (success && tempPhotoUri != null) {
            onImageSelected(tempPhotoUri!!)
        }
    }
    
    /**
     * Handle the result from gallery selection
     */
    fun handleGalleryResult(uri: Uri?) {
        uri?.let { onImageSelected(it) }
    }
}

/**
 * Composable function to create image picker launchers
 */
@Composable
fun rememberImagePickerLaunchers(
    onImageSelected: (Uri) -> Unit
): ImagePickerLaunchers {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imagePickerManager = remember { ImagePickerManager(context, onImageSelected) }
    
    // Gallery launcher (Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imagePickerManager.handleGalleryResult(uri)
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        imagePickerManager.handleCameraResult(success)
    }
    
    return remember(imagePickerManager, galleryLauncher, cameraLauncher) {
        ImagePickerLaunchers(
            imagePickerManager = imagePickerManager,
            galleryLauncher = galleryLauncher,
            cameraLauncher = cameraLauncher
        )
    }
}

/**
 * Data class holding the image picker launchers
 */
data class ImagePickerLaunchers(
    val imagePickerManager: ImagePickerManager,
    val galleryLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>
) {
    fun launchGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    
    fun launchCamera() {
        val uri = imagePickerManager.createTempImageUri()
        cameraLauncher.launch(uri)
    }
}

