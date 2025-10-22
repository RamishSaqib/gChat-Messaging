package com.gchat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gchat.data.local.dao.ConversationDao
import com.gchat.data.local.dao.MessageDao
import com.gchat.data.local.dao.UserDao
import com.gchat.data.local.entity.ConversationEntity
import com.gchat.data.local.entity.MessageEntity
import com.gchat.data.local.entity.UserEntity

/**
 * Room Database for gChat
 * 
 * Local storage for offline-first architecture
 */
@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 7, // v7: Changed deletedBy to deletedAt with timestamps for fresh message history
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    
    companion object {
        const val DATABASE_NAME = "gchat_db"
    }
}

