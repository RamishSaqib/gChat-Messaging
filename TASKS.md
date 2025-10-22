# gChat - Development Tasks

> **Last Merged:** PR #13 - Inline Translation UI | **Status:** 🎉 First AI Feature Complete!

---

## 📊 Quick Status

**Completed PRs:** 13 (Merged to main)  
**Current PR:** None  
**Current Sprint:** AI Translation MVP Complete ✅  
**Next Up:** Additional AI Features (Smart Replies, Cultural Context, etc.)

---

## ✅ PR #13: Inline Translation UI (MERGED ✅)

**Goal:** Implement inline message translation with caching and rate limiting

**Branch:** `feature/pr13-inline-translation` → **Merged to `main`**

**Status:** ✅ Merged

**Priority:** High (First AI Feature - Primary Selling Point)

**Time Spent:** ~4 hours

### Features Implemented
- [x] Long-press message to show translation menu
- [x] Language selector dialog with 20+ languages
- [x] Inline translation display below original message
- [x] Translation loading indicator
- [x] Error handling with retry button
- [x] Hide translation button to dismiss
- [x] Translation caching in Firestore (30-day TTL)
- [x] Local Room database cache for offline access
- [x] Rate limiting (100 translations/hour, 200 language detections/hour)
- [x] Works for both sent and received messages

### Technical Implementation

**Backend (Firebase Functions):**
- [x] `translateMessage` Cloud Function with OpenAI GPT-4
- [x] Automatic source language detection
- [x] SHA-256 cache key generation
- [x] Rate limiting with Firestore tracking
- [x] Environment-based configuration

**Android App:**
- [x] `Translation` domain model with 20+ language support
- [x] `TranslationRepository` with dual caching (Firestore + Room)
- [x] Room database v9 migration for translation cache
- [x] `TranslateMessageUseCase` for clean architecture
- [x] UI components: `LanguageSelectorDialog`, `TranslationDisplay`, `TranslationLoading`, `TranslationError`
- [x] Long-press gesture detection in `MessageBubble`
- [x] ViewModel state management for translations, loading, and errors

### Files Created
- `app/src/main/java/com/gchat/domain/model/Translation.kt`
- `app/src/main/java/com/gchat/domain/repository/TranslationRepository.kt`
- `app/src/main/java/com/gchat/domain/usecase/TranslateMessageUseCase.kt`
- `app/src/main/java/com/gchat/data/local/entity/TranslationEntity.kt`
- `app/src/main/java/com/gchat/data/local/dao/TranslationDao.kt`
- `app/src/main/java/com/gchat/data/mapper/TranslationMapper.kt`
- `app/src/main/java/com/gchat/data/remote/firebase/FirebaseTranslationDataSource.kt`
- `app/src/main/java/com/gchat/data/repository/TranslationRepositoryImpl.kt`
- `app/src/main/java/com/gchat/presentation/chat/TranslationComponents.kt`

### Files Modified
- `app/src/main/java/com/gchat/data/local/AppDatabase.kt` (v8 → v9, v9 → v10)
- `app/src/main/java/com/gchat/data/local/entity/ConversationEntity.kt` (added type/mediaUrl)
- `app/src/main/java/com/gchat/data/local/dao/ConversationDao.kt` (update SQL)
- `app/src/main/java/com/gchat/data/repository/ConversationRepositoryImpl.kt` (read type/mediaUrl from entity)
- `app/src/main/java/com/gchat/data/mapper/ConversationMapper.kt` (map type/mediaUrl)
- `app/src/main/java/com/gchat/data/remote/firestore/FirestoreConversationDataSource.kt` (save type/mediaUrl)
- `app/src/main/java/com/gchat/domain/repository/ConversationRepository.kt` (add params)
- `app/src/main/java/com/gchat/domain/usecase/SendMessageUseCase.kt` (pass type/mediaUrl)
- `app/src/main/java/com/gchat/presentation/chat/ChatScreen.kt` (translation UI)
- `app/src/main/java/com/gchat/presentation/chat/ChatViewModel.kt` (translation state)
- `app/src/main/java/com/gchat/presentation/chat/ConversationListScreen.kt` (preview text)
- `app/src/main/java/com/gchat/di/RepositoryModule.kt` (bind TranslationRepository)
- `app/src/main/java/com/gchat/di/AppModule.kt` (provide TranslationDao)
- `firebase/firestore.rules` (allow text=null, add translations/rateLimits rules)

### Critical Bugs Fixed
1. **Image Messages Not Syncing** - Firestore rules rejected `text = null`, fixed validation
2. **Conversation Preview Showing "New Message"** - Repository hardcoded type/mediaUrl, now reads from entity
3. **Translation Loading Not Showing** - UI wasn't collecting state flows, added `collectAsState()`
4. **Image-Only Message Sync Failure** - UseCase passed empty string instead of null, fixed

### Testing
- [x] Long-press text message shows language selector
- [x] Select language triggers translation
- [x] Loading indicator appears during translation
- [x] Translation displays below original message
- [x] Hide button removes translation
- [x] Retry button works on error
- [x] Translation caches (instant second time)
- [x] Rate limiting shows friendly error
- [x] Both sender and receiver messages translate
- [x] Image messages don't show translation menu
- [x] Conversation preview shows "📷 Image" for image-only messages

---

## ✅ PR #12: Backend Infrastructure & Translation API (MERGED ✅)

**Goal:** Set up OpenAI integration and translation backend

**Branch:** `feature/pr12-translation-backend` → **Merged to `main`**

**Status:** ✅ Merged

**Priority:** High (Required for PR #13)

**Time Spent:** ~2 hours

### Features Implemented
- [x] OpenAI GPT-4 integration
- [x] `translateMessage` callable function
- [x] `detectLanguage` callable function
- [x] Translation caching with SHA-256 keys
- [x] Rate limiting infrastructure
- [x] Centralized OpenAI client
- [x] Environment variable management

### Files Created
- `firebase/functions/src/utils/openai.ts` - OpenAI client and config
- `firebase/functions/src/utils/cache.ts` - Translation caching
- `firebase/functions/src/utils/rateLimit.ts` - Rate limiting
- `firebase/functions/AI_SETUP.md` - Setup documentation
- `firebase/functions/ENV_SETUP.md` - Environment variables

### Files Modified
- `firebase/functions/src/ai/translation.ts` - Refactored with new utilities
- `firebase/functions/src/index.ts` - Export new functions
- `firebase/functions/package.json` - Added OpenAI dependency
- `firebase/firestore.rules` - Added translations and rateLimits rules

### Testing
- [x] Translation API called from Android
- [x] Cache working (instant second request)
- [x] Rate limiting triggers at 100/hour
- [x] Language detection working
- [x] Firestore rules allow authenticated reads
- [x] Cloud Functions deployed successfully

---

## ✅ PR #11: Online Status Accuracy Fix (MERGED ✅)

**Goal:** Fix online indicators showing stale "online" status for force-killed users

**Branch:** `feature/pr11-online-status-fix` → **Merged to `main`**

**Status:** ✅ Merged

**Priority:** High (Bug Fix)

**Time Spent:** ~1 hour

### Problem
When emulators/apps were force-killed, the `onStop()` lifecycle callback never fired, leaving users with `isOnline = true` in Firestore indefinitely. Other users would see them as online even though they weren't running the app.

### Solution Implemented
Implemented heartbeat-based presence detection with client-side validation:
1. Added 60-second heartbeat to update `lastSeen` while app is in foreground
2. Created `isActuallyOnline()` helper that checks BOTH `isOnline = true` AND `lastSeen` within 2 minutes
3. Updated all UI to use `isActuallyOnline()` instead of raw `isOnline` flag
4. Changed registration to set `isOnline = false` (app lifecycle sets it true on first launch)

### Core Changes
- [x] Add heartbeat mechanism to GChatApplication (updates every 60 seconds)
- [x] Add `isActuallyOnline()` helper function to User model
- [x] Update ConversationListScreen to use new logic
- [x] Update ChatScreen to use new logic
- [x] Update DMInfoScreen to use new logic
- [x] Update GroupInfoScreen to use new logic
- [x] Update NewConversationScreen to use new logic
- [x] Fix registration to set `isOnline = false` for new users
- [x] Fix Google Sign-In to set `isOnline = false` for new users

### Technical Implementation
- Added `heartbeatJob` coroutine that runs while app is in foreground
- Heartbeat updates `lastSeen` timestamp every 60 seconds via `updateOnlineStatus()`
- `isActuallyOnline()` returns true only if `isOnline = true` AND `lastSeen > currentTime - 2 minutes`
- Cancelled heartbeat in `onStop()` lifecycle callback
- Updated 7 locations in presentation layer to use `isActuallyOnline()`

### Files Modified
- `GChatApplication.kt` - Added heartbeat mechanism
- `User.kt` - Added `isActuallyOnline()` helper
- `ConversationListScreen.kt` - Use `isActuallyOnline()`
- `ChatScreen.kt` - Use `isActuallyOnline()`
- `DMInfoScreen.kt` - Use `isActuallyOnline()` (3 locations)
- `GroupInfoScreen.kt` - Use `isActuallyOnline()`
- `NewConversationScreen.kt` - Use `isActuallyOnline()`
- `AuthRepositoryImpl.kt` - Set `isOnline = false` for new users (2 locations)

### Expected Behavior After Fix
1. **App Running:** Heartbeat updates `lastSeen` every 60 seconds, user shows as online
2. **Force Kill:** After 2 minutes of no heartbeat, user automatically appears offline to others
3. **Proper Close:** `onStop()` sets `isOnline = false` immediately
4. **Reopen App:** `onStart()` sets `isOnline = true` and restarts heartbeat

### Testing
- [x] Test force-kill scenario (user appears offline after 2 minutes)
- [x] Test proper app close (user appears offline immediately)
- [x] Test app reopen (user appears online immediately)
- [x] Test heartbeat updates lastSeen every 60 seconds
- [x] Test new user registration (starts offline, becomes online on first launch)
- [x] Verify all UI locations show correct online status

---

## ✅ PR #10: Nickname System (MERGED ✅)

**Goal:** Implement per-conversation nickname system for personalizing how users appear

**Branch:** `feature/pr10-nickname-system` → **Merged to `main`**

**Status:** ✅ Merged

**Priority:** Medium (UX Enhancement)

**Time Spent:** ~2 hours

### Core Nickname System
- [x] Add `nicknames` map to Conversation model (userId → nickname)
- [x] Add `getUserDisplayName()` helper method to Conversation
- [x] Update ConversationEntity with nicknames field
- [x] Migrate Room database to version 6
- [x] Add `setNickname()` to ConversationRepository

### Group Chat Nicknames
- [x] Add "Change Nickname" option to GroupInfoScreen member menu
- [x] Create nickname dialog UI (input, save, remove, cancel)
- [x] Add `getCurrentNickname()` to GroupInfoViewModel
- [x] Add `setNickname()` to GroupInfoViewModel
- [x] Update member list to display nicknames

### DM Nicknames
- [x] Add "Change Nickname" menu item to DMInfoScreen
- [x] Create nickname dialog UI for DMs
- [x] Add nickname methods to DMInfoViewModel
- [x] Update DM info display to show nicknames

### Display Nicknames Everywhere
- [x] Update ChatScreen message bubbles to show nicknames
- [x] Update ChatScreen TopBar to show nickname for DMs
- [x] Update typing indicator text to use nicknames
- [x] Update ConversationListScreen titles to show nicknames
- [x] Update message preview formatting for clean display

### Conversation List Integration
- [x] Update ConversationWithUser to include lastMessageSender
- [x] Fetch lastMessageSender User object for nickname lookup
- [x] Create buildLastMessageText() helper for smart formatting
- [x] Handle group vs DM message preview differences

### Push Notifications
- [x] Update Cloud Functions to retrieve nicknames map
- [x] Use nickname for notification title/body in sendMessageNotification
- [x] Deploy updated Cloud Functions

### Firestore Rules
- [x] Allow participants to update nicknames field in conversations

### Bug Fixes
- [x] Fix nickname not showing in conversation list title
- [x] Fix nickname not showing in notifications
- [x] Fix notification click navigation (MainActivity intent handling)
- [x] Fix message preview showing duplicate names in DMs
- [x] Fix val reassignment errors in GroupInfoScreen
- [x] Add missing User import in ConversationListScreen

### Testing
- [x] Test setting nickname in group chat
- [x] Test removing nickname in group chat
- [x] Test setting nickname in DM
- [x] Test removing nickname in DM
- [x] Test nickname appears in messages
- [x] Test nickname appears in typing indicators
- [x] Test nickname appears in conversation list
- [x] Test nickname appears in notifications
- [x] Test notification click navigation works correctly
- [x] Test fallback to display name when no nickname

---

## ✅ PR #9: Profile & Group Management (MERGED ✅)

**Goal:** Implement user profile editing, group management, and enhanced TopBar UI

**Branch:** `feature/pr9-profile-group-management` → **Merged to `main`**

**Status:** ✅ Merged

**Priority:** High (Core MVP Feature)

**Time Spent:** ~2.5 hours

### User Profile
- [x] Add profile picture to ConversationListScreen TopBar
- [x] Create ProfileScreen with display name and picture editing
- [x] Create ProfileViewModel for profile updates
- [x] Add navigation route for ProfileScreen
- [x] Clickable profile picture in TopBar navigates to ProfileScreen
- [x] Image upload UI (camera/gallery picker)
- [x] Upload progress indicator
- [x] Update Firestore storage rules for profile_pictures/

### Group Info
- [x] Create GroupInfoScreen UI (icon, name, members list)
- [x] Create GroupInfoViewModel
- [x] Add member management (add/remove/promote)
- [x] Add group name editing
- [x] Add group icon editing
- [x] Add leave group functionality
- [x] Wire GroupInfoScreen to ChatScreen TopBar
- [x] Show "(Admin)" badge next to admin names
- [x] Admin-only permissions for add/remove members
- [x] Anyone can change group name/icon
- [x] Update Firestore storage rules for group_icons/

### DM Info
- [x] Create DMInfoScreen
- [x] Wire DMInfoScreen to ChatScreen TopBar
- [x] Show other user's profile picture and online status
- [x] Display last seen timestamp

### Repository Extensions
- [x] Add updateGroupName() to ConversationRepository
- [x] Add updateGroupIcon() to ConversationRepository
- [x] Add addParticipants() to ConversationRepository
- [x] Add removeParticipant() to ConversationRepository
- [x] Add promoteToAdmin() to ConversationRepository
- [x] Add leaveGroup() to ConversationRepository

### UI Enhancements
- [x] Add profile picture to ChatScreen TopBar (with name)
- [x] Show online indicator for 1-on-1 chats in TopBar
- [x] Redesign ConversationListScreen TopBar layout
- [x] Move profile picture to left side of TopBar
- [x] Center "gChat" title using CenterAlignedTopAppBar
- [x] Keep logout button on right

### Code Quality
- [x] Refactor GroupInfoContent to reduce complexity
- [x] Extract GroupIconSection composable
- [x] Extract GroupNameSection composable
- [x] Extract MembersSectionHeader composable
- [x] Fix val reassignment errors with proper callbacks

### Testing
- [x] Test profile picture upload (camera and gallery)
- [x] Test group name and icon editing
- [x] Test adding/removing members (admin only)
- [x] Test promoting users to admin
- [x] Test leaving a group
- [x] Verify online status in chat TopBar
- [x] Verify centered title in main screen

---

## ✅ PR #8: Typing Indicators (MERGED ✅)

**Goal:** Implement real-time typing indicators showing who is typing in 1-on-1 and group chats

**Branch:** `feature/pr8-typing-indicators` → **Merged to `main`**

**Status:** ✅ Merged

**Priority:** Medium (UX Enhancement)

**Time Spent:** ~1.5 hours

### Feature Tasks
- [x] Create TypingIndicator domain model (conversationId, userId, isTyping, timestamp)
- [x] Create FirestoreTypingDataSource for real-time typing updates
- [x] Create TypingRepository interface and implementation
- [x] Add TypingRepository binding to DI module
- [x] Inject TypingRepository into ChatViewModel
- [x] Implement typing status tracking in updateMessageText()
- [x] Add debouncing logic (3-second timeout for inactivity)
- [x] Clear typing status on message send
- [x] Create typingIndicatorText StateFlow with smart formatting
- [x] Filter out current user from typing indicators

### UI Components
- [x] Create TypingIndicator composable with translucent bubble
- [x] Create TypingDots composable with animated fade effect
- [x] Create TypingDot composable for individual dot animation
- [x] Add typing indicator to ChatScreen LazyColumn
- [x] Collect typingIndicatorText StateFlow in ChatScreen
- [x] Show typing indicator at bottom of message list
- [x] Add animation imports (rememberInfiniteTransition, animateFloat)

### Group Chat Formatting
- [x] Format 1 typer: "John is typing..."
- [x] Format 2 typers: "John and Sarah are typing..."
- [x] Format 3+ typers: "John, Sarah, and 2 others are typing..."
- [x] Fetch user display names from participantUsers map

### Data Layer
- [x] Firestore rules already support /typing/{userId} subcollection
- [x] Participants can read and write their own typing status
- [x] Real-time snapshot listener in FirestoreTypingDataSource
- [x] Efficient Firestore writes with debouncing

### Testing
- [x] Test typing indicator in 1-on-1 chat (Device A types, Device B sees indicator)
- [x] Test 3-second debounce (stop typing, indicator disappears after 3s)
- [x] Test immediate clear on send (send message, indicator disappears)
- [x] Test group chat with multiple typers (show all names)
- [x] Test typing indicator auto-scrolls to bottom
- [x] Verify Firestore writes are debounced (not excessive)

### Bugs Fixed During Development
- [x] Fixed typing indicators not showing in 1-on-1 chats (populated participantUsers for all chats)
- [x] Fixed race condition with `.map()` snapshot access (changed to `combine()` for reactive updates)

---

## ✅ PR #6: Read Receipts (MERGED ✅)

**Goal:** Implement read receipts with checkmarks for 1-on-1 chats and profile avatars for group chats

**Status:** ✅ Merged to `main`

**Priority:** High (User Experience Enhancement)

**Time Spent:** ~1.5 hours

### Feature Tasks
- [x] Update Message model to include readBy map (userId → timestamp)
- [x] Add helper functions: isReadBy(), getReadTimestamp(), isReadByAll(), isReadByAny()
- [x] Update MessageEntity and mapper for read status persistence
- [x] Create MarkMessageAsReadUseCase for marking messages as read
- [x] Update ChatViewModel to mark messages as read when viewed
- [x] Implement markAllMessagesAsRead() to auto-mark on screen open

### UI Components
- [x] Create ReadReceiptCheckmarks composable for 1-on-1 chats
  - [x] Single gray checkmark (✓) for sent but not read
  - [x] Double white checkmarks (✓✓) for read
- [x] Create ReadByAvatars composable for group chats
  - [x] Show stacked profile picture bubbles
  - [x] Limit to 3 visible avatars with "+X" indicator
  - [x] Position at bottom-right next to timestamp
- [x] Update MessageBubble to conditionally show checkmarks vs avatars
- [x] Exclude sender from read indicators

### Data Layer
- [x] Update Firestore rules to allow read status updates
- [x] Implement Firestore transaction for atomic read status updates
- [x] Support both new map format and old list format (backward compatible)
- [x] Room database support for readBy map serialization

### Bug Fixes
- [x] Fix compilation errors from readBy type change (List → Map)
- [x] Update ConversationMapper to use emptyMap() instead of emptyList()
- [x] Update ConversationRepositoryImpl for all lastMessage instances
- [x] Consolidate duplicate use case files (removed plural version)
- [x] Change read checkmark color to white for better contrast

### Testing Checklist
- [x] Test 1-on-1 read receipts (checkmarks change from gray to white)
- [x] Test group chat read receipts (avatars appear as users read)
- [x] Verify real-time updates across devices
- [x] Test auto-mark as read when opening conversation

---

## 🔔 PR #7: Push Notifications (FCM) (MERGED ✅)

**Goal:** Implement Firebase Cloud Messaging for push notifications when new messages arrive

**Status:** ✅ Merged to `main`

**Priority:** High (MVP Feature)

**Time Spent:** ~3 hours

### Client-Side Tasks
- [x] Enhance MessagingService to use UserRepository
- [x] Add FCM token update in GChatApplication.onStart()
- [x] Update MessagingService.onNewToken() to use repository pattern
- [x] Improve notification display with sender name and message preview
- [x] Add support for group chat notifications
- [x] Handle notification click to navigate to conversation
- [x] Add notification permission request for Android 13+ (POST_NOTIFICATIONS)
- [x] Update MainActivity to handle notification intents
- [x] Test notification permission flow

### Server-Side Tasks (Cloud Functions)
- [x] Create firebase/functions/ directory structure
- [x] Add package.json with dependencies (firebase-admin, firebase-functions)
- [x] Create `sendMessageNotification` Cloud Function
  - [x] Trigger on new message onCreate
  - [x] Fetch conversation and participant data
  - [x] Get sender user data for display name
  - [x] Get recipient FCM tokens
  - [x] Build notification payload (title, body, data)
  - [x] Send push notification via Firebase Admin SDK
  - [x] Handle invalid tokens and cleanup
- [x] Create `deleteConversationMessages` Cloud Function (cleanup)
- [x] Create `updateUserLastSeen` Cloud Function (auto-update)
- [x] Add .gitignore for functions (never commit service keys!)
- [x] Create comprehensive README for Cloud Functions setup

### Documentation
- [x] Document Cloud Functions setup steps
- [x] Document notification payload structure
- [x] Document testing procedure
- [x] Add deployment instructions

### Deployment Steps (For User)
1. Navigate to `firebase/functions/`
2. Run `npm install`
3. Run `firebase login` (if not already logged in)
4. Run `npm run deploy`
5. Verify functions in Firebase Console

### Testing Checklist
- [x] Deploy Cloud Functions to Firebase
- [x] Send message from Device A
- [x] Verify notification appears on Device B (foreground)
- [x] Verify notification appears on Device B (background)
- [x] Verify notification appears on Device B (app killed)
- [x] Tap notification and verify app opens to correct conversation
- [x] Test group chat notifications
- [x] Test notification permission request on Android 13+

### Bug Fixes
- [x] Fixed 404 error when sending notifications (updated to FCM HTTP v1 API)
- [x] Replaced deprecated `sendToDevice` with modern `sendEach` method
- [x] Added comprehensive FCM token logging for debugging
- [x] Verified FCM tokens are correctly saved to Firestore

---

## ✅ PR #5.7: Real-Time Online Presence Detection (MERGED)

**Goal:** Implement automatic online/offline status detection with real-time updates across devices

**Status:** ✅ Merged to Main

**Time Spent:** ~45 minutes

### Tasks Completed
- [x] Add lifecycle-process dependency (androidx.lifecycle:lifecycle-process:2.6.2)
- [x] Implement ProcessLifecycleOwner observer in GChatApplication
- [x] Create DefaultLifecycleObserver for app state detection (onStart/onStop)
- [x] Update online status to true when app comes to foreground
- [x] Update online status to false when app goes to background
- [x] Modify UserRepositoryImpl.getUserFlow() to observe Firestore snapshots
- [x] Add user flow cache in ConversationListViewModel to prevent duplicate listeners
- [x] Fix visual bug in ProfilePicture.kt (white background covering green indicator)
- [x] Fix login not updating online status in Firestore and local cache
- [x] Test status updates on app open/close (~1-2 minute detection)

### Technical Implementation
- ProcessLifecycleOwner detects app lifecycle events
- onStart → sets isOnline = true in Firestore + local DB
- onStop → sets isOnline = false in Firestore + local DB
- Real-time Firestore snapshot listeners in getUserFlow()
- ConversationListViewModel combines conversations with real-time user status
- Efficient caching prevents duplicate listeners for same users
- Updates propagate to all devices within 1-2 minutes

---

## ✅ PR #5: Group Chat (MERGED)

**Goal:** Enable group conversations with 3+ participants, group names, avatars, and member management

**Status:** ✅ Merged to Main

**Priority:** High

**Deferred to Future PRs:**
- GroupInfoScreen (view/edit group details) → Will add in PR #5.5 or PR #6
- Message history filtering for new group members → PR #5.5: Group Privacy & Message Filtering

### Backend Tasks
- [x] Create `CreateGroupUseCase` for multi-participant conversations
- [x] Add group icon upload support to `MediaRepository` (already existed)
- [x] Update `ConversationRepository` to handle group-specific operations (uses existing methods)
- [ ] Add `addParticipant()` and `removeParticipant()` functions (deferred to GroupInfoScreen PR)
- [x] Update Firestore rules to validate group participant limits (3-50 members)
- [x] Add group admin role validation in security rules

### Data Models
- [x] Verify `Conversation` model supports group metadata (already has name, iconUrl, participants)
- [x] Add `groupAdmins` field to `ConversationEntity` (list of admin user IDs)
- [x] Update `ConversationMapper` to handle group admin field

### UI Components
- [x] Create `CreateGroupScreen` with name input and participant selection
- [x] Create `CreateGroupViewModel` with state management
- [ ] Create `GroupInfoScreen` for viewing/editing group details (deferred to future PR)
- [ ] Create `GroupInfoViewModel` with add/remove participant logic (deferred to future PR)
- [ ] Create `ParticipantListItem` composable for member display (deferred to future PR)
- [x] Update `ChatScreen` to show sender names in group messages
- [x] Update `ConversationItem` to display group icon and name (already worked correctly)
- [x] Add "New Group" button/option to `ConversationListScreen`

### Navigation
- [x] Add `CreateGroup` route to `Screen` sealed class
- [x] Add `GroupInfo` route with conversationId parameter
- [x] Update `NavGraph` with new routes (CreateGroup only, GroupInfo for future)
- [x] Add navigation from conversation list to create group
- [ ] Add navigation from chat TopBar to group info (deferred - needs GroupInfoScreen first)

### Group Message UX
- [x] Show sender name above/in message bubbles for group chats
- [ ] Display sender profile picture in group messages (future enhancement)
- [ ] Add participant count indicator in conversation list (future enhancement)
- [ ] Show "You:" prefix for own messages in group preview (future enhancement)

### Group Management Features
- [x] Implement group creation flow (select participants → set name → create)
- [ ] Allow group admins to edit group name and icon (deferred to GroupInfoScreen PR)
- [ ] Allow admins to add new participants (deferred to GroupInfoScreen PR)
- [ ] Allow admins to remove participants (deferred to GroupInfoScreen PR)
- [ ] Allow any member to leave group (deferred to GroupInfoScreen PR)
- [ ] Show admin badge for group admins in participant list (deferred to GroupInfoScreen PR)

### Testing Tasks
- [ ] Test creating group with 3 participants
- [ ] Test sending messages in group chat
- [ ] Test sender names display correctly
- [ ] Test Firebase security rules (group creation, admin validation)
- [ ] Test group icon upload to Firebase Storage
- [ ] Verify conversation list shows group correctly
- [ ] Test navigation flow (list → create group → chat)

---

## ✅ PR #4.5: Image Upload UI with Caption Support (MERGED)

**Goal:** Complete the image upload UI with preview, caption support, and polished UX

**Status:** ✅ Merged to Main

**Summary:**  
Implemented complete image upload flow with attachment button, camera/gallery picker, image preview before sending, optional caption support, and upload progress indicator. Users can now send images with text captions in a polished, modern messaging UX.

**Time Spent:** ~3 hours

### Implementation Tasks
- [x] Add `sendImageMessage()` to ChatViewModel with caption parameter
- [x] Update `SendMessageUseCase` to support mediaUrl parameter
- [x] Add attachment button to MessageInput
- [x] Create ImagePickerBottomSheet with Camera/Gallery options
- [x] Wire ImagePickerManager to ChatScreen
- [x] Add camera permission handling with Accompanist Permissions
- [x] Implement image preview in MessageInput (above text field)
- [x] Add remove/cancel image button (X overlay on preview)
- [x] Show upload progress indicator (CircularProgressIndicator)
- [x] Handle upload errors with Snackbar
- [x] Update placeholder text based on image selection state
- [x] Clear text field after sending image with caption
- [x] Enable send button when image selected (text optional)
- [x] Disable attachment button while image is selected

### Bugs Fixed During Development
- [x] Fixed uploadImage parameter order (Uri, path vs userId, uri, path)
- [x] Added camera permission request before launching camera (SecurityException)
- [x] Clear text field after sending image with caption

### Testing Completed
- [x] Camera capture flow (permission → capture → preview → send)
- [x] Gallery selection flow (pick → preview → send)
- [x] Image with caption (text displays in message)
- [x] Image without caption (image only message)
- [x] Remove image button (cancel selection)
- [x] Upload progress indicator
- [x] Firebase Storage upload and download
- [x] Error handling and Snackbar display

---

## ✅ PR #4: Media Sharing & Core Messaging Fixes (MERGED)

**Goal:** Enable image sharing infrastructure and fix critical messaging bugs

**Status:** ✅ Merged to Main

**Summary:**  
Fixed critical CASCADE delete bug that was deleting all messages, implemented media infrastructure (Firebase Storage, image compression, ProfilePicture component, ImageViewer), and improved UX (message ordering, preview text, bubble colors). Image upload UI deferred to future PR.

**Time Spent:** ~6 hours (4 hours features + 2 hours debugging)

### Backend Tasks
- [x] Add `mediaUrl` and `mediaType` fields to Message model (already existed)
- [x] Update Message entity and mapper for media support (already existed)
- [x] Create `MediaRepository` interface
- [x] Implement `MediaRepositoryImpl` with Firebase Storage
- [x] Add Firebase Storage upload/download functions
- [x] Update Firestore security rules for Storage access
- [x] Add image compression utility (max 1920px, <500KB)
- [x] Update User model mapper for profile picture URLs (already existed)

### Critical Bug Fixes (Completed)
- [x] **CRITICAL:** Fixed CASCADE delete removing all messages when conversation updated
- [x] Fixed message ordering (DESC → ASC for proper chat flow)
- [x] Fixed Firestore rules for optional text field in messages
- [x] Fixed message preview showing "No messages yet" 
- [x] Fixed received message bubble colors (too light)
- [x] Fixed StateFlow timeout causing messages to disappear
- [x] Database migration v1 → v2 (removed foreign key constraint)

### UI Tasks - Image Picker & Upload
- [x] Add image picker dependency (Coil, Accompanist Permissions - already existed)
- [x] Create `ImagePickerManager` for camera/gallery selection
- [x] Request runtime permissions (CAMERA, READ_MEDIA_IMAGES)
- [x] Add image compression before upload
- [ ] **DEFERRED:** Create image picker bottom sheet UI
- [ ] **DEFERRED:** Wire image picker to ChatScreen
- [ ] **DEFERRED:** Show upload progress indicator
- [ ] **DEFERRED:** Handle upload errors gracefully

### UI Tasks - Profile Pictures
- [x] Display profile pictures in conversation list with ProfilePicture composable
- [x] Add placeholder/initial letter fallback for missing pictures
- [x] Implement image caching with Coil
- [x] Show online status indicator (UI only, backend not implemented)
- [ ] **FUTURE:** Add profile picture upload to settings/profile screen
- [ ] **FUTURE:** Display profile pictures in chat screen

### UI Tasks - Message Images
- [x] Update Message model UI to support image messages
- [x] Create ImageMessageBubble component
- [x] Create fullscreen ImageViewerScreen with pinch-to-zoom
- [x] Add image viewer to navigation
- [x] Show loading skeleton while image loads
- [ ] **DEFERRED:** Add image attachment button to chat input
- [ ] **DEFERRED:** Display image thumbnails in message list
- [ ] **DEFERRED:** Handle image selection → compression → upload → send flow

### Testing (Manual)
- [x] Test message persistence (no disappearing messages)
- [x] Test message ordering (oldest to newest)
- [x] Test message preview in conversation list
- [x] Test received message bubble colors
- [ ] **DEFERRED:** Test camera image capture
- [ ] **DEFERRED:** Test gallery image selection
- [ ] **DEFERRED:** Test image upload and display

---

## ✅ PR #3: New Conversation Flow (MERGED)

**Goal:** Enable users to start new conversations with other users

**Status:** ✅ Completed (Oct 20, 2025)

### Backend Tasks
- [x] Create `UserRepository.searchUsers()` function
- [x] Add Firestore query for user search by email
- [x] Update Firestore security rules for user search (allow list queries)
- [x] Verify `CreateConversationUseCase` works (already existed)
- [x] Verify duplicate conversation check logic (already implemented)

### UI Tasks
- [x] Create `NewConversationScreen` composable
- [x] Verify "New Chat" FAB in `ConversationListScreen` (already existed)
- [x] Create user search UI with TextField
- [x] Display search results as clickable list with online indicators
- [x] Handle empty state (no users found, prompt to search)
- [x] Add loading state during search
- [x] Navigate to chat screen after conversation creation
- [x] Create `NewConversationViewModel` with search state

### Navigation
- [x] Add `NewConversation` route to `Screen` sealed class
- [x] Update `NavGraph` with new route and wire up navigation
- [x] Handle back navigation from NewConversationScreen
- [x] Proper navigation flow (list → new conversation → chat)

### Features Implemented
- Search by email with prefix matching
- Minimum 2 characters to search
- Excludes current user from results
- Limits to 20 results
- Real-time search updates
- Error handling and user feedback
- Online status indicators in search results

**Actual Time:** ~2 hours

---

## ✅ PR #2: Logout Functionality & Firestore Listener Fixes (MERGED)

**Goal:** Add logout functionality and fix app crashes on logout

**Status:** ✅ Merged (Oct 20, 2025)

### Authentication Features
- [x] Add logout function to `ConversationListViewModel`
- [x] Inject `AuthRepository` into ViewModel
- [x] Add logout button to `ConversationListScreen` TopBar
- [x] Implement logout callback with navigation

### Bug Fixes
- [x] Fix app crash on logout (Firestore listeners)
- [x] Add try-catch in `ConversationRepositoryImpl.syncConversationsFromFirestore()`
- [x] Add try-catch in `MessageRepositoryImpl.syncMessagesFromFirestore()`
- [x] Update `ConversationListViewModel` flow timeout to 0
- [x] Update `ChatViewModel` flow timeout to 0
- [x] Fix navigation to prevent crashes (popUpTo with inclusive)
- [x] Fix infinite loading spinner (add `AuthResult.Idle` state)

### Security
- [x] Restore comprehensive Firestore security rules
- [x] Add participant validation for conversations
- [x] Add sender validation for messages
- [x] Add typing indicator access controls

### Documentation
- [x] Update PRD.md with logout feature
- [x] Update TASKS.md with authentication improvements
- [x] Add PR history section to PRD.md

---

## ✅ PR #1: Initial MVP - Authentication & Messaging (MERGED)

**Goal:** Set up foundational app with authentication and real-time messaging

**Status:** ✅ Merged (Oct 20, 2025)

### Project Setup
- [x] Initialize Android project with Kotlin + Compose
- [x] Set up Firebase project and services
- [x] Configure `google-services.json`
- [x] Set up Gradle dependencies (Firebase, Room, Hilt, Coil)
- [x] Configure build variants (dev, prod)
- [x] Create .gitignore for security
- [x] Set up Hilt with KSP
- [x] Create launcher icons

### Architecture
- [x] Implement Clean Architecture (Domain, Data, Presentation)
- [x] Create domain models (User, Message, Conversation)
- [x] Create Room database with entities and DAOs
- [x] Set up Firebase data sources
- [x] Implement repositories with offline-first pattern
- [x] Create use cases for business logic
- [x] Set up Hilt dependency injection modules

### Authentication
- [x] Implement Firebase Authentication
- [x] Create `AuthRepository` and implementation
- [x] Email/password registration flow
- [x] Email/password login flow
- [x] Google Sign-In integration
- [x] Auth state persistence
- [x] Create Login and Register screens
- [x] Create `AuthViewModel` with state management
- [x] Add password security (KeyboardOptions)

### Messaging Core
- [x] Implement Firestore message sync
- [x] Create `MessageRepository` with offline support
- [x] Create `ConversationRepository`
- [x] Implement real-time message listeners
- [x] Add message status tracking
- [x] Implement optimistic UI updates
- [x] Add message persistence with Room

### UI
- [x] Set up Jetpack Compose Navigation
- [x] Create `ConversationListScreen`
- [x] Create `ChatScreen` with message bubbles
- [x] Implement Material 3 theme
- [x] Add loading and error states
- [x] Create ViewModels for all screens

### Firebase Backend
- [x] Deploy Firestore security rules
- [x] Deploy Firestore composite indexes
- [x] Set up Cloud Functions project structure
- [x] Configure Firebase Storage
- [x] Configure Firebase Cloud Messaging (FCM)

---

## 🎯 Future PRs (Backlog)

### PR #5.5: Group Privacy & Message Filtering
**Priority:** High (Privacy Feature)  
**Estimated:** 2-3 hours  
**Depends on:** PR #5

**Problem:** Currently, when someone joins an existing group, they can see the entire message history. This is a privacy concern.

**Solution:**
- Change participants data structure from `List<String>` to `Map<String, Long>` (userId → joinTimestamp)
- Update Firestore security rules to prevent reading messages before join date
- Filter messages client-side based on user's join timestamp
- Update all mappers and conversation logic
- Add "X joined the group" system messages

**Affected Files:**
- `Conversation.kt` domain model
- `ConversationEntity.kt` and `ConversationMapper.kt`
- Firestore security rules
- `MessageRepository` query logic
- `CreateGroupUseCase` and any add participant functions

### PR #5.6 (or #6): GroupInfoScreen & Member Management
**Priority:** Medium  
**Estimated:** 3-4 hours  
**Depends on:** PR #5

- GroupInfoScreen to view/edit group details
- Add/remove participants (admins only)
- Edit group name and icon (admins only)
- Leave group functionality (all members)
- Show admin badges in participant list
- Display participant count and list
- Navigation from chat TopBar (groups only)

### PR #6: Push Notifications (FCM)
**Priority:** High  
**Estimated:** 2-3 hours
- Request notification permissions
- Handle FCM token registration
- Cloud Function for notification triggers
- Deep linking to chats
- Notification channels

### PR #7: Enhanced Message Features
**Priority:** Medium  
**Estimated:** 2-3 hours
- Display typing indicators in UI
- Show read receipts (checkmarks)
- Add unread badge counts
- Message delivery timestamps

### PR #8: AI Translation Integration
**Priority:** High (Core Feature)  
**Estimated:** 5-6 hours
- Integrate translation API
- Real-time message translation
- Language detection
- Translation caching
- Translation toggle UI

---

## 📝 Notes

- All PRs should be committed to feature branches and merged to `main`
- Update this file as tasks are completed (check off boxes)
- Add new sections for each PR as work begins
- Keep PRD.md PR history updated with condensed summaries
