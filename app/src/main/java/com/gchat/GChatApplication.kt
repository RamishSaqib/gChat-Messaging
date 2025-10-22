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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private var heartbeatJob: Job? = null

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
                        
                        // Start heartbeat to keep lastSeen fresh
                        startHeartbeat(userId)
                    }
                }
            }
            
            override fun onStop(owner: LifecycleOwner) {
                // App went to background
                applicationScope.launch {
                    // Cancel heartbeat
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    
                    val userId = authRepository.getCurrentUserId()
                    if (userId != null) {
                        userRepository.updateOnlineStatus(userId, false)
                    }
                }
            }
        })
    }
    
    /**
     * Start periodic heartbeat to update lastSeen timestamp
     * This ensures users appear offline after 2 minutes of force-kill
     */
    private fun startHeartbeat(userId: String) {
        heartbeatJob?.cancel() // Cancel any existing heartbeat
        heartbeatJob = applicationScope.launch {
            while (true) {
                delay(60_000) // Update every 60 seconds
                try {
                    // Just update lastSeen timestamp (isOnline stays true)
                    userRepository.updateOnlineStatus(userId, true)
                    android.util.Log.d("GChatApp", "Heartbeat: Updated lastSeen for user $userId")
                } catch (e: Exception) {
                    android.util.Log.e("GChatApp", "Heartbeat failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get current FCM token and update in Firestore
     */
    private suspend fun updateFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            android.util.Log.d("GChatApp", "FCM Token retrieved: ${token.take(20)}...")
            userRepository.updateFcmToken(userId, token).fold(
                onSuccess = {
                    android.util.Log.d("GChatApp", "FCM Token updated successfully in Firestore")
                },
                onFailure = { error ->
                    android.util.Log.e("GChatApp", "Failed to save FCM token to Firestore: ${error.message}")
                }
            )
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("GChatApp", "Failed to retrieve FCM token: ${e.message}", e)
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

