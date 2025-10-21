package com.gchat.utils

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gchat.GChatApplication
import com.gchat.R
import com.gchat.domain.repository.UserRepository
import com.gchat.presentation.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service
 * 
 * Handles:
 * - Push notifications for new messages
 * - FCM token registration and updates
 */
@AndroidEntryPoint
class MessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var auth: FirebaseAuth
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        android.util.Log.d("MessagingService", "New FCM token received: ${token.take(20)}...")
        
        // Update FCM token in Firestore using repository
        val userId = auth.currentUser?.uid ?: run {
            android.util.Log.w("MessagingService", "No user logged in, cannot save FCM token")
            return
        }
        
        android.util.Log.d("MessagingService", "Saving FCM token for user: $userId")
        
        serviceScope.launch {
            userRepository.updateFcmToken(userId, token)
                .onSuccess {
                    android.util.Log.d("MessagingService", "FCM token saved successfully")
                }
                .onFailure { e ->
                    android.util.Log.e("MessagingService", "Failed to update FCM token: ${e.message}", e)
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val data = message.data
        
        when (data["type"]) {
            "NEW_MESSAGE" -> {
                val conversationId = data["conversationId"] ?: return
                val senderId = data["senderId"] ?: return
                val senderName = data["senderName"] ?: "Someone"
                val messageText = data["messageText"] ?: message.notification?.body ?: "New message"
                val isGroupChat = data["isGroup"]?.toBoolean() ?: false
                val groupName = data["groupName"]
                
                showMessageNotification(conversationId, senderId, senderName, messageText, isGroupChat, groupName)
            }
            "TYPING" -> {
                // Handle typing indicator if needed
                // Usually not shown as notification, just updates UI if app is open
            }
        }
    }

    private fun showMessageNotification(
        conversationId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        isGroupChat: Boolean,
        groupName: String?
    ) {
        // Create intent to open conversation
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("conversationId", conversationId)
            putExtra("openChat", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification title based on chat type
        val title = if (isGroupChat) {
            groupName ?: "Group Chat"
        } else {
            senderName
        }
        
        // For group chats, prepend sender name to message
        val displayText = if (isGroupChat) {
            "$senderName: $messageText"
        } else {
            messageText
        }
        
        // Build notification
        val notification = NotificationCompat.Builder(this, GChatApplication.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using launcher icon for now
            .setContentTitle(title)
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup("MESSAGES")
            .build()
        
        // Show notification
        NotificationManagerCompat.from(this)
            .notify(conversationId.hashCode(), notification)
    }
}

