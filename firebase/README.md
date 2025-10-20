# Firebase Configuration

This directory contains Firebase configuration files and Cloud Functions for gChat.

## Directory Structure

```
firebase/
├── functions/              # Cloud Functions (AI features)
│   ├── src/
│   │   ├── ai/            # AI-powered features
│   │   ├── triggers/      # Firestore triggers
│   │   └── index.ts       # Entry point
│   ├── package.json       # Node.js dependencies
│   └── tsconfig.json      # TypeScript config
├── firestore.rules        # Security rules
├── firestore.indexes.json # Composite indexes
└── README.md              # This file
```

## Cloud Functions

### Translation Functions (`ai/translation.ts`)

- **`translateMessage`** - Translates text between languages using GPT-4
- **`detectLanguage`** - Detects the language of input text

### Smart Reply Functions (`ai/smartReply.ts`)

- **`generateSmartReplies`** - Generates contextual reply suggestions using RAG pipeline

### Cultural Context Functions (`ai/culturalContext.ts`)

- **`getCulturalContext`** - Explains idioms, slang, and cultural references
- **`adjustFormality`** - Adjusts message formality for different cultures

### Triggers (`triggers/onMessageCreated.ts`)

- **`onMessageCreated`** - Sends push notifications when new messages are created

## Setup

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Configure Environment

```bash
cp env.example.txt .env
nano .env  # Add your API keys
```

Required environment variables:
- `OPENAI_API_KEY` - Your OpenAI API key
- `OPENAI_MODEL` - Model to use (default: `gpt-4-turbo-preview`)

### 3. Deploy

```bash
# Deploy all functions
npm run deploy

# Or deploy individually
firebase deploy --only functions:translateMessage
firebase deploy --only functions:generateSmartReplies
```

### 4. Test Locally

```bash
# Start Firebase emulators
npm run serve

# Functions will be available at:
# http://localhost:5001/YOUR-PROJECT-ID/us-central1/translateMessage
```

## Firestore Security Rules

Security rules enforce:
- Users can only read/write their own data
- Conversation participants can read/write messages
- Only Cloud Functions can write to translations cache
- Rate limiting and validation

Deploy rules:
```bash
firebase deploy --only firestore:rules
```

## Firestore Indexes

Composite indexes optimize queries for:
- Messages by conversation and timestamp
- Conversations by participant and update time
- Message delivery status tracking

Deploy indexes:
```bash
firebase deploy --only firestore:indexes
```

## Monitoring

### View Logs

```bash
# All functions
firebase functions:log

# Specific function
firebase functions:log --only translateMessage

# Follow logs in real-time
firebase functions:log --follow
```

### Firebase Console

Monitor usage, errors, and performance:
- https://console.firebase.google.com/
- Navigate to: Functions > Dashboard

## Cost Optimization

### Translation Caching
- Translations are cached in Firestore for 30 days
- Cache hit rate: ~80% (expected)
- Reduces OpenAI API costs significantly

### Rate Limiting
Configure in `env.example.txt`:
```env
MAX_TRANSLATION_REQUESTS_PER_MINUTE=30
MAX_SMART_REPLY_REQUESTS_PER_MINUTE=10
```

### Function Configuration
- Memory: 256MB (default), 512MB (AI functions)
- Timeout: 60s (AI functions), 10s (triggers)
- Region: us-central1 (minimize latency)

## API Keys

**Never commit API keys to version control!**

### Local Development
Store in `.env` file (gitignored)

### Production
Set environment variables:
```bash
firebase functions:config:set openai.api_key="your-key"
firebase deploy --only functions
```

## Troubleshooting

### Functions not deploying
```bash
# Check Firebase CLI version
firebase --version

# Update if needed
npm install -g firebase-tools

# Try deploying with debug
firebase deploy --only functions --debug
```

### Translation errors
```bash
# Check logs
firebase functions:log --only translateMessage

# Common issues:
# - Invalid OpenAI API key
# - Rate limit exceeded
# - Insufficient API credits
```

### Firestore permission denied
```bash
# Redeploy security rules
firebase deploy --only firestore:rules

# Check rules in Firebase Console:
# Firestore > Rules tab
```

## Development Workflow

1. Make changes to functions in `src/`
2. Build TypeScript: `npm run build`
3. Test locally: `npm run serve`
4. Deploy to Firebase: `npm run deploy`
5. Monitor logs: `firebase functions:log`

## Resources

- [Firebase Functions Docs](https://firebase.google.com/docs/functions)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

