package com.gchat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for Translation cache
 */
@Entity(
    tableName = "translations",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["messageId", "targetLanguage"])
    ]
)
data class TranslationEntity(
    @PrimaryKey val id: String, // messageId_targetLanguage
    val messageId: String,
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long
)

