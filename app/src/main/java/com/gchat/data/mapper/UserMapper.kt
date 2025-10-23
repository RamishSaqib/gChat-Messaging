package com.gchat.data.mapper

import com.gchat.data.local.entity.UserEntity
import com.gchat.domain.model.User
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Maps User between different representations
 */
object UserMapper {
    
    fun toDomain(entity: UserEntity): User {
        return User(
            id = entity.id,
            displayName = entity.displayName,
            email = entity.email,
            phoneNumber = entity.phoneNumber,
            profilePictureUrl = entity.profilePictureUrl,
            preferredLanguage = entity.preferredLanguage,
            autoTranslateEnabled = entity.autoTranslateEnabled,
            isOnline = entity.isOnline,
            lastSeen = entity.lastSeen
        )
    }
    
    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.id,
            displayName = domain.displayName,
            email = domain.email,
            phoneNumber = domain.phoneNumber,
            profilePictureUrl = domain.profilePictureUrl,
            preferredLanguage = domain.preferredLanguage,
            autoTranslateEnabled = domain.autoTranslateEnabled,
            isOnline = domain.isOnline,
            lastSeen = domain.lastSeen
        )
    }
    
    fun fromFirestore(document: DocumentSnapshot): User? {
        return try {
            User(
                id = document.id,
                displayName = document.getString("displayName") ?: return null,
                email = document.getString("email"),
                phoneNumber = document.getString("phoneNumber"),
                profilePictureUrl = document.getString("profilePictureUrl"),
                preferredLanguage = document.getString("preferredLanguage") ?: "en",
                autoTranslateEnabled = document.getBoolean("autoTranslateEnabled") ?: false,
                isOnline = document.getBoolean("isOnline") ?: false,
                lastSeen = document.getLong("lastSeen") ?: 0L,
                fcmToken = document.getString("fcmToken"),
                createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun toFirestore(user: User): Map<String, Any?> {
        return mapOf(
            "displayName" to user.displayName,
            "email" to user.email,
            "phoneNumber" to user.phoneNumber,
            "profilePictureUrl" to user.profilePictureUrl,
            "preferredLanguage" to user.preferredLanguage,
            "autoTranslateEnabled" to user.autoTranslateEnabled,
            "isOnline" to user.isOnline,
            "lastSeen" to user.lastSeen,
            "fcmToken" to user.fcmToken,
            "createdAt" to user.createdAt
        )
    }
}

