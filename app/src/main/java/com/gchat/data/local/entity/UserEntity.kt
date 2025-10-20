package com.gchat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for User
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["id"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val preferredLanguage: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

