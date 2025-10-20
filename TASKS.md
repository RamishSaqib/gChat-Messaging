# gChat - Development Tasks

> **Current PR:** Planning PR #3 | **Last Merged:** PR #2 - Logout Functionality

---

## 📊 Quick Status

**Completed PRs:** 2  
**Current Sprint:** Feature Expansion  
**Next Up:** New Conversation Flow (PR #3)

---

## 🔄 PR #3: New Conversation Flow (PLANNED)

**Goal:** Enable users to start new conversations with other users

**Status:** 📋 Not Started

### Backend Tasks
- [ ] Create `UserRepository.searchUsers()` function
- [ ] Add Firestore query for user search by email/name
- [ ] Update Firestore security rules for user search
- [ ] Create `CreateConversationUseCase` (already exists, verify it works)
- [ ] Add duplicate conversation check logic

### UI Tasks
- [ ] Create `NewConversationScreen` composable
- [ ] Add "New Chat" FAB to `ConversationListScreen`
- [ ] Create user search UI with TextField
- [ ] Display search results as clickable list
- [ ] Handle empty state (no users found)
- [ ] Add loading state during search
- [ ] Navigate to chat screen after conversation creation

### Navigation
- [ ] Add `NewConversation` route to `Screen` sealed class
- [ ] Update `NavGraph` with new route
- [ ] Handle back navigation from NewConversationScreen

### Testing
- [ ] Test creating conversation with existing user
- [ ] Test duplicate conversation handling
- [ ] Test search functionality
- [ ] Test empty states

**Estimated Time:** 2-3 hours

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
