# Test AI Response

## 🧪 How to Check What AI is Returning

### Step 1: Send Your Test Message

Send this message in Telegram:
```
I heard they are cancelling the exam next week
```

### Step 2: Check Logcat

Run this command:
```bash
adb logcat -c  # Clear log
adb logcat | findstr "GeminiFactChecker"
```

### Step 3: Look for These Lines

You should see:

```
GeminiFactChecker: 📋 RAW AI RESPONSE:
GeminiFactChecker:    isMisinformation: true
GeminiFactChecker:    isHumor: false
GeminiFactChecker:    severity: LOW
GeminiFactChecker:    label: UNVERIFIED
GeminiFactChecker:    confidence: 0.6
```

**This is what the AI actually returned!**

### Step 4: Check Final Response

```
GeminiFactChecker: 🚨 Misinformation detected - keeping severity: LOW
GeminiFactChecker: 📤 FINAL RESPONSE:
GeminiFactChecker:    severity: LOW
GeminiFactChecker:    isHumor: false
GeminiFactChecker:    isMisinformation: true
```

**This is after post-processing**

### Step 5: Check Badge Decision

```
TelegramNotifService: 🎯 BADGE DECISION:
TelegramNotifService:    isMisinformation: true
TelegramNotifService:    isHumor: false
TelegramNotifService:    severity: LOW
TelegramNotifService:    needsBadge: true
TelegramNotifService: 🎨 BADGE COLOR: 🟡 YELLOW (Low)
```

**This tells you if badge will show**

### Step 6: Check Badge Display

```
OverlayManager: Badge styling - Severity: LOW, IsHumor: false
OverlayManager: Applying LOW SEVERITY badge (info circle, yellow)
OverlayManager: Badge applied: Type=LOW, Color=#FFC107
OverlayManager: ✅ Badge displayed successfully at (123, 456)
```

**Badge should now be visible in Telegram!**

---

## 🔍 Troubleshooting

### If AI Returns Wrong Severity

**Problem**: AI returns `severity: HIGH` instead of `LOW`

**Check**:
```
GeminiFactChecker: 📋 RAW AI RESPONSE:
GeminiFactChecker:    severity: HIGH  ← WRONG!
```

**Possible causes**:
1. Message has trigger words (bleach, scam, etc.)
2. AI misunderstood context
3. API key issue (using wrong model)

**Solution**: 
- Add more context: "I heard they are cancelling the exam next week (just a rumor)"
- Or add emoji: "I heard they are cancelling the exam 🤔"

---

### If Badge Not Showing

**Problem**: Logcat shows correct severity but no badge

**Check**:
```
TelegramNotifService: 🎯 BADGE DECISION:
TelegramNotifService:    needsBadge: false  ← PROBLEM!
```

**Possible causes**:
1. Accessibility service not enabled
2. Overlay permission denied
3. Message already dismissed

**Solution**:
1. Go to Settings → Accessibility → Enable "AI Monitor"
2. Go to Settings → Apps → Special Access → Display over other apps → Enable
3. Clear app data and try again

---

### If Getting RED Instead of YELLOW

**Problem**: Badge shows but wrong color

**Check**:
```
OverlayManager: Badge applied: Type=HIGH, Color=#FF1744  ← Should be LOW!
```

**This means AI classified it as HIGH severity**

**Solution**: The AI prompt needs adjustment. Try:
- "I heard they might cancel the exam"
- "Rumor: exam cancelled next week"
- "Someone said exam is cancelled (unconfirmed)"

---

## 📊 Expected Results for Common Messages

### Test 1: Rumor (Should be YELLOW)
```
Message: "I heard they are cancelling the exam"

Expected:
✓ isMisinformation: true
✓ isHumor: false
✓ severity: LOW
✓ label: UNVERIFIED
✓ Badge: 🟡 YELLOW (info circle)
```

### Test 2: Humor (Should be BLUE)
```
Message: "I'm so broke I ate grass 😂"

Expected:
✓ isMisinformation: false
✓ isHumor: true
✓ severity: NONE
✓ label: TRUE
✓ Badge: 🔵 BLUE (smiley)
```

### Test 3: Dangerous (Should be RED)
```
Message: "Drinking bleach cures COVID"

Expected:
✓ isMisinformation: true
✓ isHumor: false
✓ severity: HIGH
✓ label: FALSE
✓ Badge: 🔴 RED (octagon)
```

### Test 4: True (Should be NO BADGE)
```
Message: "The weather is nice today"

Expected:
✓ isMisinformation: false
✓ isHumor: false
✓ severity: NONE
✓ label: TRUE
✓ Badge: None (no badge)
```

---

## 🚨 If Still Not Working

### 1. Check API Keys

Open `ApiClient.kt` and verify:
```kotlin
const val GEMINI_API_KEY = "AIza..." // Should start with AIza
const val GROQ_API_KEY = "gsk_..." // Should start with gsk_
```

### 2. Test APIs Manually

In DebugActivity, tap "Test APIs" button

Expected output:
```
✅ Gemini: SUCCESS
✅ Groq: SUCCESS
```

### 3. Check Internet Connection

```bash
adb shell ping -c 3 google.com
```

### 4. Clear App Data

```bash
adb shell pm clear com.antigravity.aimonitor
```

Then reinstall and grant permissions again.

---

## 📝 Share Logcat Output

If still not working, share the FULL logcat output:

```bash
adb logcat -c
# Send your test message
adb logcat -d > logcat.txt
```

Then share `logcat.txt` file.

Look for these sections:
1. **Message Detection** (TelegramNotifService)
2. **AI Analysis** (GeminiFactChecker)
3. **Badge Display** (OverlayManager)

---

## ✅ Success Indicators

You'll know it's working when:

1. **Logcat shows**:
   ```
   📋 RAW AI RESPONSE: severity: LOW
   📤 FINAL RESPONSE: severity: LOW
   🎨 BADGE COLOR: 🟡 YELLOW (Low)
   Badge applied: Type=LOW, Color=#FFC107
   ```

2. **Telegram shows**: Yellow info circle badge (ℹ️) next to message

3. **Clicking badge**: Yellow popup with "ℹ️ LOW SEVERITY MISINFORMATION"

---

**If you see all these, it's working perfectly!** 🎉
