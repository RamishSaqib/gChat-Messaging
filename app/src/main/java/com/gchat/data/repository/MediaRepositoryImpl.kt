package com.gchat.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.gchat.domain.repository.MediaRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MediaRepository using Firebase Storage
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : MediaRepository {
    
    private val storageRef = storage.reference
    
    override suspend fun uploadImage(uri: Uri, path: String): Result<String> {
        return try {
            // Compress the image before uploading
            val compressedData = compressImage(uri)
            
            // Upload to Firebase Storage
            val imageRef = storageRef.child(path)
            imageRef.putBytes(compressedData).await()
            
            // Get download URL
            val downloadUrl = imageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun uploadAudio(file: File, path: String): Result<String> {
        return try {
            // Upload audio file to Firebase Storage with explicit metadata
            val audioRef = storageRef.child(path)
            
            // Set metadata to ensure correct MIME type
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("audio/mp4") // M4A files use audio/mp4 MIME type
                .build()
            
            audioRef.putFile(Uri.fromFile(file), metadata).await()
            
            // Get download URL
            val downloadUrl = audioRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteImage(url: String): Result<Unit> {
        return try {
            // Extract storage path from URL and delete
            val imageRef = storage.getReferenceFromUrl(url)
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAudio(url: String): Result<Unit> {
        return try {
            // Extract storage path from URL and delete
            val audioRef = storage.getReferenceFromUrl(url)
            audioRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCachedImageUri(url: String): Uri? {
        return try {
            // Coil handles caching automatically, so we just return the URL
            // This method is for future enhancements if needed
            Uri.parse(url)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Compress image to reduce file size before upload
     * Target: < 500KB while maintaining quality
     */
    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI")
        
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        // Calculate scaling factor to keep under reasonable size
        val maxDimension = 1920 // Max width or height
        val scale = if (bitmap.width > bitmap.height) {
            if (bitmap.width > maxDimension) {
                maxDimension.toFloat() / bitmap.width
            } else 1f
        } else {
            if (bitmap.height > maxDimension) {
                maxDimension.toFloat() / bitmap.height
            } else 1f
        }
        
        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        
        // Compress to JPEG with quality adjustment
        val outputStream = ByteArrayOutputStream()
        var quality = 90
        
        do {
            outputStream.reset()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > 500 * 1024 && quality > 20) // Target 500KB
        
        scaledBitmap.recycle()
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        
        return outputStream.toByteArray()
    }
}

