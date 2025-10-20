package com.gchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for gChat
 * 
 * Initializes:
 * - Hilt dependency injection
 * - Firebase services
 * - Notification channels
 */
@HiltAndroidApp
class GChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Messages channel
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableVibration(true)
                enableLights(true)
            }
            
            // Typing indicators channel (low priority)
            val typingChannel = NotificationChannel(
                CHANNEL_ID_TYPING,
                "Typing Indicators",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Typing indicator notifications"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(messagesChannel, typingChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_ID_MESSAGES = "messages"
        const val CHANNEL_ID_TYPING = "typing"
    }
}

