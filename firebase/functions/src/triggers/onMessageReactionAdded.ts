/**
 * Reaction Triggers
 * 
 * Handles automatic actions when message reactions are updated (e.g., push notifications)
 */

import { onDocumentUpdated } from 'firebase-functions/v2/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';

const messaging = getMessaging();
const db = getFirestore();

/**
 * Send push notification when someone reacts to a message
 */
export const onMessageReactionAdded = onDocumentUpdated(
  'conversations/{conversationId}/messages/{messageId}',
  async (event) => {
    console.log('🎯 onMessageReactionAdded triggered');
    
    const beforeData = event.data?.before.data();
    const afterData = event.data?.after.data();
    
    if (!beforeData || !afterData) {
      console.log('❌ Missing before or after data');
      return;
    }

    const conversationId = event.params.conversationId;
    const messageId = event.params.messageId;
    
    console.log(`📝 Processing message ${messageId} in conversation ${conversationId}`);

    try {
      // Check if reactions field was updated
      const beforeReactions = (beforeData.reactions as Record<string, string[]>) || {};
      const afterReactions = (afterData.reactions as Record<string, string[]>) || {};
      
      console.log('Before reactions:', JSON.stringify(beforeReactions));
      console.log('After reactions:', JSON.stringify(afterReactions));
      
      // Find new reactions
      const newReactions: Array<{ emoji: string; userId: string }> = [];
      
      for (const [emoji, userIds] of Object.entries(afterReactions)) {
        const beforeUserIds = beforeReactions[emoji] || [];
        const addedUserIds = userIds.filter(uid => !beforeUserIds.includes(uid));
        
        addedUserIds.forEach(userId => {
          newReactions.push({ emoji, userId });
        });
      }
      
      console.log(`Found ${newReactions.length} new reactions:`, newReactions);
      
      // If no new reactions, exit
      if (newReactions.length === 0) {
        console.log('✅ No new reactions to process');
        return;
      }
      
      // Get message sender (person who will receive notification)
      const messageSenderId = afterData.senderId as string;
      
      // Send notification for each new reaction (but only to message sender)
      for (const { emoji, userId } of newReactions) {
        console.log(`👤 Processing reaction ${emoji} from user ${userId} to message sender ${messageSenderId}`);
        
        // Skip if user reacted to their own message
        if (userId === messageSenderId) {
          console.log(`⏭️ Skipping self-reaction from ${userId}`);
          continue;
        }
        
        console.log(`📥 Fetching reactor details for ${userId}`);
        // Get reactor details
        const reactorDoc = await db.collection('users').doc(userId).get();
        const reactor = reactorDoc.data();
        const reactorName = reactor?.displayName || 'Someone';
        
        console.log(`👤 Reactor name: ${reactorName}`);
        
        console.log(`📥 Fetching message sender details for ${messageSenderId}`);
        // Get message sender's FCM token
        const senderDoc = await db.collection('users').doc(messageSenderId).get();
        const sender = senderDoc.data();
        
        if (!sender?.fcmToken) {
          console.log(`❌ No FCM token for message sender ${messageSenderId}`);
          continue;
        }
        
        console.log(`✅ Found FCM token for sender: ${sender.fcmToken.substring(0, 20)}...`);
        
        console.log(`📥 Fetching conversation details for ${conversationId}`);
        // Get conversation details for nickname support
        const conversationDoc = await db
          .collection('conversations')
          .doc(conversationId)
          .get();
        
        const conversation = conversationDoc.data();
        const nicknames = (conversation?.nicknames as Record<string, string>) || {};
        const displayName = nicknames[userId] || reactorName;
        
        console.log(`📛 Display name (with nickname): ${displayName}`);
        console.log(`📤 Sending FCM notification...`);
        
        // Send notification
        await messaging.send({
          token: sender.fcmToken,
          data: {
            type: 'REACTION',
            conversationId,
            messageId,
            reactorId: userId,
            reactorName: displayName,
            emoji,
            messageText: (afterData.text as string) || '',
          },
          android: {
            priority: 'high',
          },
          apns: {
            headers: {
              'apns-priority': '10',
            },
          },
        });
        
        console.log(`✅ Notification sent successfully`);
        console.log(`📝 Updating conversation lastMessage...`);
        
        // Update conversation's lastMessage to show reaction event
        await db.collection('conversations').doc(conversationId).update({
          lastMessage: `${emoji} ${displayName} reacted to your message`,
          lastMessageType: 'SYSTEM',
          updatedAt: FieldValue.serverTimestamp(),
        });
        
        console.log(`✅ Conversation preview updated`);
      }
      
    } catch (error) {
      console.error('❌ Error in onMessageReactionAdded:', error);
      console.error('Error details:', JSON.stringify(error, null, 2));
    }
  }
);

