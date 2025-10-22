package com.gchat.domain.model

/**
 * User domain model
 * 
 * Represents a user in the gChat system
 */
data class User(
    val id: String,
    val displayName: String,
    val email: String?,
    val phoneNumber: String?,
    val profilePictureUrl: String? = null,
    val preferredLanguage: String = "en",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Determines if user is actually online based on both isOnline flag and lastSeen timestamp.
     * Users are only considered online if:
     * 1. isOnline = true AND
     * 2. lastSeen is within the last 2 minutes
     * 
     * This prevents stale "online" status when apps are force-killed without proper cleanup.
     */
    fun isActuallyOnline(): Boolean {
        if (!isOnline) return false
        val currentTime = System.currentTimeMillis()
        val twoMinutesAgo = currentTime - (2 * 60 * 1000)
        return lastSeen > twoMinutesAgo
    }
    
    companion object {
        fun empty() = User(
            id = "",
            displayName = "",
            email = null,
            phoneNumber = null
        )
    }
}

