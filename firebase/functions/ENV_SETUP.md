# Environment Variable Setup

This file contains instructions for setting up environment variables for AI features.

## Local Development (.env file)

Create a `.env` file in `firebase/functions/` directory:

```env
OPENAI_API_KEY=sk-your-openai-api-key-here
OPENAI_MODEL=gpt-4-turbo-preview
TRANSLATION_CACHE_TTL_DAYS=30
MAX_REQUESTS_PER_HOUR=100
```

**IMPORTANT:** The `.env` file is in `.gitignore` and should NEVER be committed to git!

## Production Deployment

For production, set environment variables in Firebase:

```bash
# Set OpenAI API key as a secret
firebase functions:secrets:set OPENAI_API_KEY

# Or use config (less secure for API keys)
firebase functions:config:set openai.api_key="sk-your-actual-key"
firebase functions:config:set openai.model="gpt-4-turbo-preview"
```

## Getting Your OpenAI API Key

1. Go to https://platform.openai.com/
2. Sign up or login
3. Navigate to API Keys section
4. Create a new secret key
5. Copy the key (starts with `sk-`)
6. Set up billing (required for GPT-4)

## Testing the Configuration

After setting up, test locally with Firebase emulators:

```bash
cd firebase/functions
npm run serve
```

Then call the function from Android app or use Firebase Console.

