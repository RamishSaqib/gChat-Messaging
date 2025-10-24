package com.gchat.domain.model

/**
 * Per-user reaction notification for conversation preview
 * Allows showing reaction previews only to the message owner
 */
data class ReactionNotification(
    val text: String,
    val timestamp: Long,
    val messageId: String,
    val reactorId: String
)

