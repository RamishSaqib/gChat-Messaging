package com.gchat.domain.model

/**
 * Represents a cultural context explanation for an idiom, slang, or cultural reference.
 * 
 * @property phrase The exact phrase or expression from the message
 * @property literalTranslation Word-for-word translation (if applicable), null for slang
 * @property actualMeaning The actual meaning or definition
 * @property culturalContext Cultural background, usage notes, and context
 * @property examples Optional list of example usages
 */
data class CulturalContext(
    val phrase: String,
    val literalTranslation: String?,
    val actualMeaning: String,
    val culturalContext: String,
    val examples: List<String> = emptyList()
)

/**
 * Contains all cultural context information for a specific message.
 * 
 * @property messageId The ID of the message being analyzed
 * @property contexts List of cultural contexts found in the message
 * @property language The language of the analyzed text
 * @property cached Whether this result was retrieved from cache
 */
data class CulturalContextResult(
    val messageId: String,
    val contexts: List<CulturalContext>,
    val language: String,
    val cached: Boolean = false
)

