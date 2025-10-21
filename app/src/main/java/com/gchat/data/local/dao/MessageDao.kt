package com.gchat.data.local.dao

import androidx.room.*
import com.gchat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Message entities
 */
@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getMessages(conversationId: String, limit: Int = 50): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: String): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)
    
    @Update
    suspend fun update(message: MessageEntity)
    
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: String)
    
    @Query("UPDATE messages SET readBy = :readBy WHERE id = :messageId")
    suspend fun updateReadBy(messageId: String, readBy: String)
    
    @Query("UPDATE messages SET translatedText = :translatedText, translationSourceLang = :sourceLang, translationTargetLang = :targetLang WHERE id = :messageId")
    suspend fun updateTranslation(
        messageId: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String
    )
    
    @Delete
    suspend fun delete(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)
    
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}

