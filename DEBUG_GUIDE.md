# Debug Guide: Performance & Notification Issues

## 🚨 Current Status

I've added **comprehensive logging** to help identify the root causes of:
1. **Chat header showing "Chat" briefly** when opening a DM
2. **Notification clicks not working** from background/killed app states

---

## 📋 What Was Changed

### 1. Enhanced MainActivity Logging

Added detailed logs with visual separators to `MainActivity.kt`:
- **onCreate**: Shows intent action, extras, conversationId, openChat flag, and intent flags
- **onNewIntent**: Same detailed logging for when app receives new intents while running
- **Visual separators**: `═══════════════` to make logs easy to spot

### 2. Enhanced ChatViewModel Logging

Added detailed logs to `ChatViewModel.kt` for the `otherUserName` flow:
- Shows when the flow starts
- Shows if conversation is found
- Shows if it's a DM or group chat
- **✅ Success**: When cached name is found and emitted immediately
- **❌ Failure**: When no cached name is found and "Chat" is emitted
- **📡 Updates**: When real-time name updates arrive

---

## 🧪 How to Test & Get Useful Logs

### Test 1: Chat Header Performance

**Steps:**
1. Open the app and log in
2. Navigate to conversation list
3. **Click on a DM conversation** (not a group chat)
4. Watch the top-left header

**What to watch for:**
- Does it show "Chat" briefly before the user's name?
- How long does the flicker last?

**Logs to capture (filter by "ChatViewModel"):**
```
grep ChatViewModel
```

**Expected logs if working:**
```
ChatViewModel: otherUserName flow - conversationId: xxx, userId: yyy, conv: true
ChatViewModel: otherUserName flow - isGroupChat: false, otherUserId: zzz
ChatViewModel: Fetching user from cache for DM: zzz
ChatViewModel: ✅ Emitting cached name immediately: John Doe
```

**Logs if NOT working (problem):**
```
ChatViewModel: ❌ No cached user found, emitting 'Chat' while loading
ChatViewModel: 📡 Emitting updated name from flow: John Doe
```

---

### Test 2: Notification Deep Linking (Foreground)

**Steps:**
1. Open the app
2. Navigate to conversation list (NOT in a chat)
3. **Have someone send you a message**
4. **Click the notification**

**What to watch for:**
- Does it navigate to the correct chat?
- Does the back button work correctly?

**Logs to capture (filter by "MainActivity"):**
```
grep MainActivity
```

**Expected logs if working:**
```
MainActivity: ═══════════════════════════════════════
MainActivity: onNewIntent called
MainActivity: Intent action: com.gchat.OPEN_CONVERSATION
MainActivity: Intent extras: conversationId, openChat
MainActivity: conversationId extra: 95bc9a0a-91fe-4eea-8c7a-945153ce3155
MainActivity: openChat extra: true
MainActivity: ═══════════════════════════════════════
MainActivity: handleIntent called
MainActivity: Setting navigation event for: 95bc9a0a-91fe-4eea-8c7a-945153ce3155
MainActivity: Navigating to chat from notification: 95bc9a0a-91fe-4eea-8c7a-945153ce3155
```

**Logs if NOT working:**
```
MainActivity: Intent action: android.intent.action.MAIN  ← WRONG!
MainActivity: Intent extras: null  ← NO DATA!
MainActivity: openChat extra: false
```

---

### Test 3: Notification Deep Linking (Background)

**Steps:**
1. Open the app
2. Press the home button to background it
3. **Have someone send you a message**
4. **Click the notification from notification shade**

**What to watch for:**
- Does the app open?
- Does it navigate to the correct chat?

**Logs to capture:**
Same as Test 2, but look for **onNewIntent** (not onCreate).

---

### Test 4: Notification Deep Linking (Killed)

**IMPORTANT:** This test requires you to **actually click a notification**, not just open the app!

**Steps:**
1. Open the app and log in
2. **Force kill the app** (swipe away from recents menu)
3. **Wait for someone to send you a message** - you should see a notification appear
4. **TAP ON THE NOTIFICATION** in the notification shade (NOT the app icon!)
5. Watch what happens

**What to watch for:**
- Does the app launch?
- Does it go directly to the login/conversation list, or to the specific chat?

**How to verify you clicked the notification correctly:**
The logs MUST show MainActivity's onCreate logs with the notification intent. If you see:
```
PROCESS STARTED
```
But NO `MainActivity: ═══════════════` logs, then **you didn't click the notification** - you opened the app normally!

**Logs to capture:**
```
grep "MainActivity\|PROCESS STARTED\|MessagingService"
```

**Expected logs if working:**
```
---------------------------- PROCESS STARTED (12345) for package com.gchat.debug ----------------------------
MainActivity: ═══════════════════════════════════════
MainActivity: onCreate called - savedInstanceState: NEW
MainActivity: Intent action: com.gchat.OPEN_CONVERSATION  ← CORRECT!
MainActivity: Intent extras: conversationId, openChat  ← HAS DATA!
MainActivity: conversationId extra: 95bc9a0a-91fe-4eea-8c7a-945153ce3155
MainActivity: openChat extra: true
MainActivity: ═══════════════════════════════════════
```

**Logs if NOT working (current issue):**
```
---------------------------- PROCESS STARTED (12345) for package com.gchat.debug ----------------------------
MainActivity: ═══════════════════════════════════════
MainActivity: onCreate called - savedInstanceState: NEW
MainActivity: Intent action: android.intent.action.MAIN  ← WRONG!
MainActivity: Intent extras: null  ← NO DATA!
MainActivity: conversationId extra: null
MainActivity: openChat extra: false
MainActivity: ═══════════════════════════════════════
```

---

## 🔍 What the Logs Will Tell Us

### For Chat Header Issue:

**If you see:**
```
❌ No cached user found, emitting 'Chat' while loading
```

**Then:** The user data is NOT in the local cache, even though it should be. This means:
- Either the user was never cached
- Or the cache was cleared
- Or there's a timing issue where the cache isn't populated yet

**If you see:**
```
✅ Emitting cached name immediately: John Doe
```
**But still see "Chat" briefly**, then it's a UI rendering issue, not a data issue.

---

### For Notification Deep Linking Issue:

**If you see:**
```
Intent action: android.intent.action.MAIN
Intent extras: null
```

**Then:** The custom action (`com.gchat.OPEN_CONVERSATION`) is NOT being received. This means:
- The notification wasn't created with the custom action (check `MessagingService` logs)
- The manifest change didn't take effect (need to uninstall/reinstall)
- Android is stripping the intent extras for some reason

**If you see:**
```
Intent action: com.gchat.OPEN_CONVERSATION
Intent extras: conversationId, openChat
conversationId extra: 95bc9a0a-91fe-4eea-8c7a-945153ce3155
```

**Then:** The intent IS being received correctly, and navigation should work!

---

## 📝 How to Provide Logs

When testing, please:

1. **Clear the logcat** before each test
2. **Perform ONLY ONE test** at a time
3. **Capture the full logs** immediately after
4. **Tell me which test you performed**

**Example:**
```
Test 3 (Notification from Background):
[paste logs here]

Result: App opened but didn't navigate to chat, stayed on conversation list.
```

---

## ✅ Next Steps

**After rebuilding and testing:**

1. **First test**: Chat header performance (Test 1)
   - This will tell us if the cache-first approach is working

2. **Second test**: Notification from foreground (Test 2)
   - This should already work, just confirming

3. **Third test**: Notification from background (Test 3)
   - This will show if `onNewIntent` is receiving the custom action

4. **Fourth test**: Notification from killed state (Test 4)
   - This is the critical one that's currently failing

**Once I see the logs, I'll know exactly what's wrong and can fix it!**

