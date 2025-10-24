package com.gchat.domain.model

/**
 * Reaction data model for viewer display
 * 
 * Represents a single reaction with user information for display in the reaction viewer sheet
 */
data class Reaction(
    val emoji: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

