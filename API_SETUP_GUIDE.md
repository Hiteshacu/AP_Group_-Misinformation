# API Setup Guide

This guide will help you set up your own API keys for the AI Misinformation Monitor app.

## 🔑 Why You Need API Keys

The app uses two AI services to analyze messages:
1. **Gemini AI** (Google) - Primary AI for fact-checking
2. **Groq AI** - Fast fallback AI for speed optimization

Both services offer free tiers that are sufficient for personal use.

## 📝 Step 1: Get Gemini API Key (Google)

### Create Account
1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Accept the terms of service

### Generate API Key
1. Click "Get API Key" or "Create API Key"
2. Select "Create API key in new project" (or use existing project)
3. Copy the generated API key (starts with `AIza...`)
4. **Important**: Keep this key private!

### Free Tier Limits
- **60 requests per minute**
- **1,500 requests per day**
- Sufficient for monitoring multiple chats

### Pricing (if you exceed free tier)
- Very affordable: ~$0.00025 per request
- Most users stay within free tier

## 🚀 Step 2: Get Groq API Key

### Create Account
1. Go to [Groq Console](https://console.groq.com/)
2. Sign up with email or GitHub
3. Verify your email

### Generate API Key
1. Navigate to [API Keys](https://console.groq.com/keys)
2. Click "Create API Key"
3. Give it a name (e.g., "AI Monitor App")
4. Copy the generated key (starts with `gsk_...`)
5. **Important**: Save it immediately - you can't view it again!

### Free Tier Limits
- **30 requests per minute**
- **14,400 requests per day**
- Ultra-fast responses (< 1 second)

### Pricing
- Currently FREE for all users
- May introduce paid tiers in future

## 🔧 Step 3: Add Keys to Your App

### Option A: Direct Code Update (Simple)

1. Open Android Studio
2. Navigate to: `app/src/main/java/com/antigravity/aimonitor/data/ApiClient.kt`
3. Find these lines:
```kotlin
const val GEMINI_API_KEY = "YOUR_GEMINI_KEY_HERE"
const val GROQ_API_KEY = "YOUR_GROQ_KEY_HERE"
```
4. Replace with your actual keys:
```kotlin
const val GEMINI_API_KEY = "YOUR_GEMINI_KEY_HERE"
const val GROQ_API_KEY = "YOUR_GROQ_KEY_HERE"
```
4. Replace with your actual keys:
```kotlin
const val GEMINI_API_KEY = "YOUR_GEMINI_KEY_HERE"
const val GROQ_API_KEY = "YOUR_GROQ_KEY_HERE"
```
5. Rebuild the app

### Option B: Environment Variables (Secure)

For better security, use BuildConfig:

1. Open `app/build.gradle.kts`
2. Add to `defaultConfig`:
```kotlin
defaultConfig {
    // ... existing config
    
    buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
    buildConfigField("String", "GROQ_API_KEY", "\"${System.getenv("GROQ_API_KEY") ?: ""}\"")
}
```

3. Update `ApiClient.kt`:
```kotlin
const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
const val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
```

4. Set environment variables:
```bash
# Linux/Mac
export GEMINI_API_KEY="your_key_here"
export GROQ_API_KEY="your_key_here"

# Windows (PowerShell)
$env:GEMINI_API_KEY="your_key_here"
$env:GROQ_API_KEY="your_key_here"
```

### Option C: local.properties (Recommended)

1. Open/create `local.properties` in project root
2. Add your keys:
```properties
gemini.api.key=YOUR_GEMINI_KEY_HERE
groq.api.key=YOUR_GROQ_KEY_HERE
```

3. Update `app/build.gradle.kts`:
```kotlin
// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    defaultConfig {
        // ... existing config
        
        buildConfigField("String", "GEMINI_API_KEY", 
            "\"${localProperties.getProperty("gemini.api.key", "")}\"")
        buildConfigField("String", "GROQ_API_KEY", 
            "\"${localProperties.getProperty("groq.api.key", "")}\"")
    }
}
```

4. Update `ApiClient.kt`:
```kotlin
const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
const val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
```

5. Add to `.gitignore`:
```
local.properties
```

## ✅ Step 4: Test Your Setup

### Using Debug Activity
1. Build and install the app
2. Open the app
3. Tap "Debug" button (if available)
4. Check API test results

### Manual Testing
1. Enable all permissions
2. Start monitoring
3. Send a test message in Telegram: "Breaking: Drinking bleach cures COVID"
4. Check Logcat for API responses:
```bash
adb logcat | grep -E "GeminiFactChecker|TelegramNotifService"
```

### Expected Output
```
GeminiFactChecker: 🏁 DUAL API RACING STARTED
GeminiFactChecker: 🏆 WINNER: Groq API
GeminiFactChecker: ⏱️ Response time: 1234ms
GeminiFactChecker: ✅ Misinformation: true
GeminiFactChecker: 🏷️ Label: FALSE
```

## 🔒 Security Best Practices

### DO:
✅ Use `local.properties` or environment variables
✅ Add `local.properties` to `.gitignore`
✅ Rotate keys periodically
✅ Use separate keys for development and production
✅ Monitor API usage in dashboards

### DON'T:
❌ Commit API keys to Git
❌ Share keys publicly
❌ Use production keys in debug builds
❌ Hardcode keys in release builds
❌ Share your APK with keys embedded

## 📊 Monitoring Usage

### Gemini AI
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Navigate to "APIs & Services" → "Dashboard"
4. View Gemini API usage

### Groq AI
1. Go to [Groq Console](https://console.groq.com/)
2. Check usage dashboard
3. Monitor rate limits

## 🚨 Troubleshooting

### "API Key Invalid" Error
- Double-check you copied the entire key
- Ensure no extra spaces or quotes
- Verify key is enabled in console
- Check if key has expired

### "Quota Exceeded" Error
- Wait for quota to reset (usually 24 hours)
- Upgrade to paid tier if needed
- Use both APIs for redundancy

### "Network Error"
- Check internet connection
- Verify firewall isn't blocking API calls
- Try using VPN if APIs are blocked in your region

### Both APIs Failing
- Check Logcat for detailed errors
- Verify API keys are correct
- Test APIs directly using curl:

```bash
# Test Gemini
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=YOUR_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"contents":[{"parts":[{"text":"Hello"}]}]}'

# Test Groq
curl "https://api.groq.com/openai/v1/chat/completions" \
  -H "Authorization: Bearer YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"llama-3.1-8b-instant","messages":[{"role":"user","content":"Hello"}]}'
```

## 💡 Tips

1. **Start with free tiers** - They're usually sufficient
2. **Monitor usage** - Set up alerts in API consoles
3. **Use both APIs** - The app automatically falls back if one fails
4. **Keep keys secure** - Treat them like passwords
5. **Test before deploying** - Always verify keys work before building release APK

## 📞 Support

If you encounter issues:
1. Check API console for errors
2. Review Logcat logs
3. Verify internet connectivity
4. Test with curl commands above
5. Check API service status pages

---

**Ready to go?** Once you've added your keys, rebuild the app and start monitoring! 🚀
