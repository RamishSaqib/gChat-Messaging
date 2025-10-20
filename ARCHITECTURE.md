# gChat - Technical Architecture

## Overview

gChat is built using a modern Android tech stack with Firebase backend, following clean architecture principles and offline-first design patterns. This document details the system architecture, data models, tech stack decisions, and implementation guidelines.

**Core Principles:**
- Offline-first: App works seamlessly without network
- Real-time sync: Changes propagate instantly when online
- Clean Architecture: Clear separation of concerns
- Testable: Business logic isolated from framework code
- Scalable: Designed to handle millions of users

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Android App (Kotlin)                    │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Presentation Layer (Compose)              │  │
│  │  • Screens     • ViewModels      • UI Components      │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Domain Layer                          │  │
│  │  • Use Cases    • Domain Models    • Repositories     │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                   │
│                           ▼                                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   Data Layer                           │  │
│  │  ┌─────────────────┐         ┌──────────────────────┐ │  │
│  │  │ Local (Room)    │         │ Remote (Firebase)    │ │  │
│  │  │ • Messages      │ ◄─────► │ • Firestore         │ │  │
│  │  │ • Conversations │  Sync   │ • Auth              │ │  │
│  │  │ • Users         │         │ • Storage           │ │  │
│  │  └─────────────────┘         │ • Cloud Functions   │ │  │
│  │                               └──────────────────────┘ │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      Firebase Backend                        │
│                                                               │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  Firestore  │  │ Cloud        │  │  Cloud Storage   │   │
│  │  (Database) │  │ Functions    │  │  (Media Files)   │   │
│  │             │  │              │  │                  │   │
│  │ • Real-time │  │ • AI API     │  │ • Images         │   │
│  │ • Offline   │  │ • Translation│  │ • Profile pics   │   │
│  │ • Sync      │  │ • Smart Reply│  │                  │   │
│  └─────────────┘  └──────────────┘  └──────────────────┘   │
│                           │                                   │
│                           ▼                                   │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              AI Integration Layer                      │  │
│  │  • OpenAI GPT-4      • RAG Pipeline      • Caching    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

### Mobile (Android)

#### Core Technologies
- **Language:** Kotlin 1.9+
- **Min SDK:** 24 (Android 7.0 - covers 95% of devices)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle 8.2+ with Kotlin DSL

#### UI Layer
- **Framework:** Jetpack Compose (declarative UI)
- **Material Design:** Material 3 (latest design system)
- **Navigation:** Compose Navigation
- **Image Loading:** Coil (Compose-native image loading)
- **Animations:** Compose Animation APIs

#### Architecture Components
- **ViewModel:** Android Architecture Components ViewModel
- **LiveData/Flow:** Kotlin Flow (reactive data streams)
- **Lifecycle:** Jetpack Lifecycle
- **Navigation:** Compose Navigation

#### Dependency Injection
- **Framework:** Hilt (recommended DI for Android)
- **Scopes:** Application, Activity, ViewModel

#### Local Database
- **ORM:** Room (SQLite wrapper)
- **Migrations:** Automated with Room
- **Type Converters:** For complex types (lists, dates)

#### Networking & Sync
- **Firebase SDK:** Firebase Android SDK
- **HTTP Client:** Ktor or Retrofit (for non-Firebase APIs)
- **Serialization:** Kotlinx Serialization

#### Background Processing
- **WorkManager:** For reliable background tasks
- **Coroutines:** For async operations

#### Testing
- **Unit Tests:** JUnit 4, Mockk
- **UI Tests:** Compose UI Test
- **Integration Tests:** Hilt testing

---

### Backend (Firebase)

#### Firebase Services
- **Authentication:** Firebase Auth (email/password, phone)
- **Database:** Cloud Firestore (NoSQL, real-time)
- **Storage:** Cloud Storage for Firebase (images, media)
- **Functions:** Cloud Functions for Firebase (Node.js 18)
- **Messaging:** Firebase Cloud Messaging (FCM)
- **Analytics:** Firebase Analytics
- **Crashlytics:** Firebase Crashlytics

#### Cloud Functions Runtime
- **Runtime:** Node.js 18
- **Memory:** 256MB (default), 512MB for AI functions
- **Timeout:** 60s for AI functions, 10s for others
- **Region:** us-central1 (default)

---

### AI Integration

#### LLM Provider
- **Primary:** OpenAI GPT-4 (gpt-4-turbo)
- **Fallback:** Anthropic Claude 3.5 Sonnet
- **Translation Specialist:** DeepL API (optional)

#### Agent Framework
- **Option 1:** AI SDK by Vercel (recommended)
- **Option 2:** LangChain (if complex multi-agent)
- **Option 3:** Direct OpenAI SDK (simplest)

#### Vector Database (for RAG)
- **Option 1:** Firestore Vector Search (native integration)
- **Option 2:** Pinecone (specialized vector DB)
- **Option 3:** In-memory embeddings (small scale)

---

## Clean Architecture Layers

### Presentation Layer (`/presentation`)

**Responsibility:** UI and user interaction

**Components:**
- **Screens:** Full-screen Composables (LoginScreen, ChatScreen, etc.)
- **ViewModels:** State management and business logic coordination
- **UI State:** Data classes representing UI state
- **UI Events:** User actions (button clicks, text input)

**Example Structure:**
```
/presentation
  /auth
    LoginScreen.kt
    RegisterScreen.kt
    AuthViewModel.kt
    AuthUiState.kt
  /chat
    ConversationListScreen.kt
    ChatScreen.kt
    ChatViewModel.kt
    ChatUiState.kt
  /components
    MessageBubble.kt
    ProfilePicture.kt
    TypingIndicator.kt
```

**Key Patterns:**
- Unidirectional data flow (UDF)
- State hoisting
- Single source of truth
- ViewModels don't reference Android framework (testable)

---

### Domain Layer (`/domain`)

**Responsibility:** Business logic and use cases

**Components:**
- **Use Cases:** Single-purpose business operations
- **Domain Models:** Pure Kotlin data classes (no Android dependencies)
- **Repository Interfaces:** Abstraction of data sources

**Example Structure:**
```
/domain
  /model
    User.kt
    Conversation.kt
    Message.kt
  /usecase
    SendMessageUseCase.kt
    GetConversationsUseCase.kt
    TranslateMessageUseCase.kt
  /repository
    MessageRepository.kt
    UserRepository.kt
    ConversationRepository.kt
```

**Use Case Pattern:**
```kotlin
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        senderId: String
    ): Result<Message> {
        return try {
            val message = Message(
                id = generateId(),
                conversationId = conversationId,
                senderId = senderId,
                text = text,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING
            )
            
            // Save locally first (optimistic update)
            messageRepository.insertLocal(message)
            
            // Sync to remote
            messageRepository.syncToRemote(message)
            
            // Update conversation last message
            conversationRepository.updateLastMessage(conversationId, message)
            
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### Data Layer (`/data`)

**Responsibility:** Data management and persistence

**Components:**
- **Repository Implementations:** Coordinate local and remote data sources
- **Data Sources:** Local (Room) and Remote (Firebase)
- **DTOs:** Data transfer objects for network/database
- **Mappers:** Convert between DTOs and domain models

**Example Structure:**
```
/data
  /repository
    MessageRepositoryImpl.kt
    UserRepositoryImpl.kt
  /local
    /dao
      MessageDao.kt
      ConversationDao.kt
    /entity
      MessageEntity.kt
      ConversationEntity.kt
    AppDatabase.kt
  /remote
    /firestore
      FirestoreMessageDataSource.kt
      FirestoreUserDataSource.kt
    /storage
      FirebaseStorageDataSource.kt
  /mapper
    MessageMapper.kt
    UserMapper.kt
```

**Repository Pattern:**
```kotlin
class MessageRepositoryImpl @Inject constructor(
    private val localDataSource: MessageDao,
    private val remoteDataSource: FirestoreMessageDataSource,
    private val mapper: MessageMapper
) : MessageRepository {
    
    override fun getMessagesForConversation(
        conversationId: String
    ): Flow<List<Message>> {
        // Local-first approach
        return localDataSource.getMessagesFlow(conversationId)
            .map { entities -> entities.map { mapper.toDomain(it) } }
            .onStart {
                // Sync from remote in background
                syncFromRemote(conversationId)
            }
    }
    
    override suspend fun insertLocal(message: Message) {
        localDataSource.insert(mapper.toEntity(message))
    }
    
    override suspend fun syncToRemote(message: Message) {
        remoteDataSource.sendMessage(mapper.toDto(message))
    }
}
```

---

## Data Models

### Domain Models

#### User
```kotlin
data class User(
    val id: String,
    val displayName: String,
    val email: String?,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val preferredLanguage: String = "en",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

#### Conversation
```kotlin
data class Conversation(
    val id: String,
    val type: ConversationType, // ONE_ON_ONE, GROUP
    val participants: List<String>, // User IDs
    val name: String?, // For groups
    val iconUrl: String?, // For groups
    val lastMessage: Message?,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val autoTranslateEnabled: Boolean = false
)

enum class ConversationType {
    ONE_ON_ONE,
    GROUP
}
```

#### Message
```kotlin
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val type: MessageType = MessageType.TEXT,
    val text: String? = null,
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENDING,
    val readBy: List<String> = emptyList(),
    val translation: Translation? = null,
    val culturalContext: String? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM // "User joined", "Group created", etc.
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class Translation(
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val cachedAt: Long = System.currentTimeMillis()
)
```

---

## Database Schemas

### Room Database (Local Storage)

#### Database Class
```kotlin
@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
```

#### MessageEntity
```kotlin
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["id"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val type: String, // TEXT, IMAGE
    val text: String?,
    val mediaUrl: String?,
    val timestamp: Long,
    val status: String, // SENDING, SENT, DELIVERED, READ
    val readBy: String, // JSON array of user IDs
    val translatedText: String?,
    val translationSourceLang: String?,
    val translationTargetLang: String?
)
```

#### ConversationEntity
```kotlin
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["id"], unique = true)]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String, // ONE_ON_ONE, GROUP
    val participants: String, // JSON array of user IDs
    val name: String?,
    val iconUrl: String?,
    val lastMessageId: String?,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long,
    val unreadCount: Int,
    val updatedAt: Long,
    val autoTranslateEnabled: Boolean
)
```

#### UserEntity
```kotlin
@Entity(
    tableName = "users",
    indices = [Index(value = ["id"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val phoneNumber: String?,
    val profilePictureUrl: String?,
    val preferredLanguage: String,
    val isOnline: Boolean,
    val lastSeen: Long
)
```

---

### Firestore Database (Remote Storage)

#### Collection Structure
```
/users/{userId}
  - displayName: string
  - email: string
  - phoneNumber: string
  - profilePictureUrl: string
  - preferredLanguage: string
  - isOnline: boolean
  - lastSeen: timestamp
  - fcmToken: string
  - createdAt: timestamp

/conversations/{conversationId}
  - type: string ("ONE_ON_ONE" | "GROUP")
  - participants: array<string> (user IDs)
  - name: string (for groups)
  - iconUrl: string (for groups)
  - lastMessage: map {
      id: string
      senderId: string
      text: string
      timestamp: timestamp
    }
  - updatedAt: timestamp
  - createdAt: timestamp

/conversations/{conversationId}/messages/{messageId}
  - conversationId: string
  - senderId: string
  - type: string ("TEXT" | "IMAGE")
  - text: string
  - mediaUrl: string
  - timestamp: timestamp
  - status: string ("SENT" | "DELIVERED" | "READ")
  - readBy: array<string> (user IDs)

/conversations/{conversationId}/typing/{userId}
  - isTyping: boolean
  - timestamp: timestamp
  - ttl: timestamp (auto-delete after 5 seconds)

/translations/{messageId}
  - messageId: string
  - translatedText: string
  - sourceLanguage: string
  - targetLanguage: string
  - cachedAt: timestamp
```

#### Firestore Indexes
```json
{
  "indexes": [
    {
      "collectionGroup": "messages",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "conversationId", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "conversations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "participants", "arrayConfig": "CONTAINS" },
        { "fieldPath": "updatedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "messages",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "conversationId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

#### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    function isParticipant(conversation) {
      return request.auth.uid in conversation.data.participants;
    }
    
    // Users collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow create: if isOwner(userId);
      allow update: if isOwner(userId);
      allow delete: if isOwner(userId);
    }
    
    // Conversations collection
    match /conversations/{conversationId} {
      allow read: if isAuthenticated() && isParticipant(resource);
      allow create: if isAuthenticated() && isParticipant(request.resource);
      allow update: if isAuthenticated() && isParticipant(resource);
      allow delete: if isAuthenticated() && isParticipant(resource);
      
      // Messages subcollection
      match /messages/{messageId} {
        allow read: if isAuthenticated() && isParticipant(get(/databases/$(database)/documents/conversations/$(conversationId)));
        allow create: if isAuthenticated() && isParticipant(get(/databases/$(database)/documents/conversations/$(conversationId)));
        allow update: if isAuthenticated();
        allow delete: if isAuthenticated() && request.auth.uid == resource.data.senderId;
      }
      
      // Typing indicators subcollection
      match /typing/{userId} {
        allow read: if isAuthenticated() && isParticipant(get(/databases/$(database)/documents/conversations/$(conversationId)));
        allow write: if isAuthenticated() && isOwner(userId);
      }
    }
    
    // Translations collection (cached translations)
    match /translations/{messageId} {
      allow read: if isAuthenticated();
      allow write: if false; // Only Cloud Functions can write
    }
  }
}
```

---

## Real-Time Sync Strategy

### Offline-First Architecture

**Principle:** Local database is the single source of truth for UI.

**Flow:**
1. **User action** (send message) → Write to Room immediately
2. **UI updates** instantly from Room (optimistic update)
3. **Background sync** writes to Firestore
4. **Firestore listener** receives confirmation → Update Room
5. **UI reflects** final state from Room

### Sync Patterns

#### Pattern 1: Optimistic Write
```kotlin
suspend fun sendMessage(message: Message) {
    // 1. Write to local DB immediately
    messageDao.insert(message.copy(status = MessageStatus.SENDING))
    
    // 2. UI shows message immediately
    
    try {
        // 3. Sync to Firestore
        firestoreMessageDataSource.sendMessage(message)
        
        // 4. Update status to SENT
        messageDao.updateStatus(message.id, MessageStatus.SENT)
    } catch (e: Exception) {
        // 5. Mark as FAILED, retry later
        messageDao.updateStatus(message.id, MessageStatus.FAILED)
        enqueueRetry(message)
    }
}
```

#### Pattern 2: Real-Time Listener
```kotlin
fun observeMessages(conversationId: String): Flow<List<Message>> {
    // Set up Firestore listener
    firestore.collection("conversations")
        .document(conversationId)
        .collection("messages")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                // Write to Room
                val messages = snapshot.toObjects(MessageDto::class.java)
                messageDao.insertAll(messages.map { mapper.toEntity(it) })
            }
        }
    
    // Return Flow from Room (single source of truth)
    return messageDao.getMessagesFlow(conversationId)
        .map { entities -> entities.map { mapper.toDomain(it) } }
}
```

#### Pattern 3: Conflict Resolution
```kotlin
// Last-write-wins based on server timestamp
fun resolveConflict(local: Message, remote: Message): Message {
    return if (remote.timestamp > local.timestamp) {
        remote // Server version is newer
    } else {
        local // Local version is newer (rare)
    }
}
```

### Network State Handling

```kotlin
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        
        connectivityManager.registerDefaultNetworkCallback(callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
```

---

## AI Integration Architecture

### Cloud Functions Structure

```
/functions
  /src
    /ai
      translation.ts
      smartReply.ts
      culturalContext.ts
      languageDetection.ts
    /utils
      openai.ts
      cache.ts
      rateLimit.ts
    /triggers
      onMessageCreated.ts
      onUserCreated.ts
    index.ts
  package.json
  tsconfig.json
  .env
```

### Translation Function

```typescript
// functions/src/ai/translation.ts
import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY
});

interface TranslationRequest {
  text: string;
  sourceLanguage: string;
  targetLanguage: string;
}

export const translateMessage = onCall<TranslationRequest>(
  { 
    memory: '512MB',
    timeoutSeconds: 60,
    region: 'us-central1'
  },
  async (request) => {
    // Verify authentication
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { text, sourceLanguage, targetLanguage } = request.data;

    // Check cache first
    const cached = await checkTranslationCache(text, targetLanguage);
    if (cached) {
      return { translatedText: cached, cached: true };
    }

    try {
      const completion = await openai.chat.completions.create({
        model: 'gpt-4-turbo-preview',
        messages: [
          {
            role: 'system',
            content: `You are a professional translator. Translate text from ${sourceLanguage} to ${targetLanguage}. Maintain the original tone, context, and intent. Provide natural, conversational translations.`
          },
          {
            role: 'user',
            content: text
          }
        ],
        temperature: 0.3, // Lower temperature for consistent translations
        max_tokens: 500
      });

      const translatedText = completion.choices[0].message.content;

      // Cache translation
      await cacheTranslation(text, targetLanguage, translatedText);

      return {
        translatedText,
        cached: false,
        sourceLanguage,
        targetLanguage
      };
    } catch (error) {
      console.error('Translation error:', error);
      throw new HttpsError('internal', 'Translation failed');
    }
  }
);
```

### Smart Reply Function (RAG Pipeline)

```typescript
// functions/src/ai/smartReply.ts
import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';
import { firestore } from 'firebase-admin';

interface SmartReplyRequest {
  conversationId: string;
  incomingMessage: string;
  userId: string;
  targetLanguage: string;
}

export const generateSmartReplies = onCall<SmartReplyRequest>(
  {
    memory: '512MB',
    timeoutSeconds: 60
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { conversationId, incomingMessage, userId, targetLanguage } = request.data;

    // 1. Retrieve conversation history (RAG)
    const conversationContext = await getConversationContext(
      conversationId,
      userId,
      50 // last 50 messages
    );

    // 2. Analyze user's communication style
    const userStyle = await analyzeUserStyle(userId, conversationContext);

    // 3. Generate contextual replies
    const completion = await openai.chat.completions.create({
      model: 'gpt-4-turbo-preview',
      messages: [
        {
          role: 'system',
          content: `You are an AI assistant helping generate natural reply suggestions. Analyze the conversation context and user's communication style, then generate 3 short, contextually appropriate replies in ${targetLanguage}.

User's communication style:
- Typical tone: ${userStyle.tone}
- Emoji usage: ${userStyle.emojiUsage}
- Average message length: ${userStyle.avgLength} words
- Common phrases: ${userStyle.commonPhrases.join(', ')}

Generate replies that match this style and are appropriate responses to the incoming message.`
        },
        {
          role: 'user',
          content: `Conversation context:\n${conversationContext}\n\nIncoming message: "${incomingMessage}"\n\nGenerate 3 reply options as a JSON array.`
        }
      ],
      response_format: { type: 'json_object' },
      temperature: 0.7
    });

    const replies = JSON.parse(completion.choices[0].message.content);

    return {
      replies: replies.options, // ["Reply 1", "Reply 2", "Reply 3"]
      userStyle
    };
  }
);

async function getConversationContext(
  conversationId: string,
  userId: string,
  limit: number
): Promise<string> {
  const messages = await firestore()
    .collection('conversations')
    .doc(conversationId)
    .collection('messages')
    .orderBy('timestamp', 'desc')
    .limit(limit)
    .get();

  return messages.docs
    .reverse()
    .map(doc => {
      const data = doc.data();
      const isUser = data.senderId === userId;
      return `${isUser ? 'You' : 'Other'}: ${data.text}`;
    })
    .join('\n');
}

async function analyzeUserStyle(userId: string, context: string) {
  // Extract user's messages from context
  const userMessages = context
    .split('\n')
    .filter(line => line.startsWith('You:'))
    .map(line => line.substring(5));

  // Simple analysis (can be enhanced with ML)
  const avgLength = userMessages.reduce((sum, msg) => sum + msg.split(' ').length, 0) / userMessages.length;
  const emojiCount = userMessages.filter(msg => /[\u{1F600}-\u{1F64F}]/u.test(msg)).length;
  const emojiUsage = emojiCount / userMessages.length > 0.3 ? 'frequent' : 'occasional';

  return {
    tone: avgLength < 5 ? 'casual and brief' : 'conversational',
    emojiUsage,
    avgLength: Math.round(avgLength),
    commonPhrases: [] // Can extract common patterns
  };
}
```

### Cultural Context Function

```typescript
// functions/src/ai/culturalContext.ts
import { onCall } from 'firebase-functions/v2/https';
import { OpenAI } from 'openai';

interface CulturalContextRequest {
  text: string;
  language: string;
}

export const getCulturalContext = onCall<CulturalContextRequest>(
  async (request) => {
    const { text, language } = request.data;

    const completion = await openai.chat.completions.create({
      model: 'gpt-4-turbo-preview',
      messages: [
        {
          role: 'system',
          content: `Analyze the following ${language} text and identify any idioms, slang, cultural references, or expressions that may not translate literally. Provide:
1. The identified phrase/expression
2. Literal translation (if applicable)
3. Actual meaning
4. Cultural context and usage

If there are no special expressions, return an empty array.`
        },
        {
          role: 'user',
          content: text
        }
      ],
      response_format: { type: 'json_object' }
    });

    return JSON.parse(completion.choices[0].message.content);
  }
);
```

---

## Push Notifications

### FCM Implementation

#### Android Client
```kotlin
class MessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        // Update token in Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        
        when (data["type"]) {
            "NEW_MESSAGE" -> {
                val conversationId = data["conversationId"]
                val senderId = data["senderId"]
                val text = data["text"]
                
                showNotification(conversationId, senderId, text)
            }
            "TYPING" -> {
                // Update UI if app is in foreground
                updateTypingIndicator(data["conversationId"], true)
            }
        }
    }
    
    private fun showNotification(conversationId: String, senderId: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New message from $senderId")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(conversationId))
            .build()
        
        NotificationManagerCompat.from(this).notify(conversationId.hashCode(), notification)
    }
}
```

#### Cloud Function Trigger
```typescript
// functions/src/triggers/onMessageCreated.ts
import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { messaging } from 'firebase-admin';

export const onMessageCreated = onDocumentCreated(
  'conversations/{conversationId}/messages/{messageId}',
  async (event) => {
    const message = event.data.data();
    const conversationId = event.params.conversationId;
    
    // Get conversation participants
    const conversation = await firestore()
      .collection('conversations')
      .doc(conversationId)
      .get();
    
    const participants = conversation.data().participants;
    const recipientIds = participants.filter(id => id !== message.senderId);
    
    // Get recipient FCM tokens
    const recipientDocs = await firestore()
      .collection('users')
      .where('__name__', 'in', recipientIds)
      .get();
    
    const tokens = recipientDocs.docs
      .map(doc => doc.data().fcmToken)
      .filter(token => token != null);
    
    if (tokens.length === 0) return;
    
    // Send notification
    await messaging().sendMulticast({
      tokens,
      data: {
        type: 'NEW_MESSAGE',
        conversationId,
        senderId: message.senderId,
        text: message.text
      },
      notification: {
        title: 'New message',
        body: message.text
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'messages',
          sound: 'default'
        }
      }
    });
  }
);
```

---

## Security Considerations

### API Key Management
- **Never commit API keys to git**
- Store keys in Firebase Cloud Functions environment variables
- Use `.env.local` for local development
- Rotate keys regularly

### Authentication & Authorization
- Use Firebase Authentication for user management
- Implement Firestore security rules (shown above)
- Validate all Cloud Function inputs
- Use HTTPS-only for Cloud Functions

### Data Privacy
- Never log sensitive user data
- Encrypt media files in Cloud Storage
- Implement user data export (GDPR compliance)
- Provide data deletion mechanism

### Rate Limiting
```typescript
// Simple rate limiting for AI features
const rateLimiter = new Map<string, number[]>();

function checkRateLimit(userId: string, maxRequests: number, windowMs: number): boolean {
  const now = Date.now();
  const userRequests = rateLimiter.get(userId) || [];
  
  // Remove old requests outside the window
  const recentRequests = userRequests.filter(timestamp => now - timestamp < windowMs);
  
  if (recentRequests.length >= maxRequests) {
    return false; // Rate limit exceeded
  }
  
  recentRequests.push(now);
  rateLimiter.set(userId, recentRequests);
  return true;
}
```

---

## Performance Optimization

### Caching Strategy

#### Translation Cache
- Cache translations in Firestore (`/translations` collection)
- TTL: 30 days
- Key: hash(text + targetLanguage)

#### Image Loading
- Use Coil's disk and memory cache
- Lazy load images in message list
- Compress images before upload (max 1920px width)

#### Message Pagination
- Load messages in batches (20-50 per page)
- Implement infinite scroll
- Keep recent messages in memory

### Database Optimization

#### Room
- Use indexes on frequently queried columns
- Implement database migrations properly
- Use transactions for batch operations

#### Firestore
- Create composite indexes (shown above)
- Use `limit()` queries to reduce reads
- Implement pagination with `startAfter()`
- Use offline persistence (enabled by default)

---

## Testing Strategy

### Unit Tests
- Test ViewModels (business logic)
- Test Use Cases (domain logic)
- Test Repositories (data layer)
- Test Mappers (data transformations)

### Integration Tests
- Test Room database operations
- Test Firestore operations (use emulator)
- Test Cloud Functions locally

### UI Tests
- Test key user flows (login, send message, etc.)
- Use Compose UI Test
- Test with fake data sources

### Manual Testing
- Test on physical devices (multiple Android versions)
- Test offline scenarios thoroughly
- Test with real Firebase backend
- Test AI features with various inputs

---

## Deployment

### Build Variants
```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        create("prod") {
            dimension = "environment"
        }
    }
}
```

### APK Generation
```bash
# Debug APK
./gradlew assembleProdDebug

# Release APK (signed)
./gradlew assembleProdRelease
```

### Cloud Functions Deployment
```bash
# Deploy all functions
firebase deploy --only functions

# Deploy specific function
firebase deploy --only functions:translateMessage
```

---

## Monitoring & Analytics

### Firebase Analytics Events
```kotlin
// Track key events
firebaseAnalytics.logEvent("message_sent") {
    param("conversation_type", conversationType)
}

firebaseAnalytics.logEvent("translation_used") {
    param("source_language", sourceLang)
    param("target_language", targetLang)
}

firebaseAnalytics.logEvent("smart_reply_accepted") {
    param("reply_index", replyIndex)
}
```

### Crashlytics
```kotlin
// Log non-fatal errors
FirebaseCrashlytics.getInstance().recordException(exception)

// Add custom keys
FirebaseCrashlytics.getInstance().apply {
    setCustomKey("user_id", userId)
    setCustomKey("conversation_id", conversationId)
}
```

---

## Conclusion

This architecture provides a solid foundation for building gChat as a production-quality messaging app with AI features. The key principles are:

1. **Offline-first:** App works without network, syncs when online
2. **Real-time:** Messages deliver instantly using Firestore listeners
3. **Scalable:** Clean architecture allows easy feature additions
4. **Testable:** Clear separation of concerns enables comprehensive testing
5. **Maintainable:** Modern tech stack with best practices

Follow this architecture, and gChat will rival iMessage in quality while providing unique value through AI-powered translation features.

