package com.gchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.gchat.domain.repository.AuthRepository
import com.gchat.domain.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Application class for gChat
 * 
 * Initializes:
 * - Hilt dependency injection
 * - Firebase services
 * - Notification channels
 * - Online presence detection
 */
@HiltAndroidApp
class GChatApplication : Application() {

    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var userRepository: UserRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Create notification channels
        createNotificationChannels()
        
        // Setup presence detection
        setupPresenceDetection()
    }
    
    private fun setupPresenceDetection() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // App came to foreground
                applicationScope.launch {
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        // Update online status
                        userRepository.updateOnlineStatus(userId, true)
                        
                        // Update FCM token
                        updateFcmToken(userId)
                    }
                }
            }
            
            override fun onStop(owner: LifecycleOwner) {
                // App went to background
                applicationScope.launch {
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        userRepository.updateOnlineStatus(userId, false)
                    }
                }
            }
        })
    }
    
    /**
     * Get current FCM token and update in Firestore
     */
    private suspend fun updateFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepository.updateFcmToken(userId, token)
        } catch (e: Exception) {
            // Log error but don't crash
            println("Failed to update FCM token: ${e.message}")
        }
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

