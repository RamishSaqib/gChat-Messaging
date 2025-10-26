package com.gchat.utils

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
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
        
        android.util.Log.d("MessagingService", "Message received at ${System.currentTimeMillis()}")
        
        val data = message.data
        
        when (data["type"]) {
            "NEW_MESSAGE" -> {
                val conversationId = data["conversationId"] ?: return
                val senderId = data["senderId"] ?: return
                val senderName = data["senderName"] ?: "Someone"
                val messageText = data["messageText"] ?: message.notification?.body ?: "New message"
                val isGroupChat = data["isGroup"]?.toBoolean() ?: false
                val groupName = data["groupName"]
                
                // Check if user is currently viewing this conversation
                val currentConversationId = (application as? GChatApplication)?.currentConversationId
                
                if (conversationId != currentConversationId) {
                    // User is NOT in this chat - show notification
                    android.util.Log.d("MessagingService", "User in different chat, showing notification")
                    showMessageNotification(conversationId, senderId, senderName, messageText, isGroupChat, groupName)
                } else {
                    android.util.Log.d("MessagingService", "User is in this chat, suppressing notification")
                }
            }
            "REACTION" -> {
                val conversationId = data["conversationId"] ?: return
                val reactorId = data["reactorId"] ?: return
                val reactorName = data["reactorName"] ?: "Someone"
                val emoji = data["emoji"] ?: "❤️"
                val messageText = data["messageText"] ?: ""
                
                android.util.Log.d("MessagingService", "Received REACTION notification: conversationId=$conversationId, reactorId=$reactorId, reactorName=$reactorName, emoji=$emoji")
                
                // Check if user is currently viewing this conversation
                val currentConversationId = (application as? GChatApplication)?.currentConversationId
                android.util.Log.d("MessagingService", "Current conversation: $currentConversationId, notification for: $conversationId")
                
                if (conversationId != currentConversationId) {
                    // User is NOT in this chat - show notification
                    android.util.Log.d("MessagingService", "User in different chat, showing reaction notification")
                    showReactionNotification(conversationId, reactorId, reactorName, emoji, messageText)
                } else {
                    android.util.Log.d("MessagingService", "User is in this chat, suppressing reaction notification")
                }
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
        // Don't show notification for own messages
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == senderId) {
            android.util.Log.d("MessagingService", "Skipping notification for own message")
            return
        }
        
        // Create intent to open conversation
        // CRITICAL: Use explicit component and FLAG_ACTIVITY_NEW_TASK for killed state
        val intent = Intent(this, MainActivity::class.java).apply {
            // Use explicit component to ensure Android launches MainActivity
            component = android.content.ComponentName(this@MessagingService, MainActivity::class.java)
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("gchat://conversation/$conversationId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId)
            putExtra("openChat", true)
        }
        
        android.util.Log.d("MessagingService", "Intent component: ${intent.component}")
        android.util.Log.d("MessagingService", "Intent flags: ${intent.flags}")
        
        // Use unique request code per conversation to prevent intent clobbering
        val requestCode = conversationId.hashCode()
        
        // Use FLAG_MUTABLE for Android 12+ to allow extras delivery when launching from killed state
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            pendingIntentFlags
        )
        
        android.util.Log.d("MessagingService", "PendingIntent created with requestCode: $requestCode")
        
        android.util.Log.d("MessagingService", "Created notification for conversation: $conversationId")
        android.util.Log.d("MessagingService", "Intent extras: conversationId=$conversationId, openChat=true")
        
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
            
        android.util.Log.d("MessagingService", "Notification displayed with ID: ${conversationId.hashCode()}")
    }
    
    private fun showReactionNotification(
        conversationId: String,
        reactorId: String,
        reactorName: String,
        emoji: String,
        messageText: String
    ) {
        android.util.Log.d("MessagingService", "showReactionNotification called: conversationId=$conversationId, reactor=$reactorName")
        
        // Don't show notification for own reactions
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == reactorId) {
            android.util.Log.d("MessagingService", "Skipping notification for own reaction")
            return
        }
        
        // Create intent to open conversation
        val intent = Intent(this, MainActivity::class.java).apply {
            component = android.content.ComponentName(this@MessagingService, MainActivity::class.java)
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("gchat://conversation/$conversationId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId)
            putExtra("openChat", true)
        }
        
        val requestCode = conversationId.hashCode()
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            pendingIntentFlags
        )
        
        // Build notification
        val title = "$reactorName reacted $emoji"
        val displayText = if (messageText.isNotEmpty() && messageText.length <= 50) {
            "To: $messageText"
        } else if (messageText.isNotEmpty()) {
            "To: ${messageText.take(47)}..."
        } else {
            "To your message"
        }
        
        val notification = NotificationCompat.Builder(this, GChatApplication.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setGroup("REACTIONS")
            .build()
        
        // Show notification with unique ID
        NotificationManagerCompat.from(this)
            .notify(("reaction_$conversationId").hashCode(), notification)
            
        android.util.Log.d("MessagingService", "Reaction notification displayed")
    }
}

