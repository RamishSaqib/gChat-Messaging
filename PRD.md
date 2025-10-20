# gChat - Product Requirements Document

> **Status**: 🚀 MVP Phase Complete | 🎯 Ready for AI Features

---

## 📋 Pull Request History

### PR #3: New Conversation Flow
**Status:** ✅ Ready for Merge  
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
