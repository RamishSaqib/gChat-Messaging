package com.gchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gchat.data.local.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Translation cache operations
 */
@Dao
interface TranslationDao {
    
    /**
     * Insert or update translation cache
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(translation: TranslationEntity)
    
    /**
     * Get cached translation for a message in specific language
     */
    @Query("SELECT * FROM translations WHERE messageId = :messageId AND targetLanguage = :targetLanguage LIMIT 1")
    fun getTranslation(messageId: String, targetLanguage: String): Flow<TranslationEntity?>
    
    /**
     * Get all translations for a message
     */
    @Query("SELECT * FROM translations WHERE messageId = :messageId")
    fun getTranslationsForMessage(messageId: String): Flow<List<TranslationEntity>>
    
    /**
     * Delete all translations (for cleanup)
     */
    @Query("DELETE FROM translations")
    suspend fun deleteAll()
    
    /**
     * Delete old translations (older than 30 days)
     */
    @Query("DELETE FROM translations WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldTranslations(cutoffTimestamp: Long)
}

