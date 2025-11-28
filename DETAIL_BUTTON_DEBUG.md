# Detail Button Debug Guide

## What to Check

When you click the "Detail" button, check Logcat for these logs in order:

### 1. Button Click (OverlayManager)
```
🔍 DETAIL BUTTON CLICKED
   Message: "..."
   Launching DetailSourcesActivity...
✅ Intent sent successfully
```
**If you DON'T see this** → Button click handler not working

### 2. Activity Launch (DetailSourcesActivity)
```
🚀 DetailSourcesActivity onCreate called
📩 Message text from intent: ...
✅ Message text received, initializing views...
🔍 Starting source scan...
```
**If you DON'T see this** → Activity not launching (check manifest)

### 3. Source Scanning (SourceScanner)
```
📡 scanSources() called
🌐 Starting web source scan...
   Calling SourceScanner.scanSources()...
📥 Scan completed, result: SUCCESS/NULL
```
**If you DON'T see this** → Scanning not starting

### 4. Results Display
```
✅ Got X sources, displaying results...
Displayed X sources
```

## Common Issues

### Issue 1: Nothing in Logcat
**Solution**: Filter Logcat by `DetailSourcesActivity` or `OverlayManager`

### Issue 2: "Message text is null"
**Problem**: Intent not passing message correctly
**Check**: Button click handler uses correct extra name

### Issue 3: Activity crashes immediately
**Look for**: Stack trace in Logcat
**Common cause**: Missing view IDs or layout issues

### Issue 4: Blank/Transparent screen
**Problem**: Theme makes activity invisible
**Solution**: Already set to translucent theme in manifest

## How to Test

1. **Rebuild** the app
2. Send a test message in Telegram
3. Click the badge
4. Click **"🔍 Detail"** button
5. **Watch Logcat** - filter by "DetailSourcesActivity"
6. Copy and send me the logs you see (or tell me what's missing)
