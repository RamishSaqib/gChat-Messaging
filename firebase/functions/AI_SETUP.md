# AI Features Setup Guide

This guide explains how to set up and deploy the AI translation features for gChat.

## Prerequisites

1. **OpenAI API Key**
   - Sign up at https://platform.openai.com/
   - Create an API key
   - Set up billing (required for GPT-4)

2. **Firebase CLI**
   ```bash
   npm install -g firebase-tools
   firebase login
   ```

## Setup Steps

### 1. Install Dependencies

```bash
cd firebase/functions
npm install
```

This will install:
- `openai` - OpenAI SDK for GPT-4 translation
- `firebase-admin` - Firebase Admin SDK
- `firebase-functions` - Cloud Functions runtime

### 2. Configure Environment Variables

Create a `.env` file in `firebase/functions/`:

```bash
cp .env.example .env
```

Edit `.env` and add your OpenAI API key:

```env
OPENAI_API_KEY=sk-your-actual-api-key-here
OPENAI_MODEL=gpt-4-turbo-preview
TRANSLATION_CACHE_TTL_DAYS=30
MAX_REQUESTS_PER_HOUR=100
```

**IMPORTANT:** Never commit the `.env` file to git! It's already in `.gitignore`.

### 3. Set Firebase Environment Variables

For production deployment, set the environment variables in Firebase:

```bash
cd firebase/functions
firebase functions:config:set openai.api_key="sk-your-actual-api-key-here"
firebase functions:config:set openai.model="gpt-4-turbo-preview"
firebase functions:config:set cache.ttl_days="30"
firebase functions:config:set ratelimit.max_per_hour="100"
```

Or set them all at once:

```bash
firebase functions:secrets:set OPENAI_API_KEY
```

### 4. Deploy Firestore Rules

```bash
cd firebase
firebase deploy --only firestore:rules
```

### 5. Deploy Cloud Functions

Deploy all functions:

```bash
cd firebase
firebase deploy --only functions
```

Or deploy specific functions:

```bash
firebase deploy --only functions:translateMessage
firebase deploy --only functions:detectLanguage
```

## Testing

### Local Testing with Emulator

```bash
cd firebase/functions
npm run serve
```

This starts the Firebase emulators for local testing.

### Test Translation Function

Use the Firebase Console or call from your Android app:

```kotlin
val functions = Firebase.functions
val data = hashMapOf(
    "text" to "Hello, how are you?",
    "sourceLanguage" to "en",
    "targetLanguage" to "es"
)

functions
    .getHttpsCallable("translateMessage")
    .call(data)
    .addOnSuccessListener { result ->
        val translation = result.data as Map<String, Any>
        println("Translated: ${translation["translatedText"]}")
    }
```

## Cost Management

### Translation Costs

GPT-4 Turbo pricing (as of Oct 2024):
- Input: $0.01 / 1K tokens
- Output: $0.03 / 1K tokens

Average translation (50 words):
- ~$0.002 per translation
- With 30-day caching, repeated translations are free

### Rate Limiting

Default limits per user per hour:
- Translation: 100 requests
- Language Detection: 200 requests

Adjust in Cloud Functions code or environment variables.

### Monitoring

Monitor usage in Firebase Console:
1. Go to Functions dashboard
2. Check invocation count and execution time
3. Set up budget alerts in Google Cloud Console

## Supported Languages

GPT-4 supports 20+ major languages:
- English (en)
- Spanish (es)
- French (fr)
- German (de)
- Italian (it)
- Portuguese (pt)
- Russian (ru)
- Japanese (ja)
- Korean (ko)
- Chinese (zh)
- Arabic (ar)
- Hindi (hi)
- And many more...

## Troubleshooting

### "OpenAI API key not found"

Make sure you've set the environment variable:
```bash
firebase functions:config:get openai.api_key
```

If empty, set it:
```bash
firebase functions:config:set openai.api_key="sk-..."
```

### "Rate limit exceeded"

User has hit the hourly limit. Adjust `MAX_REQUESTS_PER_HOUR` or wait.

### "Translation failed"

Check Firebase Functions logs:
```bash
firebase functions:log
```

Common issues:
- Invalid OpenAI API key
- OpenAI API rate limit (different from our rate limit)
- Billing issue with OpenAI account

## Security Best Practices

1. **Never expose API keys client-side**
   - API keys are ONLY in Cloud Functions
   - Android app calls Cloud Functions, not OpenAI directly

2. **Rate limiting**
   - Prevents abuse
   - Protects your OpenAI budget

3. **Firestore rules**
   - Only Cloud Functions can write translations
   - Users can only read translations
   - Rate limits enforced server-side

4. **Cost monitoring**
   - Set up Google Cloud budget alerts
   - Monitor Firebase Functions usage
   - Check OpenAI usage dashboard regularly

## Next Steps

After deployment:
1. Test translation from Android app
2. Monitor costs and usage
3. Adjust rate limits if needed
4. Consider adding more AI features (PR #14, #15)

For issues or questions, check Firebase Functions logs and OpenAI API status.

