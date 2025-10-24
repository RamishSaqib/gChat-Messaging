package com.gchat.domain.model

import kotlinx.serialization.Serializable

/**
 * Per-user reaction notification for conversation preview
 * Allows showing reaction previews only to the message owner
 */
@Serializable
data class ReactionNotification(
    val text: String,
    val timestamp: Long,
    val messageId: String,
    val reactorId: String
)

