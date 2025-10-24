/**
 * Message Triggers
 * 
 * Handles automatic actions when messages are created (e.g., push notifications)
 */

import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { getFirestore } from 'firebase-admin/firestore';
import * as admin from 'firebase-admin';

const messaging = getMessaging();
const db = getFirestore();

/**
 * Send push notification when a new message is created
 */
export const onMessageCreated = onDocumentCreated(
  'conversations/{conversationId}/messages/{messageId}',
  async (event) => {
    const message = event.data?.data();
    if (!message) return;

    const conversationId = event.params.conversationId;
    const messageId = event.params.messageId;

    try {
      // Get conversation details
      const conversationDoc = await db
        .collection('conversations')
        .doc(conversationId)
        .get();

      if (!conversationDoc.exists) {
        console.error('Conversation not found:', conversationId);
        return;
      }

      const conversation = conversationDoc.data();
      if (!conversation) return;

      // Clear all reaction notifications when a new message arrives
      // This ensures reaction previews don't stay stuck when new messages are sent
      await db.collection('conversations').doc(conversationId).update({
        reactionNotifications: {},
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      console.log(`Cleared reaction notifications for conversation ${conversationId}`);

      // Get recipient user IDs (exclude sender)
      const recipientIds = conversation.participants.filter(
        (id: string) => id !== message.senderId
      );

      if (recipientIds.length === 0) return;

      // Get sender details
      const senderDoc = await db.collection('users').doc(message.senderId).get();
      const sender = senderDoc.data();
      const senderName = sender?.displayName || 'Someone';

      // Get recipient FCM tokens (fetch individually to avoid Firestore 'in' query limits)
      const recipientDocs = await Promise.all(
        recipientIds.map((id: string) => db.collection('users').doc(id).get())
      );

      const tokens = recipientDocs
        .map((doc) => {
          const user = doc.data();
          if (user?.fcmToken) {
            console.log(`Found FCM token for user ${doc.id}: ${user.fcmToken.substring(0, 20)}...`);
            return user.fcmToken;
          } else {
            console.log(`No FCM token for user ${doc.id}`);
            return null;
          }
        })
        .filter((token): token is string => token != null && token !== '');

      if (tokens.length === 0) {
        console.log('No FCM tokens found for recipients');
        return;
      }

      // Prepare notification body
      const notificationBody = message.type === 'IMAGE'
        ? '📷 Sent an image'
        : message.type === 'AUDIO'
        ? '🎤 Voice message'
        : message.text || 'New message';

      console.log(`Sending notification from ${senderName} (${message.senderId}) with message: ${notificationBody}`);

      // Send data-only message (no notification payload)
      // This ensures MessagingService.onMessageReceived() is ALWAYS called,
      // even when the app is in the background or killed.
      const response = await messaging.sendEachForMulticast({
        tokens,
        data: {
          type: 'NEW_MESSAGE',
          conversationId,
          messageId,
          senderId: message.senderId,
          senderName: senderName,
          messageText: notificationBody,
          isGroup: conversation.type === 'GROUP' ? 'true' : 'false',
          groupName: conversation.name || '',
        },
        android: {
          priority: 'high',
        },
        apns: {
          headers: {
            'apns-priority': '10',
          },
          payload: {
            aps: {
              'content-available': 1,
            },
          },
        },
      });

      console.log(`Sent notifications: ${response.successCount} successful, ${response.failureCount} failed`);

      // Clean up invalid tokens
      const failedTokens = response.responses
        .map((resp, idx) => (resp.success ? null : tokens[idx]))
        .filter((token): token is string => token != null);

      if (failedTokens.length > 0) {
        console.log('Cleaning up failed tokens:', failedTokens);
        // Remove invalid FCM tokens
        await Promise.all(
          failedTokens.map(async (token) => {
            const userQuery = await db
              .collection('users')
              .where('fcmToken', '==', token)
              .limit(1)
              .get();

            if (!userQuery.empty) {
              await userQuery.docs[0].ref.update({ fcmToken: null });
            }
          })
        );
      }
    } catch (error) {
      console.error('Error sending notification:', error);
    }
  }
);

