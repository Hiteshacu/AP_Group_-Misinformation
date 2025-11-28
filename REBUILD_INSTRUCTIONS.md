# URGENT: Rebuild Instructions

The source code has all my changes, but your device is running old compiled code.

## Step-by-Step Rebuild

1. **In Android Studio**, go to **Build** menu → **Clean Project**
   - Wait for "BUILD SUCCESSFUL" in bottom panel

2. **Build** menu again → **Rebuild Project**  
   - Wait for completion
   - If you see RED errors, screenshot them

3. **Run** menu → **Run 'app'** (or click green play button)
   - Select your device
   - Wait for install to complete

4. **On your phone**:
   - Grant all permissions again
   - Start monitoring
   - Go to Debug Activity and click Clear Cache

5. **Test**: Send the rumor message in Telegram

6.  **Check Logcat** for this EXACT line:
   ```
   🔥🔥🔥 NEW BUILD v2.0 - 2025-11-20 20:02 🔥🔥🔥
   ```

If you see that line, my code is running. If you DON'T see it, Android Studio failed to install the new version.
