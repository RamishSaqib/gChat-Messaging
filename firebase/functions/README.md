# gChat Firebase Cloud Functions

This directory contains Firebase Cloud Functions for gChat's backend operations.

## Functions

### 1. `sendMessageNotification`
**Trigger:** `onCreate` event on `/conversations/{conversationId}/messages/{messageId}`

**Purpose:** Automatically sends push notifications to all conversation participants (except the sender) when a new message is created.

**Features:**
- Handles both 1-on-1 and group chats
- Displays sender name in notification
- Shows group name for group chats
- Supports text and image messages
- Cleans up invalid FCM tokens automatically
- Truncates long messages in notifications

### 2. `deleteConversationMessages`
**Trigger:** `onDelete` event on `/conversations/{conversationId}`

**Purpose:** Clean up all messages when a conversation is deleted (subcollection cleanup).

### 3. `updateUserLastSeen`
**Trigger:** `onUpdate` event on `/users/{userId}`

**Purpose:** Automatically updates the user's `lastSeen` timestamp when they go offline.

## Setup Instructions

### Prerequisites
- Node.js 18 or higher
- Firebase CLI installed (`npm install -g firebase-tools`)
- Firebase project initialized

### Installation

1. Navigate to the functions directory:
   ```bash
   cd firebase/functions
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Login to Firebase (if not already logged in):
   ```bash
   firebase login
   ```

### Local Development

Run functions locally using the Firebase Emulator:

```bash
npm run serve
```

This will start the Functions emulator and you can test the functions locally before deploying.

### Deployment

Deploy all functions to Firebase:

```bash
npm run deploy
```

Or deploy a specific function:

```bash
firebase deploy --only functions:sendMessageNotification
```

### View Logs

View function execution logs:

```bash
npm run logs
```

Or view logs for a specific function:

```bash
firebase functions:log --only sendMessageNotification
```

## Configuration

The functions use the Firebase Admin SDK, which is automatically configured when deployed to Firebase.

For local development, you may need to set up service account credentials:

1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate New Private Key"
3. Save the JSON file to `firebase/functions/service-account-key.json`
4. Set the environment variable:
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="path/to/service-account-key.json"
   ```

**⚠️ IMPORTANT:** Never commit service account keys to version control!

## Testing

### Test Notification Sending

1. Send a message through the app
2. Check Firebase Functions logs for execution
3. Verify notification appears on recipient devices

### Test Locally

Use the Firebase Emulator Suite to test functions before deploying:

```bash
firebase emulators:start --only functions,firestore
```

## Troubleshooting

### Functions not triggering
- Check Firebase Console → Functions for deployment status
- Verify Firestore triggers are correctly configured
- Check function logs for errors

### Notifications not sending
- Verify FCM tokens are stored correctly in Firestore user documents
- Check that POST_NOTIFICATIONS permission is granted on Android 13+
- Verify Firebase Cloud Messaging is enabled in Firebase Console

### Invalid tokens
The `sendMessageNotification` function automatically removes invalid tokens, but you can manually clean up:

```javascript
// Query users with old/invalid tokens and remove them
```

## Cost Optimization

Firebase Cloud Functions pricing is based on:
- Number of invocations
- Compute time
- Memory allocated
- Outbound networking

**Tips:**
- Keep functions lightweight
- Batch operations when possible
- Use appropriate memory allocation
- Monitor usage in Firebase Console

## Security

- Functions run with Firebase Admin privileges
- Access is controlled by Firestore Security Rules
- FCM tokens are server-side only
- No client-side notification sending (secure)

## Future Enhancements

- [ ] Notification grouping by conversation
- [ ] Silent data messages for in-app updates
- [ ] Typing indicator push notifications (optional)
- [ ] Read receipt syncing via Cloud Functions
- [ ] Message encryption/decryption hooks
- [ ] AI translation Cloud Function integration
- [ ] Scheduled message cleanup (old messages)

