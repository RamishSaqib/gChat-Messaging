/**
 * Message Triggers
 * 
 * Handles automatic actions when messages are created (e.g., push notifications)
 */

import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { getFirestore } from 'firebase-admin/firestore';

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

      // Get recipient user IDs (exclude sender)
      const recipientIds = conversation.participants.filter(
        (id: string) => id !== message.senderId
      );

      if (recipientIds.length === 0) return;

      // Get sender details
      const senderDoc = await db.collection('users').doc(message.senderId).get();
      const sender = senderDoc.data();
      const senderName = sender?.displayName || 'Someone';

      // Get recipient FCM tokens
      const recipientDocs = await Promise.all(
        recipientIds.map((id: string) => db.collection('users').doc(id).get())
      );

      const tokens = recipientDocs
        .map((doc) => doc.data()?.fcmToken)
        .filter((token): token is string => token != null && token !== '');

      if (tokens.length === 0) {
        console.log('No FCM tokens found for recipients');
        return;
      }

      // Prepare notification
      const notificationTitle = conversation.type === 'GROUP'
        ? `${senderName} in ${conversation.name || 'Group Chat'}`
        : senderName;

      const notificationBody = message.type === 'IMAGE'
        ? '📷 Sent an image'
        : message.text || 'New message';

      // Send multicast notification
      const response = await messaging.sendEachForMulticast({
        tokens,
        data: {
          type: 'NEW_MESSAGE',
          conversationId,
          messageId,
          senderId: message.senderId,
        },
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        android: {
          priority: 'high',
          notification: {
            channelId: 'messages',
            sound: 'default',
            clickAction: 'FLUTTER_NOTIFICATION_CLICK',
            tag: conversationId, // Group notifications by conversation
          },
        },
        apns: {
          payload: {
            aps: {
              sound: 'default',
              badge: 1,
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

