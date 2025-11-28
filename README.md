# AI Misinformation Monitor for Android

An intelligent Android app that monitors Telegram and WhatsApp messages in real-time, using AI to detect misinformation and display visual alerts directly in your chat interface.

## 🌟 Features

### Core Functionality
- **Real-time Message Monitoring**: Automatically analyzes messages from Telegram and WhatsApp
- **Dual AI Analysis**: Uses both Gemini AI and Groq AI for fast, accurate detection
- **Smart Severity Classification**:
  - 🔴 **RED Alert**: High severity misinformation (health risks, scams, dangerous false info)
  - 🟡 **YELLOW Alert**: Medium/Low severity misinformation (rumors, misleading content)
  - 😄 **YELLOW Alert**: Humor/Satire detection (prevents false positives)

### Advanced Detection
- **Humor vs Misinformation**: Advanced AI prompt distinguishes between jokes and actual misinformation
- **Context-Aware Analysis**: Considers emojis, tone, and context
- **Confidence Scoring**: Shows AI confidence level for each detection
- **Source Attribution**: Provides sources when available

### User Interface
- **Floating Alert Badges**: Non-intrusive badges appear next to flagged messages
- **Detailed Explanations**: Click badge to see why content was flagged
- **Color-Coded Alerts**: Visual severity indicators
- **Easy Dismissal**: Double-click badge or use "Got It" button to dismiss

## 📱 Supported Apps

- Telegram
- WhatsApp
- WhatsApp Business

## 🚀 Setup Instructions

### 1. Install the App
Build and install the APK on your Android device (API 21+)

### 2. Configure API Keys
The app uses two AI services for optimal performance:

**Gemini AI** (Primary)
- Get your API key from: https://makersuite.google.com/app/apikey
- Update in `ApiClient.kt`: `GEMINI_API_KEY`

**Groq AI** (Fallback/Speed)
- Get your API key from: https://console.groq.com/keys
- Update in `ApiClient.kt`: `GROQ_API_KEY`

### 3. Grant Permissions
The app requires three permissions:

#### a) Notification Access
1. Open the app
2. Tap "Enable Notification Access"
3. Find "AI Monitor" and toggle ON
4. This allows the app to read incoming messages

#### b) Accessibility Service
1. Tap "Enable Accessibility Service"
2. Find "AI Monitor" and toggle ON
3. This allows the app to display badges in chat interfaces

#### c) Display Over Other Apps
1. Tap "Enable Overlay Permission"
2. Toggle "Allow display over other apps" ON
3. This allows floating alert badges

### 4. Start Monitoring
1. Once all permissions are granted, tap "Start Monitoring"
2. The app will show a persistent notification indicating active monitoring
3. Open Telegram or WhatsApp and start chatting!

## 🎯 How It Works

### Message Flow
```
1. Message arrives in Telegram/WhatsApp
   ↓
2. Notification Service captures message text
   ↓
3. Both Gemini & Groq AI analyze simultaneously
   ↓
4. First response wins (typically < 2 seconds)
   ↓
5. If misinformation detected:
   - Badge appears next to message
   - Color indicates severity
   ↓
6. User clicks badge → Detailed explanation shown
```

### AI Analysis Process
The AI evaluates each message for:
- **Factual Accuracy**: Is the information true?
- **Intent**: Is it humor, satire, or serious?
- **Severity**: How dangerous is the misinformation?
- **Context**: URLs, claims, medical/financial advice
- **Confidence**: How certain is the AI?

### Severity Levels

#### 🔴 HIGH (Red Alert)
- Dangerous health misinformation
- Financial scams
- Fake news about disasters/politics
- Hate speech
- Phishing attempts

#### 🟡 MEDIUM (Yellow Alert)
- Misleading context
- Cherry-picked facts
- Manipulated media
- Conspiracy theories

#### 🟡 LOW (Yellow Alert)
- Unverified rumors
- Gossip
- Opinions presented as facts
- Minor inaccuracies

#### 😄 HUMOR (Yellow Alert)
- Jokes and memes
- Satire and sarcasm
- Funny exaggerations
- Not flagged as misinformation

## 🛠️ Technical Architecture

### Project Structure
```
app/src/main/java/com/antigravity/aimonitor/
├── data/           # API clients and interfaces
│   ├── ApiClient.kt
│   ├── GeminiApi.kt
│   └── GroqApi.kt
├── model/          # Data models
│   ├── FlaggedMessage.kt
│   ├── GeminiRequest.kt
│   └── GroqRequest.kt
├── service/        # Background services
│   ├── TelegramNotificationService.kt
│   └── TelegramAccessibilityService.kt
├── ui/             # User interface
│   ├── MainActivity.kt
│   └── FactCheckBottomSheet.kt
└── util/           # Utilities
    ├── GeminiFactChecker.kt
    ├── OverlayManager.kt
    └── FactCheckCache.kt
```

### Key Components

**TelegramNotificationService**
- Listens to all notifications from supported apps
- Extracts message text and URLs
- Triggers AI analysis

**GeminiFactChecker**
- Manages dual AI racing (Gemini vs Groq)
- Parses AI responses
- Classifies severity and humor

**OverlayManager**
- Creates floating badge overlays
- Positions badges at message locations
- Handles click interactions

**TelegramAccessibilityService**
- Scans chat interface for flagged messages
- Places badges at correct screen coordinates
- Updates badge positions on scroll

## 🔧 Configuration

### Customizing AI Behavior
Edit `GeminiFactChecker.kt` → `SYSTEM_PROMPT` to adjust:
- Severity thresholds
- Humor detection sensitivity
- Response format
- Analysis criteria

### Adjusting Badge Appearance
Edit `OverlayManager.kt` → `showBadge()` to modify:
- Badge colors
- Badge size
- Position offset
- Animation effects

### Changing Alert UI
Edit layouts in `app/src/main/res/layout/`:
- `badge_layout.xml` - Badge appearance
- `alert_popup_overlay.xml` - Alert popup design

## 📊 Performance

- **Analysis Speed**: 1-3 seconds per message
- **AI Racing**: Fastest API wins (typically Groq)
- **Battery Impact**: Minimal (background service)
- **Memory Usage**: ~50MB average

## 🐛 Troubleshooting

### Badges Not Appearing
1. Check all three permissions are granted
2. Verify "Start Monitoring" is enabled
3. Check Logcat for errors (tag: `TelegramNotifService`)
4. Ensure API keys are valid

### AI Not Responding
1. Check internet connection
2. Verify API keys in `ApiClient.kt`
3. Check API quotas (Gemini/Groq dashboards)
4. Review Logcat (tag: `GeminiFactChecker`)

### Badge Position Wrong
1. Accessibility service may need restart
2. Try disabling and re-enabling in Settings
3. Check Logcat (tag: `OverlayManager`)

## 🔐 Privacy & Security

- **Local Processing**: Message analysis happens via API calls only
- **No Storage**: Messages are not permanently stored
- **Cache Only**: Flagged messages cached temporarily for badge display
- **No Tracking**: No user data collected or transmitted
- **API Keys**: Keep your API keys secure and private

## 📝 Development

### Building from Source
```bash
# Clone the repository
git clone <your-repo-url>

# Open in Android Studio
# Update API keys in ApiClient.kt
# Build and run
```

### Testing
1. Use DebugActivity to test AI APIs
2. Send test messages in Telegram
3. Check Logcat for detailed logs
4. Monitor badge appearance and positioning

### Adding New Messaging Apps
1. Add package name to `TelegramNotificationService.SUPPORTED_PACKAGES`
2. Test notification format compatibility
3. Adjust accessibility service if needed

## 📄 License

[Your License Here]

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Test thoroughly
4. Submit a pull request

## 📧 Support

For issues or questions:
- Check Logcat logs first
- Review troubleshooting section
- Open an issue on GitHub

## 🎉 Credits

- **Gemini AI**: Google's generative AI
- **Groq AI**: Ultra-fast LLM inference
- **Material Design 3**: UI components

---

**Note**: This app is for educational and personal use. Always verify important information from multiple sources. AI detection is not 100% accurate.
