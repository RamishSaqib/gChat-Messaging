package com.gchat.data.mapper

import com.gchat.data.local.entity.TranslationEntity
import com.gchat.domain.model.Translation

/**
 * Maps Translation between domain and data layers
 */
object TranslationMapper {
    
    fun toDomain(entity: TranslationEntity): Translation {
        return Translation(
            id = entity.id,
            messageId = entity.messageId,
            originalText = entity.originalText,
            translatedText = entity.translatedText,
            sourceLanguage = entity.sourceLanguage,
            targetLanguage = entity.targetLanguage,
            timestamp = entity.timestamp,
            cached = true // From local database
        )
    }
    
    fun toEntity(domain: Translation): TranslationEntity {
        return TranslationEntity(
            id = domain.id,
            messageId = domain.messageId,
            originalText = domain.originalText,
            translatedText = domain.translatedText,
            sourceLanguage = domain.sourceLanguage,
            targetLanguage = domain.targetLanguage,
            timestamp = domain.timestamp
        )
    }
}

