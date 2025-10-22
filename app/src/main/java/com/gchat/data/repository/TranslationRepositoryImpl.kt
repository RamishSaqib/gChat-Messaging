package com.gchat.data.repository

import com.gchat.data.local.dao.TranslationDao
import com.gchat.data.mapper.TranslationMapper
import com.gchat.data.remote.firebase.FirebaseTranslationDataSource
import com.gchat.domain.model.Translation
import com.gchat.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TranslationRepository
 * 
 * Coordinates between Firebase Cloud Functions and local cache
 */
@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val translationDao: TranslationDao,
    private val firebaseDataSource: FirebaseTranslationDataSource
) : TranslationRepository {
    
    override suspend fun translateMessage(
        messageId: String,
        text: String,
        sourceLanguage: String?,
        targetLanguage: String
    ): Result<Translation> {
        return try {
            // Call Firebase Cloud Function for translation
            val result = firebaseDataSource.translateMessage(
                messageId = messageId,
                text = text,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            )
            
            result.fold(
                onSuccess = { translation ->
                    // Cache the translation locally
                    translationDao.insert(TranslationMapper.toEntity(translation))
                    android.util.Log.d("TranslationRepo", "Translation cached: ${translation.id}")
                    Result.success(translation)
                },
                onFailure = { error ->
                    android.util.Log.e("TranslationRepo", "Translation failed", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("TranslationRepo", "Translation error", e)
            Result.failure(e)
        }
    }
    
    override fun getCachedTranslation(
        messageId: String,
        targetLanguage: String
    ): Flow<Translation?> {
        return translationDao
            .getTranslation(messageId, targetLanguage)
            .map { entity ->
                entity?.let { TranslationMapper.toDomain(it) }
            }
    }
    
    override suspend fun detectLanguage(text: String): Result<String> {
        return firebaseDataSource.detectLanguage(text)
    }
    
    override suspend fun clearCache(): Result<Unit> {
        return try {
            translationDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clean up old translations (call periodically)
     */
    suspend fun cleanupOldTranslations() {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
            translationDao.deleteOldTranslations(thirtyDaysAgo)
            android.util.Log.d("TranslationRepo", "Cleaned up old translations")
        } catch (e: Exception) {
            android.util.Log.e("TranslationRepo", "Cleanup failed", e)
        }
    }
}

