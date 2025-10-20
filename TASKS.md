# gChat - Development Tasks & Roadmap

## Overview
This document outlines the 7-day sprint to build gChat MVP, broken down into concrete tasks with time estimates and dependencies. The goal is to ship a production-quality messaging app with AI translation features.

**Critical Deadline:** MVP checkpoint at 24 hours (Tuesday)  
**Early Submission:** Friday (4 days)  
**Final Submission:** Sunday (7 days)

---

## Day 0: Setup & Planning (3-4 hours)

### Environment Setup
- [ ] Install Android Studio (latest stable)
- [ ] Configure Android SDK (API 24-34)
- [ ] Set up Firebase project in console
- [ ] Download and add `google-services.json`
- [ ] Create OpenAI/Anthropic API account
- [ ] Set up physical Android device for testing
- [ ] Configure git repository and .gitignore

### Project Initialization
- [ ] Create new Android Studio project with Kotlin + Compose
- [ ] Set up Gradle dependencies (Firebase, Room, Hilt, Coil)
- [ ] Configure build variants (dev, staging, prod)
- [ ] Initialize Firebase SDK in app
- [ ] Set up Hilt dependency injection
- [ ] Create basic navigation structure
- [ ] Implement app theme (Material 3, modern colors)

### Backend Setup
- [ ] Enable Firestore, Authentication, Storage, FCM in Firebase
- [ ] Create Firestore security rules (deny all for now)
- [ ] Initialize Cloud Functions project
- [ ] Set up environment variables for API keys
- [ ] Create basic health check function

**Deliverable:** Project compiles and runs "Hello World" on device

---

## Day 1: MVP Core - Authentication & Basic Chat (16-20 hours)

**Goal:** Two users can register, log in, and exchange messages

### Morning (4 hours): Authentication
- [ ] Design authentication UI (login, register screens)
- [ ] Implement Firebase Authentication integration
  - [ ] Email/password registration
  - [ ] Email/password login
  - [ ] Password reset flow
- [ ] Create User data model (userId, displayName, profilePictureUrl, phoneNumber)
- [ ] Store user profiles in Firestore (`users` collection)
- [ ] Implement auth state persistence
- [ ] Handle auth errors gracefully

### Afternoon (6 hours): One-on-One Chat UI
- [ ] Design chat list screen (conversations)
  - [ ] List all conversations
  - [ ] Show last message preview
  - [ ] Display timestamp and unread badge
- [ ] Design conversation screen (message thread)
  - [ ] Message bubbles (sent vs received)
  - [ ] Sender profile pictures
  - [ ] Timestamps
  - [ ] Input field with send button
- [ ] Implement navigation between screens
- [ ] Create basic layouts with Compose

### Evening (6 hours): Message Data Layer
- [ ] Define Firestore schema:
  ```
  /users/{userId}
  /conversations/{conversationId}
    - participants: [userId1, userId2]
    - lastMessage: {...}
    - updatedAt: timestamp
  /messages/{conversationId}/messages/{messageId}
    - senderId, text, timestamp
    - status: sending, sent, delivered, read
  ```
- [ ] Create Room database entities
  - [ ] User entity
  - [ ] Conversation entity
  - [ ] Message entity
- [ ] Implement Room DAOs
- [ ] Create repository pattern (ConversationRepository, MessageRepository)
- [ ] Write tests for data layer

### Late Night (4 hours): Basic Message Sending
- [ ] Implement send message functionality
  - [ ] Write to Room (local storage)
  - [ ] Write to Firestore (remote sync)
  - [ ] Handle send failures with retry
- [ ] Implement Firestore real-time listeners
  - [ ] Listen for new messages
  - [ ] Update Room database
  - [ ] Notify UI layer
- [ ] Display messages in chat UI
- [ ] Test with two devices

**Checkpoint:** Two devices can exchange messages (may not be real-time yet)

---

## Day 2: MVP Core - Real-Time Sync & Offline Support (12-16 hours)

**Goal:** Messages sync in real-time with offline resilience

### Morning (4 hours): Real-Time Sync
- [ ] Implement Firestore snapshot listeners for conversations
- [ ] Set up Firestore snapshot listeners for messages
- [ ] Handle new message events
- [ ] Update UI in real-time
- [ ] Optimize for low latency (<1s)
- [ ] Test rapid-fire messages (20+ in sequence)

### Afternoon (4 hours): Optimistic UI Updates
- [ ] Generate local message IDs before send
- [ ] Display message immediately in UI
- [ ] Show "sending" status indicator
- [ ] Update message status on server confirmation
- [ ] Handle send failures gracefully
  - [ ] Show error indicator
  - [ ] Provide retry button
- [ ] Test optimistic updates on both devices

### Evening (4 hours): Offline Support
- [ ] Enable Firestore offline persistence
- [ ] Implement message queue for offline sends
- [ ] Test offline scenarios:
  - [ ] Send message while offline → queues locally
  - [ ] Go online → messages send automatically
  - [ ] Receive messages while offline → sync on reconnect
- [ ] Handle network state changes
- [ ] Show connectivity status in UI
- [ ] Test airplane mode scenarios

**Checkpoint:** Messages work flawlessly offline and sync on reconnect

---

## Day 3: MVP Polish - Status, Groups & Media (12-16 hours)

**Goal:** Hit all MVP requirements

### Morning (4 hours): Presence & Status
- [ ] Implement online/offline status
  - [ ] Update Firestore on app foreground/background
  - [ ] Use Firestore presence system
  - [ ] Display status in conversation list and chat
- [ ] Implement typing indicators
  - [ ] Send typing events to Firestore
  - [ ] Display "User is typing..." in chat
  - [ ] Debounce typing events (1-2 seconds)
- [ ] Implement message read receipts
  - [ ] Update message status to "read" when viewed
  - [ ] Display read status (checkmarks)
  - [ ] Sync read status across devices

### Afternoon (4 hours): Group Chat
- [ ] Extend conversation model for groups
  - [ ] Support 3+ participants
  - [ ] Add group name and icon
- [ ] Update Firestore schema for groups
- [ ] Implement create group UI
- [ ] Implement add participants flow
- [ ] Display group messages with sender attribution
- [ ] Show group read receipts (read by X/Y)
- [ ] Test group chat with 3+ users

### Evening (4 hours): Media Support
- [ ] Implement image picker (camera + gallery)
- [ ] Upload images to Firebase Storage
- [ ] Store image URLs in messages
- [ ] Display images in chat bubbles
- [ ] Implement image preview/fullscreen view
- [ ] Add loading indicators for uploads
- [ ] Optimize image compression
- [ ] Add profile picture upload
- [ ] Test image sending end-to-end

**MVP Checkpoint Achieved:** All core requirements complete

---

## Day 4: Push Notifications & Deployment (8-12 hours)

**Goal:** Push notifications working + deployed backend

### Morning (4 hours): Push Notifications
- [ ] Set up FCM in Firebase Console
- [ ] Add FCM token handling to app
  - [ ] Request notification permissions
  - [ ] Store FCM token in Firestore user profile
  - [ ] Refresh token on changes
- [ ] Create Cloud Function to send notifications
  - [ ] Trigger on new message creation
  - [ ] Send FCM notification to recipients
  - [ ] Include message preview and sender
- [ ] Handle notification taps (deep linking)
- [ ] Test foreground notifications
- [ ] Test background notifications
- [ ] Test when app is killed

### Afternoon (4 hours): Testing & Bug Fixes
- [ ] Run all MVP test scenarios:
  1. Real-time chat between two devices
  2. Offline → online message sync
  3. App backgrounded → receive messages
  4. Force quit → reopen (persistence)
  5. Poor network simulation
  6. Rapid-fire messages
  7. Group chat with 3+ users
- [ ] Fix any bugs discovered
- [ ] Optimize performance
- [ ] Test on multiple Android devices/versions

### Evening (2 hours): Deployment
- [ ] Generate signed APK (release build)
- [ ] Test APK on physical device
- [ ] Deploy Cloud Functions to production
- [ ] Update Firestore security rules (production-ready)
- [ ] Set up Firebase Analytics
- [ ] Create deployment documentation

**Deliverable:** Fully functional MVP with push notifications

---

## Day 5: AI Features - Translation & Language Detection (10-14 hours)

**Goal:** Implement required AI features 1-2

### Morning (5 hours): Real-Time Translation
- [ ] Create Cloud Function: `translateMessage`
  - [ ] Input: messageText, sourceLanguage, targetLanguage
  - [ ] Call OpenAI GPT-4 API
  - [ ] Prompt engineering for natural translation
  - [ ] Cache translations in Firestore
  - [ ] Return translated text
- [ ] Add translation UI to chat screen
  - [ ] Tap message → "Translate to [language]" button
  - [ ] Show translation below original text
  - [ ] Toggle between original and translation
- [ ] Implement translation caching (avoid duplicate API calls)
- [ ] Handle translation errors gracefully
- [ ] Test with multiple languages (Spanish, French, Japanese, Arabic)

### Afternoon (5 hours): Language Detection & Auto-Translate
- [ ] Create Cloud Function: `detectLanguage`
  - [ ] Input: messageText
  - [ ] Use GPT-4 for language detection
  - [ ] Return ISO language code
- [ ] Add user settings for preferred language
- [ ] Implement auto-translate toggle
  - [ ] Per-conversation setting
  - [ ] Global setting
- [ ] Auto-translate incoming messages if language differs from user preference
- [ ] Add language indicator badges on messages
- [ ] Smart detection: skip translation if sender uses user's language
- [ ] Test auto-translate in conversations

**Deliverable:** Inline translation and auto-translate working

---

## Day 6: AI Features - Cultural Context & Smart Replies (10-14 hours)

**Goal:** Implement required AI features 3-5 + advanced feature

### Morning (4 hours): Cultural Context & Formality
- [ ] Create Cloud Function: `getCulturalContext`
  - [ ] Prompt: Detect idioms, slang, cultural references
  - [ ] Return explanation and context
- [ ] Add "Explain" button to messages
- [ ] Display cultural context in modal/bottom sheet
- [ ] Create Cloud Function: `adjustFormality`
  - [ ] Input: message, targetLanguage, formalityLevel
  - [ ] Return formal and casual versions
- [ ] Add formality toggle to compose UI
- [ ] Test with various idioms and slang

### Afternoon (3 hours): Slang/Idiom Explanations
- [ ] Enhance `getCulturalContext` function
  - [ ] Detect modern slang, memes, Gen Z terms
  - [ ] Provide usage examples
- [ ] Create explanation UI component
- [ ] Cache common slang explanations
- [ ] Test with contemporary slang phrases

### Evening (5 hours): Context-Aware Smart Replies
- [ ] Implement RAG pipeline for conversation history
  - [ ] Create Cloud Function: `getConversationContext`
  - [ ] Retrieve last 50-100 messages from Firestore
  - [ ] Extract user communication patterns
- [ ] Create Cloud Function: `generateSmartReplies`
  - [ ] Input: conversation context, incoming message
  - [ ] Analyze user's tone, emoji usage, phrase preferences
  - [ ] Generate 3 contextually appropriate replies
  - [ ] Match user's communication style
- [ ] Add smart reply chips to chat UI
- [ ] Implement multi-language smart replies
- [ ] Track smart reply usage and acceptance rate
- [ ] Test smart replies in different contexts

**Deliverable:** All 5 required + advanced AI feature working

---

## Day 7: Polish, Testing & Submission (8-10 hours)

**Goal:** Ship production-quality app

### Morning (3 hours): UI/UX Polish
- [ ] Refine animations and transitions
- [ ] Add loading states for all async operations
- [ ] Improve error messages (user-friendly)
- [ ] Add empty states (no conversations, no messages)
- [ ] Polish typography and spacing
- [ ] Ensure accessibility (content descriptions, contrast)
- [ ] Add haptic feedback where appropriate
- [ ] Test on various screen sizes

### Afternoon (3 hours): Comprehensive Testing
- [ ] Test all MVP features end-to-end
- [ ] Test all AI features with real scenarios
- [ ] Verify offline support thoroughly
- [ ] Test edge cases:
  - [ ] Very long messages
  - [ ] Rapid sending
  - [ ] Large group chats
  - [ ] Poor network conditions
  - [ ] Low memory scenarios
- [ ] Fix any critical bugs

### Evening (3 hours): Documentation & Submission
- [ ] Record demo video (5-7 minutes):
  - [ ] Real-time chat between devices
  - [ ] Group chat demo
  - [ ] Offline scenario
  - [ ] App lifecycle handling
  - [ ] All 5 AI translation features
  - [ ] Smart replies in multiple languages
- [ ] Write Persona Brainlift document
- [ ] Update README with setup instructions
- [ ] Clean up code and add comments
- [ ] Generate final signed APK
- [ ] Create social media post
- [ ] Submit to Gauntlet AI

**Final Deliverable:** Complete gChat app ready for users

---

## Testing Scenarios Checklist

### Core Messaging Tests
- [ ] **Two-device real-time:** Send 20+ messages, verify <1s latency
- [ ] **Offline resilience:** 
  - [ ] Device A offline → Device B sends 10 messages
  - [ ] Device A online → receives all messages in order
- [ ] **App backgrounded:** Background app → receive message → notification appears
- [ ] **Persistence:** Force quit → reopen → all messages visible
- [ ] **Poor network:** Enable network throttling → messages still deliver
- [ ] **Rapid fire:** Send 50 messages quickly → all delivered correctly
- [ ] **Group chat:** 3 users → all receive messages → read receipts work

### AI Feature Tests
- [ ] **Translation accuracy:** Test 10 languages → 90%+ natural translations
- [ ] **Translation speed:** All translations complete in <500ms
- [ ] **Language detection:** Send messages in 5 languages → correctly detected
- [ ] **Auto-translate:** Enable auto-translate → messages translate automatically
- [ ] **Cultural context:** Send idioms ("break a leg", "piece of cake") → context provided
- [ ] **Formality:** Translate casual message to Japanese → formal version offered
- [ ] **Slang explanation:** Send Gen Z slang → accurate explanation provided
- [ ] **Smart replies:** Send 10 different messages → replies match user's style

---

## Risk Mitigation Strategies

### Technical Risks

**Risk:** Real-time sync too slow  
**Mitigation:** 
- Use Firestore listeners (not polling)
- Optimize queries with proper indexing
- Implement local-first architecture
- Test with actual 3G/4G connections

**Risk:** Firebase costs exceed budget  
**Mitigation:**
- Implement aggressive caching
- Batch AI requests where possible
- Monitor Firebase usage dashboard daily
- Set up billing alerts
- Use Firestore offline persistence

**Risk:** OpenAI API rate limits  
**Mitigation:**
- Implement exponential backoff retry logic
- Queue AI requests in Cloud Functions
- Cache translation results
- Have DeepL API as fallback

**Risk:** Offline sync conflicts  
**Mitigation:**
- Use Firestore server timestamps
- Implement last-write-wins strategy
- Generate unique IDs client-side
- Test conflict scenarios thoroughly

### Schedule Risks

**Risk:** Fall behind on MVP timeline  
**Mitigation:**
- Cut scope aggressively if needed
- Focus on core messaging first (AI can wait)
- Work in vertical slices (finish features completely)
- Test frequently to catch bugs early

**Risk:** AI features take longer than expected  
**Mitigation:**
- Start with simplest implementations
- Use existing prompts/examples from docs
- Defer advanced optimizations
- Accept 80% solution for week 1

---

## Dependencies & Blockers

### External Dependencies
- Firebase project approval (instant)
- OpenAI API access (instant with credit card)
- Physical Android device for testing (required)
- APK signing certificate (can generate locally)

### Knowledge Dependencies
- Kotlin + Jetpack Compose basics
- Firebase Firestore and Cloud Functions
- OpenAI API and prompting
- Android app lifecycle management

### Technical Blockers
- Network connectivity required for testing
- Two devices needed for proper testing
- Google account for Firebase and Play Console

---

## Success Criteria

### Must Have (MVP Pass/Fail)
✅ Two users can chat in real-time  
✅ Messages persist across app restarts  
✅ Works offline with sync on reconnect  
✅ Group chat with 3+ users functional  
✅ Push notifications working  
✅ APK deployed and testable  

### Should Have (Full Points)
✅ All 5 AI translation features working  
✅ Context-aware smart replies functional  
✅ Demo video showing all features  
✅ Polished UI (iMessage-quality feel)  
✅ Comprehensive documentation  

### Nice to Have (Bonus)
🌟 Multiple language pairs tested  
🌟 Advanced animations and transitions  
🌟 TestFlight or Play Store internal testing  
🌟 Analytics and crash reporting integrated  
🌟 Accessibility features (screen reader support)  

---

## Daily Standup Template

Use this to track progress:

**Today's Goal:**  
**Completed:**  
**In Progress:**  
**Blockers:**  
**Tomorrow's Plan:**  

---

## Post-Launch Tasks (Week 2+)

### Immediate (Week 2)
- [ ] Gather user feedback from testers
- [ ] Fix critical bugs reported
- [ ] Optimize Firebase costs
- [ ] Improve AI translation quality
- [ ] Add more languages

### Short-term (Month 1)
- [ ] Implement E2E encryption
- [ ] Add voice messages
- [ ] Build web companion app
- [ ] Improve smart reply accuracy
- [ ] Add message search

### Long-term (Month 2-3)
- [ ] Video calling with live captions
- [ ] Desktop app (Electron or native)
- [ ] Advanced AI features (voice translation)
- [ ] Monetization strategy (freemium)
- [ ] Scale to 10K+ users

---

## Resources & References

### Documentation
- [Firebase for Android](https://firebase.google.com/docs/android/setup)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [OpenAI API](https://platform.openai.com/docs)
- [AI SDK by Vercel](https://sdk.vercel.ai/docs)

### Example Prompts
**Translation:**
```
You are a professional translator. Translate the following message from {sourceLanguage} to {targetLanguage}. Maintain the original tone and context. Provide a natural, conversational translation.

Message: {text}
```

**Cultural Context:**
```
Analyze this message and identify any idioms, slang, or cultural references that may not translate literally. Provide explanations for non-native speakers.

Message: {text}
Language: {language}
```

**Smart Replies:**
```
Based on this conversation history and the user's communication style, generate 3 short, natural reply options to the latest message. Match the user's typical tone, emoji usage, and phrasing.

Conversation context: {history}
User's style: {userProfile}
Latest message: {incomingMessage}
Reply language: {targetLanguage}
```

---

## Notes

- **Prioritize reliability over features:** A simple app that works perfectly beats a feature-rich app that's flaky
- **Test on real devices:** Emulators don't accurately represent real-world performance
- **Keep it simple:** Use Firebase defaults when possible, don't over-engineer
- **Focus on the persona:** Every feature should serve International Communicators
- **Beat iMessage at one thing:** Translation quality and cross-language communication
- **Ship early, iterate fast:** Get feedback from real users ASAP

---

**Remember:** WhatsApp was built by 2 developers in months. You can build gChat in 1 week. Focus, execute, ship. 🚀

