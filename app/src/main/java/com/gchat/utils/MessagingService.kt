package com.gchat.utils

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gchat.GChatApplication
import com.gchat.R
import com.gchat.presentation.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
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
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Update FCM token in Firestore
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnFailureListener { e ->
                // Log error but don't crash
                println("Failed to update FCM token: ${e.message}")
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val data = message.data
        
        when (data["type"]) {
            "NEW_MESSAGE" -> {
                val conversationId = data["conversationId"] ?: return
                val senderId = data["senderId"] ?: return
                val messageText = message.notification?.body ?: "New message"
                
                showMessageNotification(conversationId, senderId, messageText)
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
        messageText: String
    ) {
        // Create intent to open conversation
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("conversationId", conversationId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(this, GChatApplication.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification) // TODO: Add notification icon
            .setContentTitle("New message")
            .setContentText(messageText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        
        // Show notification
        NotificationManagerCompat.from(this)
            .notify(conversationId.hashCode(), notification)
    }
}

