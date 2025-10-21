package com.gchat.data.local.dao

import androidx.room.*
import com.gchat.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Conversation entities
 */
@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAllConversations(): List<ConversationEntity>
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    fun getConversationByIdFlow(conversationId: String): Flow<ConversationEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)
    
    @Update
    suspend fun update(conversation: ConversationEntity)
    
    @Query("""
        UPDATE conversations 
        SET lastMessageId = :messageId, 
            lastMessageText = :messageText,
            lastMessageTimestamp = :timestamp,
            lastMessageSenderId = :senderId,
            updatedAt = :timestamp
        WHERE id = :conversationId
    """)
    suspend fun updateLastMessage(
        conversationId: String,
        messageId: String,
        messageText: String?,
        senderId: String,
        timestamp: Long
    )
    
    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :conversationId")
    suspend fun updateUnreadCount(conversationId: String, count: Int)
    
    @Query("UPDATE conversations SET autoTranslateEnabled = :enabled WHERE id = :conversationId")
    suspend fun updateAutoTranslate(conversationId: String, enabled: Boolean)
    
    @Query("UPDATE conversations SET name = :name, updatedAt = :timestamp WHERE id = :conversationId")
    suspend fun updateGroupName(conversationId: String, name: String, timestamp: Long)
    
    @Query("UPDATE conversations SET iconUrl = :iconUrl, updatedAt = :timestamp WHERE id = :conversationId")
    suspend fun updateGroupIcon(conversationId: String, iconUrl: String?, timestamp: Long)
    
    @Delete
    suspend fun delete(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

