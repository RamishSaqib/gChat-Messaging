package com.gchat.domain.repository

import android.net.Uri

/**
 * Repository interface for media operations (images, files)
 */
interface MediaRepository {
    /**
     * Upload an image to Firebase Storage
     * @param uri Local URI of the image
     * @param path Storage path (e.g., "profile_pictures/{userId}.jpg")
     * @return Result with download URL or error
     */
    suspend fun uploadImage(uri: Uri, path: String): Result<String>
    
    /**
     * Delete an image from Firebase Storage
     * @param url Download URL of the image to delete
     */
    suspend fun deleteImage(url: String): Result<Unit>
    
    /**
     * Get a cached local URI for a remote image URL
     * @param url Remote image URL
     * @return Local cached URI or null
     */
    suspend fun getCachedImageUri(url: String): Uri?
}

