# Debugging Guide - Alert System

## 🐛 How to Debug Badge Issues

If you're not seeing the correct badge color or no badge at all, follow these steps:

### Step 1: Check Logcat Output

Connect your device and run:
```bash
adb logcat | findstr /C:"GeminiFactChecker" /C:"TelegramNotifService" /C:"OverlayManager"
```

Or in Android Studio: View → Tool Windows → Logcat

Filter by tags:
- `GeminiFactChecker` - AI analysis
- `TelegramNotifService` - Message detection
- `OverlayManager` - Badge display

---

## 📋 What to Look For

### 1. Message Detection
```
TelegramNotifService: ✅ MESSAGE RECEIVED
TelegramNotifService: 📱 Message: "I heard they are cancelling the exam"
TelegramNotifService: 🔍 Sending to AI for analysis...
```

**If you don't see this**: Notification permission not granted or monitoring disabled

---

### 2. AI Analysis
```
GeminiFactChecker: 🏁 DUAL API RACING STARTED
GeminiFactChecker: 📝 Message: I heard they are cancelling the exam...
GeminiFactChecker: 🏆 WINNER: Groq API
GeminiFactChecker: ⏱️ Response time: 1234ms
```

**If you don't see this**: API keys invalid or no internet

---

### 3. Raw AI Response
```
GeminiFactChecker: 📋 RAW AI RESPONSE:
GeminiFactChecker:    isMisinformation: true
GeminiFactChecker:    isHumor: false
GeminiFactChecker:    severity: LOW
GeminiFactChecker:    label: UNVERIFIED
GeminiFactChecker:    confidence: 0.6
```

**This is the KEY section** - Check what the AI actually returned!

---

### 4. Final Response (After Post-Processing)
```
GeminiFactChecker: 📤 FINAL RESPONSE:
GeminiFactChecker:    severity: LOW
GeminiFactChecker:    isHumor: false
GeminiFactChecker:    isMisinformation: true
```

**Compare with raw response** - Did post-processing change anything?

---

### 5. Badge Decision
```
TelegramNotifService: 🎯 BADGE DECISION:
TelegramNotifService:    isMisinformation: true
TelegramNotifService:    isHumor: false
TelegramNotifService:    severity: LOW
TelegramNotifService:    needsBadge: true
TelegramNotifService: 🎨 BADGE COLOR: 🟡 YELLOW (Low)
```

**This tells you if a badge will show and what color**

---

### 6. Badge Display
```
OverlayManager: Badge styling - Severity: LOW, IsHumor: false
OverlayManager: Applying LOW SEVERITY badge (info circle, yellow)
OverlayManager: Badge applied: Type=LOW, Color=#FFC107
OverlayManager: ✅ Badge displayed successfully at (123, 456)
```

**If you don't see this**: Accessibility service not enabled or overlay permission denied

---

## 🔍 Common Issues & Solutions

### Issue 1: Getting RED badge instead of YELLOW

**Symptoms**: Message like "I heard..." shows red badge

**Check Logcat for**:
```
GeminiFactChecker: 📋 RAW AI RESPONSE:
GeminiFactChecker:    severity: HIGH  ← WRONG!
```

**Cause**: AI incorrectly classified severity

**Solution**: 
1. Check if message has trigger words (bleach, scam, etc.)
2. AI prompt may need adjustment
3. Try adding emoji to make it clearer: "I heard... 🤔"

---

### Issue 2: No badge appearing at all

**Check Logcat for**:
```
TelegramNotifService: 🎯 BADGE DECISION:
TelegramNotifService:    needsBadge: false  ← PROBLEM!
```

**Possible causes**:
- `severity: NONE` (message is true/verified)
- `isMisinformation: false` (not misinformation)
- Message dismissed previously

**Solution**: Check why AI thinks it's not misinformation

---

### Issue 3: Wrong icon showing

**Check Logcat for**:
```
OverlayManager: Badge styling - Severity: LOW, IsHumor: false
OverlayManager: Applying LOW SEVERITY badge (info circle, yellow)
OverlayManager: Badge applied: Type=LOW, Color=#FFC107
```

**Verify**:
- Severity matches expected (LOW/MEDIUM/HIGH/HUMOR)
- Icon resource exists (ic_info, ic_alert_triangle, ic_danger, ic_humor)
- Color code is correct

---

### Issue 4: Humor detected as misinformation

**Check Logcat for**:
```
GeminiFactChecker: 📋 RAW AI RESPONSE:
GeminiFactChecker:    isHumor: false  ← Should be true!
GeminiFactChecker:    isMisinformation: true
```

**Cause**: Not enough humor indicators

**Solution**: Add more humor markers:
- Laughing emojis: 😂🤣😆
- Exaggeration: "literally", "so broke"
- Slang: "lol", "lmao"

---

## 🧪 Test Cases

### Test 1: Low Severity Rumor (Should be YELLOW)
```
Message: "I heard they are cancelling the exam next week"

Expected Logcat:
✓ severity: LOW
✓ isHumor: false
✓ isMisinformation: true
✓ needsBadge: true
✓ BADGE COLOR: 🟡 YELLOW (Low)
✓ Badge applied: Type=LOW, Color=#FFC107
```

### Test 2: Humor (Should be BLUE)
```
Message: "I'm so broke I ate grass today 😂"

Expected Logcat:
✓ severity: NONE
✓ isHumor: true
✓ isMisinformation: false
✓ needsBadge: true
✓ BADGE COLOR: 🔵 BLUE (Humor)
✓ Badge applied: Type=HUMOR, Color=#2196F3
```

### Test 3: High Severity (Should be RED)
```
Message: "Drinking bleach cures COVID-19"

Expected Logcat:
✓ severity: HIGH
✓ isHumor: false
✓ isMisinformation: true
✓ needsBadge: true
✓ BADGE COLOR: 🔴 RED (High)
✓ Badge applied: Type=HIGH, Color=#FF1744
```

### Test 4: True Message (Should be NO BADGE)
```
Message: "The weather is nice today"

Expected Logcat:
✓ severity: NONE
✓ isHumor: false
✓ isMisinformation: false
✓ needsBadge: false
✓ Message is TRUE/VERIFIED - No badge needed
```

---

## 🔧 Advanced Debugging

### Enable Verbose Logging

The app already has detailed logging. Just filter Logcat:

**Android Studio**:
1. Open Logcat
2. Select your device
3. Filter: `GeminiFactChecker|TelegramNotifService|OverlayManager`

**Command Line**:
```bash
# Windows
adb logcat | findstr /C:"GeminiFactChecker" /C:"TelegramNotifService" /C:"OverlayManager"

# Linux/Mac
adb logcat | grep -E "GeminiFactChecker|TelegramNotifService|OverlayManager"
```

### Check API Response

Look for the full JSON response:
```
GeminiFactChecker: ✅ Groq response received: {"isMisinformation":true,"confidence":0.6,"label":"UNVERIFIED","explanation":"...","sources":[],"severity":"LOW","isHumor":false}
```

### Verify Badge Logic

The badge decision logic is:
```kotlin
needsBadge = (isMisinformation && severity != "NONE") || 
             (isHumor && severity == "NONE")
```

**Examples**:
- `isMisinformation=true, severity=LOW` → needsBadge=true ✓
- `isMisinformation=true, severity=NONE` → needsBadge=false ✗
- `isHumor=true, severity=NONE` → needsBadge=true ✓
- `isMisinformation=false, severity=NONE` → needsBadge=false ✗

---

## 📊 Severity Classification Logic

The AI uses these rules:

### HIGH Severity
- Health: "drink bleach", "vaccines cause autism"
- Financial: "send money", "guaranteed returns"
- Safety: threats, violence
- Scams: phishing, fake prizes

### MEDIUM Severity
- Manipulated media: edited videos
- Cherry-picked facts
- Conspiracy theories
- Political misinformation

### LOW Severity
- Rumors: "I heard..."
- Gossip: celebrity rumors
- Opinions as facts
- Unverified claims

### NONE Severity
- Verified facts
- Personal opinions (clearly stated)
- Humor/satire

---

## 🚨 If Nothing Works

### 1. Clean and Rebuild
```
Build → Clean Project
Build → Rebuild Project
```

### 2. Reinstall App
```bash
adb uninstall com.antigravity.aimonitor
# Then install fresh APK
```

### 3. Check Permissions
- Notification Access: Settings → Apps → Special Access → Notification Access
- Accessibility: Settings → Accessibility → AI Monitor
- Overlay: Settings → Apps → Special Access → Display over other apps

### 4. Verify API Keys
Check `ApiClient.kt`:
```kotlin
const val GEMINI_API_KEY = "AIza..." // Should start with AIza
const val GROQ_API_KEY = "gsk_..." // Should start with gsk_
```

### 5. Test APIs Manually
Use DebugActivity to test both APIs

---

## 📝 Reporting Issues

If you still have problems, provide:

1. **Full Logcat output** (filtered by tags above)
2. **Test message** you sent
3. **Expected behavior** (what badge color you expected)
4. **Actual behavior** (what badge color you got)
5. **Screenshots** if possible

---

## ✅ Success Indicators

You'll know it's working when you see:

```
TelegramNotifService: ✅ MESSAGE RECEIVED
GeminiFactChecker: 🏆 WINNER: Groq API
GeminiFactChecker: 📋 RAW AI RESPONSE: severity: LOW
GeminiFactChecker: 📤 FINAL RESPONSE: severity: LOW
TelegramNotifService: 🎨 BADGE COLOR: 🟡 YELLOW (Low)
OverlayManager: Badge applied: Type=LOW, Color=#FFC107
OverlayManager: ✅ Badge displayed successfully
```

**Then open Telegram and you'll see the yellow badge!** 🟡

---

**Happy debugging!** 🐛🔍
