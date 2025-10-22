# gChat - Product Requirements Document

> **Status**: 🚀 MVP Phase Complete | 🎯 Ready for AI Features

---

## 📋 Pull Request History

### PR #11: Online Status Accuracy Fix
**Status:** ✅ Merged to `main`  
**Date:** October 22, 2025  
**Time Spent:** ~1 hour

**Problem Fixed:**
When emulators/apps were force-killed, the `onStop()` lifecycle callback never fired, leaving users with `isOnline = true` in Firestore indefinitely. This caused all users to appear online even when they weren't running the app.

**Solution Implemented:**
- Added 60-second heartbeat mechanism to update `lastSeen` timestamp while app is in foreground
- Created `isActuallyOnline()` helper function that validates both `isOnline = true` AND `lastSeen` within 2 minutes
- Updated all 7 UI locations to use `isActuallyOnline()` instead of raw `isOnline` flag
- Changed registration to set `isOnline = false` (app lifecycle sets true on launch)

**Technical Details:**
- `GChatApplication.kt`: Added `heartbeatJob` coroutine with 60-second delay loop
- `User.kt`: Added `isActuallyOnline()` with 2-minute timestamp validation
- Updated: ConversationListScreen, ChatScreen, DMInfoScreen, GroupInfoScreen, NewConversationScreen
- `AuthRepositoryImpl.kt`: Set `isOnline = false` for new user registration (email & Google Sign-In)

**Behavior:**
- App running: Heartbeat keeps `lastSeen` fresh, user shows online
- Force-kill: After 2 minutes of no heartbeat, user auto-appears offline
- Proper close: `onStop()` sets `isOnline = false` immediately
- Reopen: `onStart()` sets `isOnline = true` and restarts heartbeat

**Bugs Fixed:**
- Fixed stale online status after force-kill
- Fixed new users defaulting to online before first app launch
- Fixed online indicators not updating within 2 minutes of inactivity

---

### PR #13: Inline Translation UI
**Status:** ✅ Merged to `main`  
**Date:** October 22, 2025  
**Time Spent:** ~4 hours

**Features Implemented:**
- ✅ Long-press message to show translation menu
- ✅ Language selector dialog with 20+ supported languages
- ✅ Inline translation display below original message
- ✅ Translation loading indicator
- ✅ Error handling with retry button
- ✅ Hide translation button to dismiss
- ✅ Translation caching in Firestore (30-day TTL)
- ✅ Local Room database cache for offline access
- ✅ Rate limiting (100 translations/hour, 200 language detections/hour)
- ✅ Works for both sent and received messages

**Technical Implementation:**
- **Backend (Firebase Functions):**
  - `translateMessage` Cloud Function with OpenAI GPT-4
  - Automatic source language detection
  - SHA-256 cache key generation
  - Rate limiting with Firestore tracking
  - Environment-based configuration (model, TTL, rate limits)
  
- **Android App:**
  - `Translation` domain model with 20+ language support
  - `TranslationRepository` with dual caching (Firestore + Room)
  - Room database v9 migration for translation cache
  - `TranslateMessageUseCase` for clean architecture
  - UI components: `LanguageSelectorDialog`, `TranslationDisplay`, `TranslationLoading`, `TranslationError`
  - Long-press gesture detection in `MessageBubble`
  - ViewModel state management for translations, loading, and errors

**Database Changes:**
- Room v8 → v9: Added `TranslationEntity` table
- Room v9 → v10: Added `lastMessageType` and `lastMessageMediaUrl` to `ConversationEntity`
- Firestore: Added `translations` collection for caching
- Firestore: Added `rateLimits/{userId}/features/{featureName}` for rate tracking

**Firestore Rules:**
- `translations` collection: Read for authenticated users, write only via Cloud Functions
- `rateLimits` collection: Internal only (Cloud Functions)

**Critical Bugs Fixed:**
1. **Image Messages Not Syncing**
   - Root cause: Firestore rules rejected `text = null` for image-only messages
   - Fix: Updated `isValidMessage()` to allow `text == null || text is string`
   
2. **Conversation Preview Showing "New Message"**
   - Root cause: Repository hardcoded `type=TEXT` and `mediaUrl=null` when reading from Room
   - Fix: Read `entity.lastMessageType` and `entity.lastMessageMediaUrl` in 4 locations
   
3. **Translation Loading Not Showing**
   - Root cause: UI wasn't collecting translation state flows from ViewModel
   - Fix: Added `collectAsState()` for `translations`, `translationLoading`, `translationErrors`

4. **Image-Only Message Sync Failure**
   - Root cause: `SendMessageUseCase` passed empty string `""` instead of `message.text` (null)
   - Fix: Pass `message.text` to ensure consistency between message and conversation preview

**Behavior:**
- User long-presses any text message → Language selector appears
- Select target language → Loading indicator shows → Translation appears below message
- Translation cached locally and in Firestore for 30 days
- Second translation of same message loads instantly from cache
- Image messages do NOT show translation menu (long-press ignored)
- Rate limit exceeded → Error message with friendly explanation
- Translation error → Retry button to attempt again

---

### PR #12: Backend Infrastructure & Translation API
**Status:** ✅ Merged to `main`  
**Date:** October 22, 2025  
**Time Spent:** ~2 hours

**Features Implemented:**
- ✅ OpenAI GPT-4 integration for Cloud Functions
- ✅ `translateMessage` callable function
- ✅ `detectLanguage` callable function
- ✅ Translation caching with SHA-256 keys
- ✅ Rate limiting infrastructure
- ✅ Centralized OpenAI client and configuration
- ✅ Environment variable management

**Technical Implementation:**
- `firebase/functions/src/utils/openai.ts`: Centralized OpenAI client
- `firebase/functions/src/utils/cache.ts`: Firestore caching utilities
- `firebase/functions/src/utils/rateLimit.ts`: Rate limiting with time windows
- `firebase/functions/src/ai/translation.ts`: Translation and language detection
- Updated `firestore.rules` for `translations` and `rateLimits` collections
- Documentation: `AI_SETUP.md` and `ENV_SETUP.md`

**Environment Variables:**
```
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4-turbo-preview
TRANSLATION_CACHE_TTL_DAYS=30
MAX_REQUESTS_PER_HOUR=100
MAX_LANGUAGE_DETECTION_PER_HOUR=200
```

---

### PR #10: Nickname System
**Status:** ✅ Merged to `main`  
**Date:** October 21, 2025  
**Time Spent:** ~2 hours

**Features Implemented:**
- ✅ Per-conversation nickname system for users
- ✅ Nickname setting in group chat member list
- ✅ Nickname setting in DM info screen
- ✅ Nicknames display everywhere: messages, typing indicators, TopBar, conversation list
- ✅ Empty nickname falls back to real display name
- ✅ Nicknames stored in Firestore conversation document
- ✅ Nicknames appear in push notifications

**UI Design:**
- **Group Chats:** Members list in GroupInfoScreen has menu option to set nickname
- **DMs:** DMInfoScreen has menu option to set nickname for other user
- **Display:** Nicknames replace display names throughout the app when set
- **Conversation List:** Shows nickname as conversation title for DMs
- **Message Preview:** Smart formatting - shows "You: " for own messages, nickname for others in groups, just content for others in DMs
- **Dialogs:** Simple text input with Save, Remove, and Cancel buttons

**Technical Implementation:**
- Extended `Conversation` model with `nicknames` map (userId → nickname string)
- Added `getUserDisplayName()` helper to get nickname or fallback to real name
- Updated `ConversationEntity` and database schema (version 6)
- Extended `ConversationRepository` with `setNickname()` method
- Updated `ChatViewModel` to use nicknames in typing indicator text
- Updated `GroupInfoViewModel` and `DMInfoViewModel` with nickname methods
- Updated `ConversationListViewModel` to fetch `lastMessageSender` for nickname display
- Modified Cloud Functions to use nicknames in push notifications
- Updated Firestore security rules to allow `nicknames` field updates

**Navigation Flow:**
- **Groups:** GroupInfoScreen → Tap member → "Change Nickname" option
- **DMs:** DMInfoScreen → "Change Nickname" menu item

**Bugs Fixed:**
- Fixed nickname not showing in conversation list title
- Fixed nickname not showing in notifications (Cloud Functions update)
- Fixed notification click navigation issue (MainActivity intent handling)
- Fixed message preview showing duplicate names in DMs
- Fixed `val` reassignment errors in GroupInfoScreen
- Fixed missing User import in ConversationListScreen

**Testing Notes:**
- Test setting/removing nicknames in group chats
- Test setting/removing nicknames in DMs
- Test that nicknames appear in all UI locations
- Test that notifications use nicknames
- Test that notification clicks navigate correctly
- Verify message preview formatting is clean

---

### PR #9: Profile & Group Management
**Status:** ✅ Merged to `main`  
**Date:** October 21, 2025  
**Time Spent:** ~2.5 hours

**Features Implemented:**
- ✅ User profile screen with display name and profile picture editing
- ✅ Profile picture button in ConversationListScreen TopBar
- ✅ Group info screen with full management capabilities
- ✅ DM info screen for 1-on-1 chat details
- ✅ Add/remove group members (admin only)
- ✅ Promote members to admin
- ✅ Edit group name and icon
- ✅ Leave group functionality
- ✅ Profile picture and online status in ChatScreen TopBar

**UI Design:**
- **Profile Screen:** Full-screen editor for display name and profile picture
- **Group Info Screen:** Group icon at top, editable name, member list with admin badges
- **DM Info Screen:** Shows other user's profile, online status, and last seen
- **TopBar Enhancements:** 
  - ConversationListScreen: Profile picture (left), "gChat" centered, logout (right)
  - ChatScreen: Profile picture + name (left), shows online indicator for 1-on-1 chats
- **Member Management:** Tap member to promote or remove (admins only)
- **Image Uploads:** Camera/gallery picker for profile pictures and group icons

**Technical Implementation:**
- Created `ProfileScreen` and `ProfileViewModel` for user profile editing
- Created `GroupInfoScreen` and `GroupInfoViewModel` for group management
- Created `DMInfoScreen` and `DMInfoViewModel` for 1-on-1 chat info
- Added navigation routes: `Profile`, `GroupInfo`, `DMInfo`
- Extended `ConversationRepository` with group management methods:
  - `updateGroupName()`, `updateGroupIcon()`
  - `addParticipants()`, `removeParticipant()`
  - `promoteToAdmin()`, `leaveGroup()`
- Updated Firestore storage rules for `profile_pictures/` and `group_icons/` paths
- Refactored `GroupInfoContent` into smaller composables to reduce complexity:
  - `GroupIconSection`, `GroupNameSection`, `MembersSectionHeader`
- Used `CenterAlignedTopAppBar` for proper title centering
- Image upload with progress indicators and error handling

**Navigation Flow:**
- **Profile:** Tap profile picture in ConversationListScreen TopBar → ProfileScreen
- **Group Info:** Tap TopBar title/icon in group chat → GroupInfoScreen
- **DM Info:** Tap TopBar title/icon in 1-on-1 chat → DMInfoScreen

**Bugs Fixed:**
- Fixed `val` reassignment errors by using callback functions properly
- Fixed TopBar title alignment using `CenterAlignedTopAppBar`

**Testing Notes:**
- Test profile picture upload from camera and gallery
- Test group name and icon editing
- Test adding/removing members (admin only)
- Test promoting users to admin
- Test leaving a group
- Verify online status shows correctly in chat TopBar
- Verify centered "gChat" title in main screen

---

### PR #8: Typing Indicators
**Status:** ✅ Merged to `main`  
**Date:** October 21, 2025  
**Time Spent:** ~1.5 hours

**Features Implemented:**
- ✅ Real-time typing indicators in 1-on-1 and group chats
- ✅ Animated typing dots with smooth fade animation
- ✅ Debouncing logic (typing stops after 3 seconds of inactivity)
- ✅ Smart formatting for group chats ("John is typing...", "John and Sarah are typing...", "John, Sarah, and 2 others are typing...")
- ✅ Automatic typing status cleanup on message send
- ✅ Efficient Firestore listeners with minimal writes

**UI Design:**
- Typing indicator appears at the bottom of the message list
- Translucent gray bubble with italic text
- Three animated dots that fade in and out sequentially
- Shows user names for clarity in group chats
- Does not show your own typing status (filters out current user)

**Technical Implementation:**
- Created `TypingIndicator` domain model (conversationId, userId, isTyping, timestamp)
- Created `FirestoreTypingDataSource` for real-time typing updates
- Created `TypingRepository` and `TypingRepositoryImpl` for typing operations
- Updated `ChatViewModel` to track and broadcast typing status
- Added debouncing with 3-second timeout using coroutine Job cancellation
- `updateMessageText()` sets typing status on text change
- `sendMessage()` clears typing status immediately
- Created `TypingIndicator`, `TypingDots`, and `TypingDot` composables
- Uses `rememberInfiniteTransition` for smooth animated dots
- Firestore rules already support typing subcollection (read/write by participants)

**How It Works:**
1. User types → `updateMessageText()` called
2. Set `isTyping = true` in Firestore `/conversations/{id}/typing/{userId}`
3. Start 3-second debounce timer
4. If no typing activity → auto-clear typing status
5. Other users observe typing indicators via Firestore snapshot listener
6. `typingIndicatorText` StateFlow formats names ("John is typing...")
7. UI displays typing indicator at bottom of chat with animated dots
8. On send message → clear typing status immediately

**Bugs Fixed:**
- Fixed typing indicators not showing in 1-on-1 chats (populated participantUsers for all chats, not just groups)
- Fixed race condition where typing indicators wouldn't update if user data loaded after typing started (changed from `.map()` to `combine()` for reactive updates)

**Testing Notes:**
- Open chat on two devices
- Type on Device A → Device B should see "[Name] is typing..." within 500ms
- Stop typing → indicator should disappear after 3 seconds
- Send message → indicator should disappear immediately
- Group chat: multiple users typing should show all names
- Typing indicator should auto-scroll to bottom

---

### PR #6: Read Receipts
**Status:** ✅ Merged to `main`  
**Date:** October 21, 2025  
**Time Spent:** ~1.5 hours

**Features Implemented:**
- ✅ Read receipts for 1-on-1 chats (checkmarks)
- ✅ Read receipts for group chats (profile picture avatars)
- ✅ Auto-mark messages as read when conversation is opened
- ✅ Real-time read status updates across devices
- ✅ Timestamp tracking for when each user reads a message

**UI Design:**
- **1-on-1 Chats:**
  - Single gray checkmark (✓) - Message sent but not read
  - Double white checkmarks (✓✓) - Message read by recipient
- **Group Chats:**
  - Small profile picture avatars of users who read the message
  - Stacked avatars (max 3 visible, "+X" for additional readers)
  - Positioned at bottom-right of message bubble next to timestamp
  - Sender's avatar excluded from read indicators

**Technical Implementation:**
- Updated `Message.readBy` from `List<String>` to `Map<String, Long>` (userId → readTimestamp)
- Created `MarkMessageAsReadUseCase` for marking messages as read
- Added helper functions: `isReadBy()`, `getReadTimestamp()`, `isReadByAll()`, `isReadByAny()`
- Created `ReadReceiptCheckmarks` composable for 1-on-1 chats
- Created `ReadByAvatars` composable for group chats (stacked profile pictures)
- Updated `MessageBubble` to conditionally show checkmarks vs avatars based on chat type
- `ChatViewModel.markAllMessagesAsRead()` auto-marks messages when screen is viewed
- Firestore and Room database support for read status persistence
- Backward compatible with old list format

**Bugs Fixed:**
- Fixed compilation errors from `readBy` type change (List → Map)
- Consolidated duplicate use case files (removed plural version)

**Testing Notes:**
- Send message from Device A, open chat on Device B to see read status change
- Group chats show stacked avatars of all users who have read the message
- Read status updates in real-time via Firestore listeners

---

### PR #7: Push Notifications (FCM)
**Status:** ✅ Merged to `main`  
**Date:** October 21, 2025  
**Time Spent:** ~3 hours

**Features Implemented:**
- ✅ Automatic FCM token registration and updates
- ✅ FCM token updates on app foreground and login
- ✅ Notification permission request for Android 13+ (POST_NOTIFICATIONS)
- ✅ Rich notifications with sender names and message preview
- ✅ Group chat notifications with group name and sender
- ✅ Notification click navigation to specific conversation
- ✅ Firebase Cloud Functions for server-side notification sending
- ✅ Automatic invalid token cleanup
- ✅ BigTextStyle notifications for long messages

**Technical Completed:**
- Enhanced MessagingService with UserRepository integration
- Added FCM token update in GChatApplication (onStart lifecycle)
- Improved notification display with sender info and group chat support
- MainActivity now handles notification intents and navigates to conversation
- ConversationListScreen requests POST_NOTIFICATIONS permission on Android 13+
- Created Cloud Function: `sendMessageNotification` (triggers on new message)
- Created Cloud Function: `deleteConversationMessages` (cleanup on conversation delete)
- Created Cloud Function: `updateUserLastSeen` (auto-update on offline)
- Added firebase/functions with package.json, index.js, README, .gitignore
- **Fixed:** Updated Cloud Functions to use modern FCM HTTP v1 API (replaced deprecated `sendToDevice` with `sendEach`)
- Added comprehensive FCM token logging for troubleshooting

**Notification Features:**
- Title: Sender name (1-on-1) or Group name (group chat)
- Body: Message text (1-on-1) or "Sender: Message" (group chat)
- Click action: Opens app directly to the conversation
- Grouped by conversation ID
- High priority for immediate delivery
- 24-hour TTL for pending notifications

**Cloud Functions Setup Required:**
1. Navigate to `firebase/functions/`
2. Run `npm install` to install dependencies
3. Run `npm run deploy` to deploy functions to Firebase
4. Verify functions are active in Firebase Console

**How It Works:**
1. User opens app → FCM token retrieved and stored in Firestore
2. User sends message → Firestore onCreate trigger
3. Cloud Function `sendMessageNotification` executes
4. Function fetches recipient FCM tokens from Firestore
5. Notification sent via Firebase Admin SDK
6. Recipient taps notification → App opens to conversation

**Bugs Fixed:**
- None (new feature)

**Testing Notes:**
- Cloud Functions must be deployed for notifications to work
- Test with two devices: one sends message, other receives notification
- Foreground notifications appear as system notifications
- Background/killed app: notification wakes app on tap

---

### PR #5.7: Real-Time Online Presence Detection
**Status:** ✅ Merged to Main  
**Date:** October 21, 2025  
**Time Spent:** ~45 minutes

**Features Implemented:**
- ✅ Automatic online/offline status updates when app opens/closes
- ✅ Real-time status indicators update across all devices
- ✅ Green dot appears next to online users in conversation list
- ✅ Status updates within 1-2 minutes when user disconnects
- ✅ App lifecycle detection (foreground/background)

**Technical Completed:**
- Added ProcessLifecycleOwner observer in GChatApplication
- DefaultLifecycleObserver monitors onStart/onStop lifecycle events
- Updates online status to true when app comes to foreground
- Updates online status to false when app goes to background
- ConversationListViewModel observes real-time user status changes via Firestore
- UserRepositoryImpl.getUserFlow() now observes Firestore (not just local DB)
- User flow cache prevents duplicate listeners for same users
- Added androidx.lifecycle:lifecycle-process:2.6.2 dependency

**Bugs Fixed:**
- Fixed online indicators not updating when users disconnect
- Fixed visual bug where white background covered green indicator (replaced .background with .border)
- Fixed login not updating online status in Firestore and local cache

**How It Works:**
1. ProcessLifecycleOwner detects app state changes (foreground/background)
2. When app opens → set isOnline = true in Firestore + local DB
3. When app closes → set isOnline = false in Firestore + local DB
4. ConversationListViewModel observes each user via Firestore snapshot listeners
5. UI updates automatically when any user's status changes
6. Efficient caching prevents duplicate listeners

---

### PR #5: Group Chat
**Status:** ✅ Merged to Main  
**Date:** October 20, 2025  
**Time Spent:** ~4 hours

**Features Implemented:**
- ✅ Create group conversations with 3-50 participants
- ✅ Group name and custom group icon/avatar upload
- ✅ Group admin roles (creator is initial admin)
- ✅ Sender names displayed in group message bubbles
- ✅ FAB menu with "New Group" option in conversation list
- ✅ Conversation list correctly displays group name and icon
- ✅ CreateGroupScreen with participant search and selection
- ✅ Real-time user search with add/remove participants

**Technical Completed:**
- CreateGroupUseCase with validation (3-50 participants, creator as admin)
- CreateGroupScreen and CreateGroupViewModel
- Updated Conversation model with groupAdmins field
- Database schema v3 (added groupAdmins to ConversationEntity)
- Firestore rules: group validation, admin-only updates, participant limits
- Firebase Storage rules: group_icons path with 5MB limit
- ChatViewModel exposes conversation and participantUsers for sender display
- Navigation: CreateGroup route integrated

**Bugs Fixed:**
- Fixed Room schema export warning (set exportSchema = false)
- Fixed CreateGroupViewModel coroutine issue (getCurrentUserId in suspend scope)
- Fixed last message preview not loading initially in conversation list

**Deferred to Future PRs:**
- GroupInfoScreen (view/edit group, manage members) → PR #5.6 or #6
- Message history filtering for new members → PR #5.5 (privacy feature)
- Participant count badge in conversation list
- Leave group functionality
- Add/remove participants after creation

---

### PR #4.5: Image Upload UI with Caption Support
**Status:** ✅ Merged to Main  
**Date:** October 20, 2025

**Features Added:**
- Complete image upload UI with attachment button and picker bottom sheet
- Image preview with caption support before sending
- Camera and gallery integration with permission handling
- Remove/cancel selected image button
- Upload progress indicator (circular progress during upload)
- Error handling with Snackbar notifications
- Smart placeholder text ("Add a caption..." when image selected)

**User Experience:**
- Tap attachment button → Choose Camera or Gallery
- Image preview appears above text input
- Add optional caption text with image
- X button to remove image if needed
- Send button enabled with image (text optional)
- Upload progress shows during Firebase Storage upload
- Text field clears after sending

**Technical Implementation:**
- ChatViewModel: `sendImageMessage()` with caption parameter
- MessageInput: Image preview, caption support, remove button
- ImagePickerBottomSheet: Beautiful UI for source selection
- Camera permission handling with Accompanist Permissions
- Firebase Storage rules deployed for `chat_images/{userId}/` path
- Image compression handled by MediaRepositoryImpl

**Bugs Fixed:**
- Fixed uploadImage parameter order (Uri, path)
- Added camera permission request before launching camera
- Clear text field after sending image with caption

---

### PR #4: Media Sharing & Core Messaging Fixes
**Status:** ✅ Merged to Main  
**Date:** October 20, 2025

**Features Added:**
- ProfilePicture composable with online status indicator
- Image message display infrastructure (ImageMessageBubble, ImageViewerScreen)
- MediaRepository with Firebase Storage integration
- Message preview in conversation list shows latest message text

**Bugs Fixed:**
- **CRITICAL:** Fixed CASCADE delete bug that was deleting all messages when conversation updated
- Fixed message ordering (now shows oldest → newest, proper chat convention)
- Fixed Firestore rules to allow messages with optional text (for image messages)
- Fixed message bubbles color (darker gray for received messages)
- Fixed message preview showing "No messages yet" for active conversations
- Fixed StateFlow timeout causing messages to briefly appear then disappear

**Technical Changes:**
- Removed foreign key constraint from MessageEntity (prevented cascade deletion)
- Database version bumped from 1 to 2
- Updated message ordering: `ORDER BY timestamp ASC`
- Enhanced ConversationMapper to parse lastMessage from Firestore
- Firebase Storage security rules with 5MB image limit
- Image compression utility in MediaRepositoryImpl
- FileProvider configuration for camera capture
- Added required permissions (CAMERA, READ_MEDIA_IMAGES)

**Note:** Image upload UI not yet wired to ChatScreen - infrastructure ready for future PR.

---

### PR #3: New Conversation Flow
**Status:** ✅ Merged  
**Date:** October 20, 2025

**Features Added:**
- User search functionality (search by email)
- New conversation screen with real-time search
- User selection to start conversations
- Online status indicators in search results

**Technical Implementation:**
- Added `searchUsers()` to UserRepository with Firestore integration
- Created NewConversationScreen and NewConversationViewModel
- Updated Firestore security rules for user list queries
- Proper navigation flow (conversation list → search → chat)
- Search limits: 2+ characters, 20 results max, excludes current user

---

### PR #2: Logout Functionality & Firestore Listener Fixes
**Status:** ✅ Merged  
**Date:** October 20, 2025

**Features Added:**
- Logout button in ConversationListScreen TopBar
- Clean logout with navigation back to login screen

**Bugs Fixed:**
- Fixed app crash on logout due to active Firestore listeners
- Fixed PERMISSION_DENIED errors after logout
- Fixed infinite loading spinner on login

**Technical Changes:**
- Added error handling in repositories for Firestore sync operations
- Updated StateFlow timeout to 0 for immediate listener cleanup
- Restored comprehensive Firestore security rules

---

### PR #1: Initial MVP - Authentication & Messaging
**Status:** ✅ Merged  
**Date:** October 20, 2025

**Features Added:**
- Email/password authentication
- Google Sign-In integration
- One-on-one real-time messaging
- Offline-first architecture with Room database
- Message status tracking (sending, sent, delivered, read)
- User profiles with Firestore sync

**Technical Setup:**
- Clean Architecture (Domain, Data, Presentation)
- Firebase integration (Auth, Firestore, Storage, FCM)
- Jetpack Compose UI with Material 3
- Hilt dependency injection with KSP
- Room local persistence
- Firestore security rules and indexes

---

## Executive Summary

**gChat** is an Android-native messaging application designed to compete with Apple's iMessage while solving a critical gap: seamless international communication. We combine iMessage-level polish with AI-powered real-time translation.

**Target Persona:** International Communicator  
**Primary Competition:** Apple iMessage  
**Unique Value Proposition:** Native Android messaging with iMessage-quality UX + AI-powered multilingual communication

---

## Product Vision

### Mission Statement
Eliminate language barriers in digital communication by providing an Android-native messaging experience that rivals iMessage in quality while enabling effortless conversations across languages.

### Why gChat?

**The Android Messaging Problem:**
- No unified, high-quality first-party messaging solution
- Fragmented ecosystem (SMS, RCS, WhatsApp, Telegram)
- Poor cross-platform experience with iOS users
- No native AI-powered translation features

**The International Communication Problem:**
- 1.5 billion people regularly communicate across language barriers
- Existing solutions require copy-paste workflows
- Translation apps lack conversational context
- Cultural nuances and formality often lost in translation

---

## MVP Features ✅ **COMPLETED**

### ✅ Core Messaging Infrastructure

#### Authentication & User Management
- [x] **Email & Password Authentication** - Firebase Auth integration
- [x] **Google Sign-In** - One-tap authentication with Google accounts
- [x] **User Profiles** - Display name, email, profile picture support
- [x] **Auth State Persistence** - Stay logged in across app restarts
- [x] **Logout Functionality** - Clean logout with navigation and state management

#### Real-Time Messaging
- [x] **One-on-One Chat** - Send and receive text messages
- [x] **Real-Time Delivery** - Sub-second message latency
- [x] **Message Persistence** - Room database for offline storage
- [x] **Optimistic UI Updates** - Instant message appearance
- [x] **Message Status** - Sending → Sent → Delivered → Read states
- [x] **Typing Indicators** - "User is typing..." real-time status
- [x] **Read Receipts** - Track when messages are read
- [x] **Online/Offline Status** - Real-time presence indicators

#### Offline Support
- [x] **Offline-First Architecture** - Room database as source of truth
- [x] **Message Queue** - Queue messages sent while offline
- [x] **Auto-Sync** - Automatic sync when connection restored
- [x] **View History Offline** - Access all past messages without internet

#### Technical Infrastructure
- [x] **Clean Architecture** - Domain, Data, Presentation layers
- [x] **Dependency Injection** - Hilt with KSP annotation processing
- [x] **Modern UI** - Jetpack Compose with Material 3
- [x] **Firebase Integration** - Firestore, Auth, Storage, FCM ready
- [x] **Security** - Firestore security rules, proper gitignore

---

## 🎯 MVP Roadmap - GitHub Issues

> These features are built and ready. Create GitHub Issues/PRs for enhancements.

### Issue #1: Group Chat Implementation
**Priority:** High  
**Status:** Not Started  
**Description:** Enable 3+ participant conversations with group message attribution

**Acceptance Criteria:**
- [ ] Create group conversations UI
- [ ] Support 3+ participants
- [ ] Group name and profile picture
- [ ] Add/remove participants
- [ ] Group message attribution (show sender)
- [ ] Group read receipts (show read counts)

**Technical Requirements:**
- Update Firestore schema for group conversations
- Modify `Conversation` model for multiple participants
- Create group creation/management UI
- Update message UI to show sender names in groups

---

### Issue #2: Media Sharing (Images)
**Priority:** High  
**Status:** Not Started  
**Description:** Send and receive photos in conversations

**Acceptance Criteria:**
- [ ] Image picker (camera + gallery)
- [ ] Upload to Firebase Storage
- [ ] Display images in message bubbles
- [ ] Image preview/fullscreen view
- [ ] Loading indicators during upload
- [ ] Image compression optimization
- [ ] Profile picture upload

**Technical Requirements:**
- Firebase Storage integration
- Image loading with Coil
- Implement `MediaUrl` support in messages
- Create image viewer component

---

### Issue #3: Push Notifications (FCM)
**Priority:** High  
**Status:** Partially Complete  
**Description:** Receive notifications for new messages when app is background/closed

**Acceptance Criteria:**
- [ ] Request notification permissions
- [ ] Handle FCM token registration
- [ ] Cloud Function to send notifications on new messages
- [ ] Notification content preview
- [ ] Deep linking (tap notification → open chat)
- [ ] Foreground and background notifications
- [ ] Handle notifications when app is killed

**Technical Requirements:**
- FCM setup in Firebase Console
- `MessagingService` implementation (already created)
- Cloud Function trigger on message creation
- Notification channel configuration

---

### Issue #4: New Conversation Flow
**Priority:** Medium  
**Status:** Not Started  
**Description:** UI to start new conversations with contacts

**Acceptance Criteria:**
- [ ] Contact selection screen
- [ ] Search contacts by name/email
- [ ] Create new conversation button
- [ ] Handle first message in new conversation
- [ ] Empty state for no conversations

**Technical Requirements:**
- Create contact selection UI
- Implement conversation creation logic
- Update navigation flow

---

### Issue #5: Message Delivery Optimization
**Priority:** Medium  
**Status:** Not Started  
**Description:** Optimize message sync and reduce Firebase costs

**Acceptance Criteria:**
- [ ] Implement aggressive caching
- [ ] Batch read receipts updates
- [ ] Optimize Firestore queries
- [ ] Add composite indexes
- [ ] Monitor Firebase usage

**Technical Requirements:**
- Review Firestore read/write patterns
- Add caching layer in repositories
- Create Firestore composite indexes

---

## 🌟 Future Features Coming Soon

> Post-MVP features. Do not create PRs for these yet.

### Phase 2: AI Translation Features

#### Real-Time Translation (Inline)
- Tap message to translate to preferred language
- Translation appears below original text
- <500ms translation speed
- Cache translations to reduce API costs
- Support 20+ languages

#### Language Detection & Auto-Translate
- Automatic language detection
- Per-conversation auto-translate toggle
- User preferred language settings
- Smart detection (skip if same language)

#### Cultural Context Hints
- Detect idioms, slang, cultural references
- Provide explanations for non-native speakers
- Example: "Break a leg" → "Good luck idiom"

#### Formality Level Adjustment
- Adjust tone for business vs casual contexts
- Japanese keigo support
- Formal/casual toggle before sending

#### Slang/Idiom Explanations
- Explain Gen Z slang, memes, regional dialect
- "That's cap" → "That's a lie (American slang)"
- Provide usage examples and context

#### Context-Aware Smart Replies
- Generate 3 reply suggestions
- Match user's communication style
- Multi-language support
- RAG pipeline for conversation history
- Learn tone, emoji usage, phrasing patterns

### Phase 3: Advanced Messaging

- Voice messages with transcription
- Video calls with live caption translation
- Emoji reactions (iMessage-style tapback)
- Message editing and deletion
- End-to-end encryption (E2EE)
- Desktop companion app (web or native)

### Phase 4: Platform Expansion

- Voice-to-text with auto-translate
- Image translation (OCR + translate text in photos)
- Conversation summarization
- Proactive language learning suggestions
- AI pronunciation guide
- RCS fallback for SMS users
- Contact sync and discovery
- Encrypted cloud backup
- Themes and customization
- Stickers and GIF integration

---

## Target Persona: International Communicator

### User Profile
**Demographics:**
- Age: 25-45
- Location: Urban areas, international hubs
- Language: Multilingual or frequent cross-language communication
- Devices: Android smartphone (primary device)

**Characteristics:**
- Has family/friends/colleagues who speak different languages
- Travels internationally or works with global teams
- Frustrated by language barriers in daily communication
- Values authentic, natural conversation over robotic translation
- Currently uses multiple apps (WhatsApp + Google Translate, etc.)

### Pain Points
1. **Language Barriers:** Constantly switching between messaging and translation apps
2. **Translation Nuances:** Generic translations miss context, tone, and cultural meaning
3. **Copy-Paste Overhead:** Manual workflow disrupts conversation flow
4. **Learning Difficulty:** Hard to learn phrases in context during conversations
5. **Formality Concerns:** Uncertain about appropriate formality levels

---

## Competitive Analysis

### iMessage (Primary Benchmark)
**Strengths:**
- Seamless real-time sync across Apple devices
- Read receipts, typing indicators, reactions
- Excellent offline support
- Rich media support
- E2E encryption
- 99.9% uptime

**Weaknesses:**
- iOS/macOS exclusive
- No translation features
- Limited cross-platform support
- No AI-powered features

**gChat's Differentiation:**
- ✅ Match iMessage reliability and polish
- ✅ Add AI translation (iMessage doesn't have)
- ✅ Native Android experience
- ✅ Context-aware smart replies
- ✅ Cultural context and formality guidance

---

## Technical Stack

### Mobile (Android)
- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** Clean Architecture (Domain, Data, Presentation)
- **Dependency Injection:** Hilt with KSP
- **Local Database:** Room (SQLite wrapper)
- **Image Loading:** Coil
- **Navigation:** Compose Navigation
- **Reactive Programming:** Kotlin Coroutines + Flow

### Backend (Firebase)
- **Database:** Cloud Firestore (real-time NoSQL)
- **Authentication:** Firebase Authentication
- **Storage:** Firebase Cloud Storage
- **Notifications:** Firebase Cloud Messaging (FCM)
- **Functions:** Cloud Functions for Firebase (Node.js/TypeScript)

### AI Integration (Future)
- **LLM Provider:** OpenAI GPT-4 or Anthropic Claude
- **Translation API:** OpenAI GPT-4 or DeepL API
- **Vector Database:** Firestore Vector Search or Pinecone
- **Agent Framework:** AI SDK by Vercel or LangChain

---

## Success Metrics

### MVP Metrics (Current)
- **Reliability:** 99% message delivery success rate
- **Performance:** <1s message latency
- **Stability:** 0 crashes during testing
- **Offline Support:** 100% message queue and sync

### AI Feature Metrics (Future)
- **Translation Quality:** >90% user satisfaction
- **Translation Speed:** <500ms for inline translation
- **Smart Reply Usage:** >40% adoption rate
- **Cultural Context:** >80% of idioms detected

### Product-Market Fit (Long-term)
- **User Retention:** >60% DAU/MAU ratio
- **Engagement:** Average 50+ messages per user per day
- **Language Coverage:** Top 20 languages by volume
- **Growth:** 20% MoM user growth

---

## Risk Mitigation

### Technical Risks
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Firebase costs exceed budget | High | Aggressive caching, batch requests, monitor usage |
| OpenAI API rate limits | High | Retry logic, DeepL fallback, queue requests |
| Poor network reliability | Medium | Offline-first architecture, local caching |
| Real-time sync conflicts | Medium | Firestore transactions, conflict resolution |

### Product Risks
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Users don't trust AI translation | High | Always show original text, build trust |
| Privacy concerns with AI | High | Clear data policy, local processing |
| iMessage users won't switch | High | Focus on Android + international users |

---

## Launch Strategy

### MVP Launch (Current)
- **Target Audience:** Friends, family, and test users
- **Distribution:** APK direct download + internal testing
- **Goal:** Validate core messaging reliability

### Alpha Launch (Week 2-4)
- **Target Audience:** 100 beta testers (international students, expats)
- **Distribution:** Google Play Internal Testing
- **Goal:** Stress test at scale, refine AI features

### Beta Launch (Month 2)
- **Target Audience:** 1,000 users via Product Hunt, Reddit
- **Distribution:** Google Play Open Beta
- **Goal:** Product-market fit validation

### Public Launch (Month 3)
- **Target Audience:** General public (Android users)
- **Distribution:** Google Play Store
- **Goal:** Scale to 10K users

---

## Conclusion

gChat addresses two critical gaps:

1. **Android lacks a native iMessage competitor** with equivalent quality
2. **International communicators struggle** with language barriers

By combining iMessage-level polish with AI-powered translation, gChat can become the default messaging app for 1.5 billion people who communicate across languages.

**The MVP foundation is complete. Now we build the features that make us unique.**

---

*Last Updated: October 2025*  
*Version: 1.0 (MVP Complete)*
