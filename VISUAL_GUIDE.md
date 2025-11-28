# Visual Guide - Alert Colors

## 🎨 Color Scheme Overview

Your app now uses a **color-coded alert system** to indicate severity levels:

### Alert Badge Colors

#### 🟡 YELLOW Badge (Default)
Used for:
- **LOW severity** misinformation (rumors, unverified claims)
- **MEDIUM severity** misinformation (misleading content, manipulated media)
- **HUMOR/SATIRE** detection (jokes, memes, sarcasm)

**Color Code**: `#FFC107` (Bright Amber Yellow)

#### 🔴 RED Badge (High Severity Only)
Used for:
- **HIGH severity** misinformation (dangerous health claims, scams, hate speech)

**Color Code**: `#FF1744` (Bright Red)

---

## 📱 Alert Popup Colors

When you click on a badge, a popup appears with detailed information:

### 🟡 Yellow Transparent Box
**Used for**: LOW, MEDIUM severity, and HUMOR
- **Background**: Semi-transparent yellow (`#DDFFC107` - 87% opacity)
- **Text**: White for high contrast
- **Buttons**: 
  - "Dismiss" - Semi-transparent white
  - "Got It!" - Solid white with black text

### 🔴 Red Transparent Box
**Used for**: HIGH severity only
- **Background**: Semi-transparent red (`#DDFF1744` - 87% opacity)
- **Text**: White for high contrast
- **Buttons**: Same as yellow box

---

## 🎯 Visual Examples

### Example 1: Low Severity (Yellow)
```
Message: "I heard John got fired from his job"
Badge: 🟡 Yellow warning icon
Popup: Yellow transparent box
Title: "⚠️ LOW SEVERITY MISINFORMATION"
Label: "UNVERIFIED • LOW SEVERITY"
```

### Example 2: Medium Severity (Yellow)
```
Message: "This video shows politician saying [misleading quote]"
Badge: 🟡 Yellow warning icon
Popup: Yellow transparent box
Title: "⚠️ MEDIUM SEVERITY MISINFORMATION"
Label: "MISLEADING • MEDIUM SEVERITY"
```

### Example 3: High Severity (Red)
```
Message: "Drinking bleach cures COVID-19"
Badge: 🔴 Red warning icon
Popup: Red transparent box
Title: "🚨 HIGH SEVERITY MISINFORMATION"
Label: "FALSE • HIGH SEVERITY"
```

### Example 4: Humor (Yellow)
```
Message: "I'm so broke I ate grass today 😂"
Badge: 🟡 Yellow warning icon
Popup: Yellow transparent box
Title: "😄 HUMOR DETECTED"
Label: "TRUE • HUMOR / SATIRE"
```

---

## 🔧 Technical Details

### Badge Implementation
Location: `OverlayManager.kt` → `showBadge()`

```kotlin
val badgeColor = when {
    severity.uppercase() == "HIGH" -> 0xFFFF1744.toInt() // RED
    else -> 0xFFFFC107.toInt() // YELLOW (default)
}
```

### Popup Implementation
Location: `OverlayManager.kt` → `showAlertPopup()`

```kotlin
val backgroundColor = when {
    response.severity.uppercase() == "HIGH" -> 
        0xDDFF1744.toInt() // Semi-transparent RED
    else -> 
        0xDDFFC107.toInt() // Semi-transparent YELLOW
}
```

### Rounded Corners
Both badge and popup have rounded corners for a modern look:
- Badge: Defined in `badge_background.xml`
- Popup: 16dp corner radius applied programmatically

---

## 🎨 Color Palette Reference

| Element | Color Name | Hex Code | Opacity | Usage |
|---------|-----------|----------|---------|-------|
| Yellow Badge | Amber | `#FFC107` | 100% | LOW/MEDIUM/HUMOR badge |
| Red Badge | Red | `#FF1744` | 100% | HIGH severity badge |
| Yellow Popup | Amber | `#FFC107` | 87% (`#DD`) | LOW/MEDIUM/HUMOR popup |
| Red Popup | Red | `#FF1744` | 87% (`#DD`) | HIGH severity popup |
| White Text | White | `#FFFFFF` | 100% | All text on colored backgrounds |
| Button Dismiss | White | `#FFFFFF` | 20% (`#33`) | Dismiss button background |
| Button Got It | White | `#FFFFFF` | 100% | Got It button background |
| Button Text | Black | `#000000` | 100% | Got It button text |

---

## 📐 Layout Specifications

### Badge
- **Size**: 50dp × 50dp
- **Icon Size**: 30dp × 30dp
- **Shape**: Circular
- **Elevation**: 8dp
- **Position**: Next to flagged message

### Popup
- **Width**: 85% of screen width
- **Height**: Wrap content (auto)
- **Padding**: 20dp all sides
- **Corner Radius**: 16dp
- **Elevation**: 12dp
- **Position**: Near the badge

---

## 🚀 User Experience Flow

1. **Message arrives** → AI analyzes
2. **Misinformation detected** → Badge appears
   - Yellow badge for LOW/MEDIUM/HUMOR
   - Red badge for HIGH severity
3. **User taps badge** → Popup appears
   - Yellow transparent box for LOW/MEDIUM/HUMOR
   - Red transparent box for HIGH severity
4. **User reads explanation** → Understands why it's flagged
5. **User dismisses** → Badge and popup disappear

---

## 💡 Design Rationale

### Why Yellow for Most Alerts?
- **Less alarming**: Yellow is a warning color, not panic-inducing
- **Better UX**: Users won't feel overwhelmed by constant red alerts
- **Clear hierarchy**: Red is reserved for truly dangerous content
- **Humor distinction**: Yellow helps users understand it's not always serious

### Why Red for High Severity?
- **Immediate attention**: Red signals danger and urgency
- **Health & safety**: Critical for dangerous health misinformation
- **Scam prevention**: Helps users avoid financial fraud
- **Clear priority**: Users know to take these seriously

### Why Transparent Backgrounds?
- **Modern design**: Follows Material Design principles
- **Better visibility**: Can see content behind the popup
- **Less intrusive**: Doesn't completely block the screen
- **Professional look**: Polished, app-like appearance

---

## 🔄 Customization Options

Want to change the colors? Edit these values:

### Change Yellow Color
```kotlin
// In OverlayManager.kt
0xFFFFC107.toInt() // Change FFC107 to your hex color
```

### Change Red Color
```kotlin
// In OverlayManager.kt
0xFFFF1744.toInt() // Change FF1744 to your hex color
```

### Change Transparency
```kotlin
// In OverlayManager.kt
0xDDFFC107.toInt() // Change DD (87%) to:
// FF = 100%, EE = 93%, DD = 87%, CC = 80%, BB = 73%, AA = 67%
```

### Change Corner Radius
```kotlin
// In OverlayManager.kt
drawable.cornerRadius = dpToPx(context, 16).toFloat() // Change 16 to desired dp
```

---

## ✅ Testing Your Changes

After building the app:

1. **Test LOW severity** (should be yellow):
   - Send: "I heard there's a new iPhone coming out next week"

2. **Test MEDIUM severity** (should be yellow):
   - Send: "This edited photo proves aliens exist"

3. **Test HIGH severity** (should be red):
   - Send: "Drinking bleach cures COVID-19"

4. **Test HUMOR** (should be yellow):
   - Send: "I'm so hungry I could eat a horse 😂"

---

## 📸 Screenshot Checklist

When testing, verify:
- ✅ Badge color matches severity (yellow or red)
- ✅ Popup background matches badge color
- ✅ Text is readable (white on colored background)
- ✅ Buttons are styled correctly
- ✅ Rounded corners are visible
- ✅ Transparency allows seeing content behind
- ✅ Popup is positioned near the badge

---

**Your app now has a beautiful, intuitive color-coded alert system!** 🎉
