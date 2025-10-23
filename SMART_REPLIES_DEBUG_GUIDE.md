# Smart Replies Settings Debug Guide

## Issue Summary

Two issues were reported:
1. **Per-chat toggle affecting global setting**: When toggling smart replies per-chat, it appeared to affect the global setting
2. **Blank screen when navigating**: Sometimes navigating between per-chat settings and global profile editor resulted in a blank screen

## How Smart Replies Settings Work

### Two-Level Settings System

1. **Global Setting** (`User.smartRepliesEnabled`):
   - Stored in Firestore: `/users/{userId}/smartRepliesEnabled`
   - Default: `true` (enabled)
   - Controlled from: Profile Screen (Settings icon → Edit Profile)
   - Affects: All conversations that don't have a per-chat override

2. **Per-Chat Setting** (`Conversation.smartRepliesEnabled`):
   - Stored in Firestore: `/conversations/{conversationId}/smartRepliesEnabled`
   - Values: `null` (use global), `true` (force on), `false` (force off)
   - Controlled from: Chat Screen (3-dot menu → Smart Replies)
   - Affects: Only the specific conversation

### Toggle Logic (Per-Chat)

When you click the Smart Replies toggle in a chat:

1. **If currently "Using Global" (null)**:
   - Global is ON → Set per-chat to OFF (override to disable)
   - Global is OFF → Set per-chat to ON (override to enable)

2. **If currently "Enabled" (true)**:
   - Set to OFF (disable for this chat)

3. **If currently "Disabled" (false)**:
   - Set to null (reset to use global setting)

**Important**: The per-chat toggle NEVER modifies the global setting. It only modifies the conversation document.

## Verification Steps

### Test 1: Verify Per-Chat Doesn't Affect Global

1. **Setup**:
   - Open Profile Screen (Settings → Edit Profile)
   - Set Global Smart Replies to ON
   - Press "Save"
   - Note the setting

2. **Test Per-Chat Toggle**:
   - Open a conversation
   - Open the 3-dot menu
   - Toggle Smart Replies OFF (should show "Disabled")
   - Go back to Profile Screen

3. **Expected Result**:
   - Global setting should still be ON
   - It should NOT have changed to OFF

4. **Check Logs**:
   ```
   Look for these log patterns:
   
   ChatViewModel: "✅ Smart replies per-chat updated to: false (does NOT affect global setting)"
   ConversationRepo: "✅ Smart replies per-chat updated successfully: conversation=..., value=false"
   ProfileViewModel: "📥 User data collected: ... smartReplies=true ..." (should still be true)
   ```

### Test 2: Verify Global Doesn't Affect Per-Chat Overrides

1. **Setup**:
   - In a conversation, set per-chat Smart Replies to OFF
   - Note it shows "Disabled" in the menu

2. **Change Global**:
   - Go to Profile Screen
   - Toggle Global Smart Replies OFF
   - Press "Save"
   - Go back to the conversation

3. **Expected Result**:
   - Per-chat setting should still be OFF (not reset to null)
   - Menu should still show "Disabled"

### Test 3: Verify Navigation Doesn't Cause Crashes

1. **Rapid Navigation Test**:
   - Open conversation
   - Toggle Smart Replies
   - Immediately navigate to Profile Screen
   - Toggle Global Smart Replies (DON'T save yet)
   - Navigate back to conversation
   - Toggle Smart Replies again
   - Navigate back to Profile Screen

2. **Expected Result**:
   - No blank screens
   - No app crashes
   - Profile Screen loads correctly each time
   - Settings display correctly

3. **Check Logs**:
   ```
   Look for error logs:
   
   ProfileViewModel: "❌ Error loading user profile" (should NOT appear)
   ProfileViewModel: "❌ No user ID found!" (should NOT appear)
   ```

## Understanding the Logs

### Per-Chat Toggle Logs

When you toggle Smart Replies in a chat, you should see:

```
ChatViewModel: Toggling smart replies - current per-chat: null, global: true
ChatViewModel: Toggling smart replies from null to false (global=true)
ChatViewModel: ✅ Smart replies per-chat updated to: false (does NOT affect global setting)
ConversationRepo: 🔧 Updating smart replies for conversation ... to false (per-chat override, NOT global)
ConversationRepo: ✅ Smart replies per-chat updated successfully: conversation=..., value=false
```

### Global Settings Save Logs

When you save in Profile Screen, you should see:

```
ProfileViewModel: 💾 Saving GLOBAL settings to Firestore: {displayName=..., autoTranslateEnabled=..., smartRepliesEnabled=true}
ProfileViewModel: ✅ Profile saved successfully (GLOBAL settings updated)
```

### Profile Load Logs

When Profile Screen loads, you should see:

```
ProfileViewModel: 🔄 Loading profile for user: lZx...
ProfileViewModel: 📥 User data collected: displayName=..., smartReplies=true, autoTranslate=...
ProfileViewModel: ✅ Updated UI state - displayName: ..., smartReplies: true
```

## What Was Fixed

### 1. Enhanced Logging
- Added emoji-prefixed logs for easy identification
- Clear distinction between per-chat and global operations
- Logs explicitly state "does NOT affect global setting"

### 2. Error Handling
- Added try-catch in ProfileViewModel.loadUserProfile()
- Added null checks in ChatViewModel.toggleSmartReplies()
- Added error messages to prevent silent failures

### 3. Code Clarity
- Improved comments explaining the toggle logic
- Added explicit state checks before operations
- Enhanced error messages with context

## If Issue Persists

### Diagnostic Steps

1. **Clear App Data**:
   ```
   Settings → Apps → gChat → Storage → Clear Data
   ```
   This resets the local Room database.

2. **Check Firestore Console**:
   - Open Firebase Console
   - Go to Firestore Database
   - Navigate to: `/users/{your-user-id}`
   - Check `smartRepliesEnabled` field (should be boolean)
   - Navigate to: `/conversations/{conversation-id}`
   - Check `smartRepliesEnabled` field (should be boolean or null)

3. **Capture Full Logs**:
   ```
   adb logcat -s ProfileViewModel:D ChatViewModel:D ConversationRepo:D UserRepository:D
   ```
   This will show only relevant logs for debugging.

4. **Look for Specific Issues**:
   - Are there any ERROR logs?
   - Does the per-chat value actually change in Firestore?
   - Does the global value change when you only toggle per-chat?
   - Are there any exceptions or crashes?

## Architecture Explanation

### Why They're Separate

The per-chat and global settings are stored in completely different Firestore documents:

- **Global**: `/users/{userId}` document
- **Per-Chat**: `/conversations/{conversationId}` document

The only code that writes to the global setting is:
```kotlin
authRepository.updateUserProfile(userId, updates) // in ProfileViewModel.saveProfile()
```

The only code that writes to the per-chat setting is:
```kotlin
conversationRepository.updateSmartReplies(conversationId, enabled) // in ChatViewModel.toggleSmartReplies()
```

These are completely independent operations and should never interfere with each other.

### Effective Setting Calculation

When displaying or using Smart Replies, the app calculates the "effective" setting:

```kotlin
val effectiveSmartReplies = conversation.smartRepliesEnabled ?: user.smartRepliesEnabled ?: true
```

This means:
1. If per-chat is set (true/false), use that
2. Otherwise, if global is set, use that
3. Otherwise, default to true (enabled)

This calculation is done at **read time**, not write time, ensuring the settings remain independent.

## Contact

If the issue persists after these fixes, please provide:
1. Full logcat output with the filters above
2. Screenshots of the Firestore documents
3. Step-by-step reproduction instructions

