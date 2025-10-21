# gChat - Development Tasks

> **Current PR:** None - Ready for Next Feature | **Last Merged:** PR #4.5 - Image Upload UI

---

## 📊 Quick Status

**Completed PRs:** 5 (All merged to main)  
**Current Sprint:** Planning Next Features  
**Next Up:** Group Chat (PR #5) or Push Notifications (PR #6) or AI Translation (PR #7)

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

### PR #4: Media Sharing (Images)
**Priority:** High  
**Estimated:** 3-4 hours
- Image picker (camera + gallery)
- Firebase Storage upload
- Display images in message bubbles
- Image viewer with fullscreen
- Profile picture uploads

### PR #5: Group Chat
**Priority:** High  
**Estimated:** 4-5 hours
- Support 3+ participants
- Group name and avatar
- Show sender names in messages
- Add/remove participants
- Group-specific UI elements

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
