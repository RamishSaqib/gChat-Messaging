# gChat - International Messaging with AI Translation

<p align="center">
  <strong>An iMessage competitor for Android with AI-powered real-time translation</strong>
</p>

<p align="center">
  Built with Kotlin • Jetpack Compose • Firebase • OpenAI GPT-4
</p>

---

## 🌍 Overview

**gChat** is a modern Android messaging app that bridges language barriers through AI-powered translation. While competing with Apple's iMessage in quality and reliability, gChat goes further by enabling seamless communication across languages—making it perfect for international communicators.

### Why gChat?

- **iMessage-Quality UX:** Polished, reliable messaging experience for Android
- **AI-Powered Translation:** Real-time message translation in 50+ languages
- **Cultural Intelligence:** Understand idioms, slang, and cultural context
- **Smart Replies:** Context-aware reply suggestions that match your communication style
- **Offline-First:** Works seamlessly without network, syncs when online
- **Privacy-Focused:** End-to-end encryption, transparent data policies

### Target Users

**International Communicators** who have friends, family, or colleagues speaking different languages and struggle with:
- Constant app-switching between messaging and translation
- Lost context and cultural nuances in machine translation
- Manual copy-paste workflows that disrupt conversation flow
- Uncertainty about appropriate formality levels in different languages

---

## ✨ Features

### Core Messaging (MVP)
- ✅ **One-on-one chat** with real-time delivery (<1s latency)
- ✅ **Group chat** supporting 3+ participants
- ✅ **Offline support** with automatic sync on reconnect
- ✅ **Optimistic UI updates** for instant message appearance
- ✅ **Read receipts** and delivery status tracking
- ✅ **Typing indicators** and online/offline status
- ✅ **Media sharing** (images with compression)
- ✅ **Push notifications** (foreground & background)
- ✅ **Profile customization** with display names and photos

### AI Translation Features
- 🤖 **Real-time translation** - Tap any message to translate inline
- 🌐 **Auto-translate mode** - Automatically translate messages based on language preference
- 💡 **Cultural context hints** - Understand idioms, slang, and cultural references
- 🎯 **Formality adjustment** - Toggle between formal and casual tone in translations
- 📚 **Slang explanations** - Learn modern expressions in context
- ✨ **Smart replies** (Advanced) - AI-generated replies matching your communication style across languages

### Technical Highlights
- **Offline-First Architecture:** Local Room database syncs with Firestore
- **Clean Architecture:** Presentation, Domain, and Data layers with dependency injection (Hilt)
- **Modern UI:** Jetpack Compose with Material Design 3
- **Firebase Backend:** Real-time Firestore, Cloud Functions for AI, FCM for push notifications
- **AI Integration:** OpenAI GPT-4 with RAG pipeline for context-aware features

---

## 🚀 Quick Start

### Prerequisites

1. **Android Studio** (latest stable version)
   - Download: https://developer.android.com/studio
   
2. **Android SDK** (API 24-34)
   - Configured in Android Studio

3. **Firebase Account**
   - Create project: https://console.firebase.google.com/

4. **OpenAI API Key**
   - Get key: https://platform.openai.com/api-keys

5. **Physical Android Device** (recommended for testing)
   - Minimum Android 7.0 (API 24)

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/gChat.git
cd gChat
```

#### 2. Set Up Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project named "gChat"
3. Add an Android app with package name: `com.gchat`
4. Download `google-services.json`
5. Place it in `app/` directory

**Enable Firebase Services:**
- ✅ Authentication (Email/Password)
- ✅ Cloud Firestore
- ✅ Cloud Storage
- ✅ Cloud Messaging (FCM)
- ✅ Analytics
- ✅ Crashlytics

**Deploy Firestore Security Rules:**
```bash
firebase deploy --only firestore:rules
```

**Deploy Firestore Indexes:**
```bash
firebase deploy --only firestore:indexes
```

#### 3. Set Up Cloud Functions

```bash
cd firebase/functions

# Install dependencies
npm install

# Copy environment template
cp env.example.txt .env

# Edit .env and add your API keys
nano .env  # or use your preferred editor

# Deploy functions
npm run deploy
```

**Required Environment Variables in `.env`:**
```env
OPENAI_API_KEY=sk-your-actual-openai-key
OPENAI_MODEL=gpt-4-turbo-preview
```

#### 4. Build the Android App

```bash
# Open project in Android Studio
# File > Open > Select gChat directory

# Sync Gradle
# Click "Sync Now" when prompted

# Build the project
./gradlew build

# Or build directly in Android Studio
# Build > Make Project
```

#### 5. Run on Device

**Option A: Android Studio**
1. Connect Android device via USB (enable USB debugging)
2. Select device in toolbar
3. Click Run ▶️ button

**Option B: Command Line**
```bash
# Install debug APK
./gradlew installDevDebug

# Or install release APK (requires signing)
./gradlew installProdRelease
```

---

## 📁 Project Structure

```
gChat/
├── app/                                    # Android application
│   ├── src/main/java/com/gchat/
│   │   ├── data/                          # Data layer
│   │   │   ├── local/                     # Room database
│   │   │   │   ├── dao/                   # Data Access Objects
│   │   │   │   └── entity/                # Room entities
│   │   │   ├── remote/                    # Firebase data sources
│   │   │   │   ├── firestore/             # Firestore operations
│   │   │   │   └── storage/               # Cloud Storage operations
│   │   │   ├── repository/                # Repository implementations
│   │   │   └── mapper/                    # DTO ↔ Domain mappers
│   │   ├── domain/                        # Business logic
│   │   │   ├── model/                     # Domain models
│   │   │   ├── usecase/                   # Use cases
│   │   │   └── repository/                # Repository interfaces
│   │   ├── presentation/                  # UI layer
│   │   │   ├── auth/                      # Login, register screens
│   │   │   ├── chat/                      # Chat screens
│   │   │   ├── components/                # Reusable UI components
│   │   │   └── theme/                     # Material 3 theme
│   │   ├── di/                            # Dependency injection (Hilt)
│   │   └── utils/                         # Utilities & helpers
│   ├── src/main/res/                      # Android resources
│   └── build.gradle.kts                   # App-level Gradle config
├── firebase/                               # Firebase configuration
│   ├── functions/                         # Cloud Functions
│   │   ├── src/
│   │   │   ├── ai/                        # AI features
│   │   │   │   ├── translation.ts         # Translation functions
│   │   │   │   ├── smartReply.ts          # Smart reply generation
│   │   │   │   └── culturalContext.ts     # Cultural insights
│   │   │   ├── triggers/                  # Firestore triggers
│   │   │   │   └── onMessageCreated.ts    # Push notifications
│   │   │   └── index.ts                   # Entry point
│   │   └── package.json                   # Node.js dependencies
│   ├── firestore.rules                    # Security rules
│   └── firestore.indexes.json             # Composite indexes
├── gradle/                                 # Gradle wrapper
├── PRD.md                                  # Product Requirements Document
├── TASKS.md                                # Development tasks & roadmap
├── ARCHITECTURE.md                         # Technical architecture
├── build.gradle.kts                        # Project-level Gradle config
├── settings.gradle.kts                     # Gradle settings
└── README.md                               # This file
```

### Architecture Overview

gChat follows **Clean Architecture** principles with three distinct layers:

1. **Presentation Layer (UI)**
   - Jetpack Compose screens and components
   - ViewModels for state management
   - Unidirectional data flow

2. **Domain Layer (Business Logic)**
   - Use cases for single-purpose operations
   - Domain models (pure Kotlin, no Android dependencies)
   - Repository interfaces

3. **Data Layer (Data Management)**
   - Repository implementations
   - Local data source (Room)
   - Remote data source (Firebase)
   - Offline-first sync strategy

**Key Patterns:**
- **Repository Pattern:** Abstract data sources
- **Dependency Injection:** Hilt for loose coupling
- **Reactive Streams:** Kotlin Flow for real-time updates
- **Offline-First:** Local database is source of truth

---

## 🔧 Configuration

### Build Variants

gChat supports multiple build configurations:

| Variant | Description | Package Name |
|---------|-------------|--------------|
| **devDebug** | Development with debug logging | `com.gchat.dev.debug` |
| **stagingDebug** | Staging environment testing | `com.gchat.staging.debug` |
| **prodRelease** | Production release build | `com.gchat` |

**Build specific variant:**
```bash
./gradlew assembleDevDebug
./gradlew assembleProdRelease
```

### Firebase Environments

For multiple environments, create separate Firebase projects:
- `gChat-dev` (development)
- `gChat-staging` (staging)
- `gChat-prod` (production)

Place corresponding `google-services.json` files:
- `app/src/dev/google-services.json`
- `app/src/staging/google-services.json`
- `app/src/prod/google-services.json`

---

## 🧪 Testing

### Unit Tests

Test business logic and use cases:

```bash
./gradlew test
```

**Example test structure:**
```
app/src/test/java/com/gchat/
├── domain/usecase/SendMessageUseCaseTest.kt
├── data/repository/MessageRepositoryTest.kt
└── presentation/chat/ChatViewModelTest.kt
```

### Integration Tests

Test database and repository interactions:

```bash
./gradlew connectedAndroidTest
```

### UI Tests

Test Compose screens with UI testing framework:

```bash
./gradlew connectedAndroidTest -P android.testInstrumentationRunnerArguments.class=com.gchat.presentation.ChatScreenTest
```

### Manual Testing Scenarios

Run these scenarios before submission:

1. ✅ **Real-time chat:** Two devices exchange 20+ messages
2. ✅ **Offline resilience:** Go offline → receive messages → come online
3. ✅ **App lifecycle:** Background app → receive message → notification
4. ✅ **Persistence:** Force quit → reopen → messages still visible
5. ✅ **Poor network:** Throttle connection → messages still deliver
6. ✅ **Group chat:** 3+ users → all receive messages
7. ✅ **Translation:** Translate messages in 5+ languages
8. ✅ **Smart replies:** Generate contextual replies

---

## 📱 Deployment

### Generate Release APK

1. **Create signing key:**
```bash
keytool -genkey -v -keystore gchat-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias gchat
```

2. **Configure signing in `app/build.gradle.kts`:**
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../gchat-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "gchat"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}
```

3. **Build signed APK:**
```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_PASSWORD=your_key_password
./gradlew assembleProdRelease
```

APK location: `app/build/outputs/apk/prod/release/app-prod-release.apk`

### Google Play Internal Testing (Optional)

1. Create app in [Google Play Console](https://play.google.com/console/)
2. Set up internal testing track
3. Upload APK or use App Bundle:
```bash
./gradlew bundleProdRelease
```
4. Add testers via email
5. Share internal testing link

---

## 🤖 AI Features Deep Dive

### Translation Architecture

```
User taps message → Android calls Cloud Function → GPT-4 translates → Cache in Firestore → Return to app
```

**Caching Strategy:**
- Translations cached for 30 days
- Cache key: `hash(text + targetLanguage)`
- Reduces API costs by 80%+

### Smart Replies (RAG Pipeline)

```
1. Retrieve last 50 messages (RAG)
2. Analyze user's communication style
3. Generate 3 contextual replies with GPT-4
4. Match user's tone, emoji usage, formality
5. Return replies in target language
```

**Personalization:**
- Learns your communication patterns
- Adapts per language and relationship
- Improves accuracy over time

### Cultural Context

Detects and explains:
- Idioms ("break a leg" → "good luck")
- Slang ("that's cap" → "that's a lie")
- Cultural references
- Regional expressions

---

## 🔒 Security & Privacy

### Data Protection
- ✅ Firebase Authentication with secure tokens
- ✅ Firestore security rules (participant-based access)
- ✅ HTTPS-only communication
- ✅ API keys stored in Cloud Functions (not in app)
- ✅ Image uploads through Firebase Storage with access controls

### Privacy Considerations
- 🔐 End-to-end encryption (planned for v2)
- 🗑️ User data deletion on account removal
- 📊 Minimal analytics (Firebase Analytics)
- 🚫 No third-party data sharing
- ✅ GDPR-compliant data export

**API Keys Security:**
- Never commit `google-services.json` to public repos
- Use environment variables for Cloud Functions
- Rotate API keys regularly
- Monitor API usage for anomalies

---

## 📊 Performance Optimization

### App Performance
- **Message Loading:** Paginated queries (20-50 messages per page)
- **Image Loading:** Coil with disk/memory cache, lazy loading
- **Database:** Room indexes on frequently queried columns
- **Network:** Firestore offline persistence enabled by default

### Firebase Costs
Monitor usage at https://console.firebase.google.com/

**Cost Optimization:**
- Aggressive translation caching (reduces OpenAI API calls)
- Batch Firestore writes where possible
- Optimize image compression (max 1920px width)
- Use Firestore queries with `limit()`

**Estimated Costs (1000 active users):**
- Firestore: ~$5-10/month
- Cloud Functions: ~$10-20/month
- OpenAI API: ~$20-50/month (with caching)
- Cloud Storage: ~$1-5/month
- **Total: ~$35-85/month**

---

## 🐛 Troubleshooting

### Common Issues

**Build Fails: "google-services.json not found"**
```
Solution: Download google-services.json from Firebase Console and place in app/ directory
```

**Cloud Functions Deploy Fails**
```bash
# Ensure Firebase CLI is logged in
firebase login

# Check project is set
firebase use --add

# Deploy with verbose logging
firebase deploy --only functions --debug
```

**Messages Not Syncing**
```
1. Check internet connection
2. Verify Firestore rules are deployed
3. Check Android Logcat for errors
4. Ensure Firebase services are enabled in Console
```

**Push Notifications Not Working**
```
1. Verify FCM is enabled in Firebase Console
2. Check notification permissions are granted
3. Ensure FCM token is stored in user profile
4. Test on physical device (emulator notifications are unreliable)
```

**Translation API Errors**
```
1. Verify OPENAI_API_KEY in Cloud Functions .env
2. Check API key has sufficient credits
3. Monitor Cloud Functions logs: firebase functions:log
```

---

## 🛣️ Roadmap

### Week 1 (MVP) ✅
- Core messaging infrastructure
- Real-time sync and offline support
- Group chat
- All 5 AI translation features
- Context-aware smart replies

### Week 2-4 (Alpha)
- [ ] Voice messages with transcription
- [ ] End-to-end encryption (E2EE)
- [ ] Message search
- [ ] Advanced media support (videos, files)
- [ ] Desktop web companion app

### Month 2-3 (Beta)
- [ ] Video calls with live caption translation
- [ ] Voice-to-text with auto-translate
- [ ] Image translation (OCR + translate)
- [ ] Conversation summarization
- [ ] Multi-device sync

### Future (v2.0+)
- [ ] Desktop native apps (Windows, macOS)
- [ ] Stickers and GIF integration
- [ ] Message reactions (iMessage-style tapback)
- [ ] Advanced analytics dashboard
- [ ] Monetization (premium features)

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

**Code Style:**
- Follow Kotlin official style guide
- Use meaningful variable names
- Add comments for complex logic
- Write unit tests for new features

---

## 📄 License

This project is built for [Gauntlet AI](https://gauntlet.ai/) Week 1 Sprint Challenge.

**Educational purposes only.** Not licensed for commercial use without permission.

---

## 🙏 Acknowledgments

- **Firebase** for real-time backend infrastructure
- **OpenAI** for GPT-4 translation and AI features
- **Jetpack Compose** for modern Android UI
- **Material Design 3** for beautiful design system
- **Gauntlet AI** for the inspiring challenge

---

## 📞 Support

For questions, issues, or feedback:

- **GitHub Issues:** https://github.com/yourusername/gChat/issues
- **Email:** your.email@example.com
- **Twitter/X:** [@YourHandle](https://twitter.com/YourHandle)

---

## 🎯 Project Goals

**Primary Goal:** Build an iMessage competitor for Android that solves international communication through AI.

**Success Metrics:**
- ✅ Pass MVP checkpoint (24-hour deadline)
- ✅ All messaging features work reliably
- ✅ All 5 AI translation features functional
- ✅ Context-aware smart replies implemented
- ✅ Deployed and testable via APK
- ✅ Comprehensive documentation complete

**Built with ❤️ for international communicators everywhere.**

---

**Remember:** WhatsApp was built by 2 developers. gChat was built in 1 week with AI assistance. The future of development is here. 🚀

