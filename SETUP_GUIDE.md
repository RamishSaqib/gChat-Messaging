# gChat Setup Guide

This guide will walk you through setting up the gChat project from scratch.

## Prerequisites Checklist

Before you begin, ensure you have:

- [ ] **Android Studio** (Arctic Fox or later) - [Download](https://developer.android.com/studio)
- [ ] **Java JDK 17** or later
- [ ] **Git** for version control
- [ ] **Google Account** for Firebase
- [ ] **OpenAI API Key** - [Get one here](https://platform.openai.com/api-keys)
- [ ] **Physical Android Device** (recommended, minimum Android 7.0)

---

## Step 1: Project Setup

### 1.1 Clone the Repository

```bash
git clone <your-repo-url>
cd gChat
```

### 1.2 Open in Android Studio

1. Launch Android Studio
2. Select **File > Open**
3. Navigate to the `gChat` directory
4. Click **OK**

Android Studio will automatically:
- Download Gradle dependencies
- Index the project
- Configure the Android SDK

**This may take 5-10 minutes on first run.**

---

## Step 2: Firebase Setup

### 2.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **Add Project**
3. Enter project name: `gChat` (or your preferred name)
4. Disable Google Analytics (optional for development)
5. Click **Create Project**

### 2.2 Add Android App to Firebase

1. In Firebase Console, click **Add app** → **Android**
2. Enter package name: `com.gchat`
3. Enter app nickname: `gChat Android`
4. Click **Register app**
5. Download `google-services.json`
6. Place file in `app/` directory (replace the example file)

### 2.3 Enable Firebase Services

In Firebase Console, enable these services:

#### Authentication
1. Go to **Build > Authentication**
2. Click **Get Started**
3. Enable **Email/Password** sign-in method

#### Cloud Firestore
1. Go to **Build > Firestore Database**
2. Click **Create Database**
3. Start in **Test Mode** (we'll deploy security rules later)
4. Choose location (e.g., `us-central`)

#### Cloud Storage
1. Go to **Build > Storage**
2. Click **Get Started**
3. Start in **Test Mode**

#### Cloud Messaging (FCM)
1. Go to **Build > Cloud Messaging**
2. Already enabled automatically

#### Analytics & Crashlytics
1. Go to **Build > Crashlytics**
2. Click **Enable Crashlytics**

### 2.4 Deploy Firestore Rules and Indexes

```bash
# Install Firebase CLI if you haven't
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase in project
firebase init

# Select:
# - Firestore (rules and indexes)
# - Functions
# - Use existing project: gChat

# Deploy Firestore configuration
firebase deploy --only firestore

# This deploys:
# - Security rules (firebase/firestore.rules)
# - Indexes (firebase/firestore.indexes.json)
```

---

## Step 3: Cloud Functions Setup

### 3.1 Install Node.js Dependencies

```bash
cd firebase/functions
npm install
```

### 3.2 Configure Environment Variables

```bash
# Copy environment template
cp env.example.txt .env

# Edit .env file
nano .env  # or use your preferred editor
```

Add your API keys:

```env
OPENAI_API_KEY=sk-proj-your-actual-openai-key-here
OPENAI_MODEL=gpt-4-turbo-preview
```

**Get OpenAI API Key:**
1. Go to https://platform.openai.com/api-keys
2. Click **Create new secret key**
3. Copy the key (starts with `sk-proj-` or `sk-`)
4. Paste into `.env` file

### 3.3 Deploy Cloud Functions

```bash
# Still in firebase/functions directory
npm run deploy

# This deploys all AI functions:
# - translateMessage
# - detectLanguage
# - generateSmartReplies
# - getCulturalContext
# - adjustFormality
# - onMessageCreated (trigger)
```

**Note:** First deployment takes 5-10 minutes.

### 3.4 Set Environment Variables in Firebase

For production, also set environment variables in Firebase:

```bash
firebase functions:config:set openai.api_key="your-openai-key"
firebase functions:config:set openai.model="gpt-4-turbo-preview"

# Deploy again to apply config
firebase deploy --only functions
```

---

## Step 4: Build the Android App

### 4.1 Sync Gradle

In Android Studio:
1. Click **File > Sync Project with Gradle Files**
2. Wait for sync to complete (may take 2-5 minutes)

### 4.2 Build Debug APK

**Option A: In Android Studio**
1. Click **Build > Make Project**
2. Wait for build to complete

**Option B: Command Line**
```bash
# From project root
./gradlew assembleDevDebug
```

### 4.3 Run on Device

**Using Android Studio:**
1. Connect your Android device via USB
2. Enable **USB Debugging** on device
3. Select device in toolbar dropdown
4. Click **Run ▶️** button

**Using Command Line:**
```bash
# Install on connected device
./gradlew installDevDebug

# Launch app
adb shell am start -n com.gchat.dev.debug/com.gchat.presentation.MainActivity
```

---

## Step 5: Testing

### 5.1 Create Test Accounts

1. Launch app on device
2. Register a new account:
   - Email: `user1@test.com`
   - Password: `password123`
   - Display Name: `Test User 1`

3. On second device (or emulator), register:
   - Email: `user2@test.com`
   - Password: `password123`
   - Display Name: `Test User 2`

### 5.2 Test Core Features

- [ ] Send message between two accounts
- [ ] Verify real-time message delivery
- [ ] Test offline mode (airplane mode)
- [ ] Verify message persistence (restart app)
- [ ] Test image sending
- [ ] Create group chat with 3+ users
- [ ] Test push notifications

### 5.3 Test AI Features

- [ ] Translate a message (tap and select "Translate")
- [ ] Enable auto-translate in conversation
- [ ] Test cultural context (send an idiom)
- [ ] Test smart replies
- [ ] Test formality adjustment

---

## Step 6: Production Preparation

### 6.1 Update Firestore Rules

Change Firestore rules from test mode to production:

1. In Firebase Console: **Firestore > Rules**
2. Rules should already be deployed from Step 2.4
3. Verify rules are restrictive (not `allow read, write: if true`)

### 6.2 Generate Signing Key

```bash
# Create release signing key
keytool -genkey -v -keystore gchat-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias gchat

# Enter password when prompted
# Fill in certificate information
```

**Keep this file safe and never commit it to git!**

### 6.3 Configure Signing

Add to `app/build.gradle.kts`:

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
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... other config
        }
    }
}
```

### 6.4 Build Release APK

```bash
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_PASSWORD=your_key_password

./gradlew assembleProdRelease
```

APK location: `app/build/outputs/apk/prod/release/app-prod-release.apk`

---

## Troubleshooting

### Issue: "google-services.json not found"

**Solution:**
1. Download from Firebase Console
2. Place in `app/` directory
3. Sync Gradle

### Issue: "Failed to resolve: com.google.firebase:firebase-..."

**Solution:**
1. Check internet connection
2. Sync Gradle: **File > Sync Project with Gradle Files**
3. Clear Gradle cache: `./gradlew clean`

### Issue: Firestore permission denied

**Solution:**
1. Verify Firestore rules are deployed: `firebase deploy --only firestore:rules`
2. Check user is authenticated
3. Verify user ID matches in security rules

### Issue: Cloud Functions not working

**Solution:**
1. Check functions are deployed: `firebase functions:list`
2. View logs: `firebase functions:log`
3. Verify environment variables: `firebase functions:config:get`
4. Ensure OpenAI API key is valid

### Issue: Push notifications not working

**Solution:**
1. Verify FCM is enabled in Firebase Console
2. Check `google-services.json` is in `app/` directory
3. Test on physical device (emulator notifications are unreliable)
4. Check notification permissions are granted
5. View Cloud Function logs: `firebase functions:log --only onMessageCreated`

---

## Next Steps

Now that your environment is set up:

1. **Read the PRD:** Understand the product vision and features
2. **Review ARCHITECTURE.md:** Understand the technical architecture
3. **Follow TASKS.md:** Start building MVP features
4. **Test frequently:** Use physical devices for realistic testing
5. **Monitor Firebase:** Watch usage and costs in Firebase Console

---

## Useful Commands

```bash
# Build
./gradlew assembleDevDebug          # Build debug APK
./gradlew assembleProdRelease       # Build release APK
./gradlew clean                     # Clean build artifacts

# Install
./gradlew installDevDebug           # Install debug on device
./gradlew installProdRelease        # Install release on device

# Test
./gradlew test                      # Run unit tests
./gradlew connectedAndroidTest      # Run instrumented tests

# Firebase
firebase deploy --only firestore    # Deploy rules and indexes
firebase deploy --only functions    # Deploy Cloud Functions
firebase functions:log              # View function logs
firebase emulators:start            # Start local emulators

# Gradle
./gradlew dependencies              # View all dependencies
./gradlew tasks                     # View all available tasks
```

---

## Getting Help

- **Documentation:** Read PRD.md, ARCHITECTURE.md, TASKS.md
- **Firebase Docs:** https://firebase.google.com/docs
- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **OpenAI API:** https://platform.openai.com/docs

---

**You're all set! Start building! 🚀**

