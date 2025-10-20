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
    companion object {
        fun empty() = User(
            id = "",
            displayName = "",
            email = null,
            phoneNumber = null
        )
    }
}

