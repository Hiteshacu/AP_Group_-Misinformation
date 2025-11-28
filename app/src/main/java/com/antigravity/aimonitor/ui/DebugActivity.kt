package com.antigravity.aimonitor.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.model.FlaggedMessage
import com.antigravity.aimonitor.util.FactCheckCache
import com.antigravity.aimonitor.util.GeminiFactChecker
import com.antigravity.aimonitor.util.ProcessedMessagesCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DebugActivity : AppCompatActivity() {
    
    private lateinit var tvDebugOutput: TextView
    private lateinit var btnTestGemini: Button
    private lateinit var btnCheckCache: Button
    private lateinit var btnTestMessage: Button
    private lateinit var btnTestBadge: Button
    private lateinit var btnSimulateRumor: Button
    private lateinit var btnClearCache: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        
        tvDebugOutput = findViewById(R.id.tvDebugOutput)
        btnTestGemini = findViewById(R.id.btnTestGemini)
        btnCheckCache = findViewById(R.id.btnCheckCache)
        btnTestMessage = findViewById(R.id.btnTestMessage)
        btnTestBadge = findViewById(R.id.btnTestBadge)
        btnSimulateRumor = findViewById(R.id.btnSimulateRumor)
        btnClearCache = findViewById(R.id.btnClearCache)
        
        btnTestGemini.setOnClickListener {
            testGeminiAPI()
        }
        
        btnCheckCache.setOnClickListener {
            checkCache()
        }
        
        btnTestMessage.setOnClickListener {
            testSpecificMessage()
        }
        
        btnTestBadge.setOnClickListener {
            testBadgeDisplay()
        }

        btnSimulateRumor.setOnClickListener {
            simulateRumor()
        }
        
        btnClearCache.setOnClickListener {
            clearCache()
        }
        
        // Add new test button for API diagnostics
        findViewById<Button>(R.id.btnTestAPIs)?.setOnClickListener {
            testBothAPIs()
        }
    }
    
    private fun testBothAPIs() {
        appendLog("========================================")
        appendLog("🧪 TESTING BOTH APIs")
        appendLog("========================================")
        appendLog("Check Logcat for detailed error messages!")
        appendLog("")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    GeminiFactChecker.testAPIs()
                }
                appendLog(result)
            } catch (e: Exception) {
                appendLog("❌ Test failed: ${e.message}")
                Log.e("DebugActivity", "Error testing APIs", e)
            }
        }
    }
    
    private fun testGeminiAPI() {
        appendLog("Testing Gemini API...")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    GeminiFactChecker.analyzeMessage("2+2=5", emptyList())
                }
                
                if (response != null) {
                    appendLog("✅ Gemini API working!")
                    appendLog("isMisinformation: ${response.isMisinformation}")
                    appendLog("confidence: ${response.confidence}")
                    appendLog("label: ${response.label}")
                    appendLog("severity: ${response.severity}")
                    appendLog("isHumor: ${response.isHumor}")
                    appendLog("explanation: ${response.explanation}")
                } else {
                    appendLog("❌ Gemini API returned null")
                    appendLog("Check Logcat for detailed error!")
                }
            } catch (e: Exception) {
                appendLog("❌ Error: ${e.message}")
                Log.e("DebugActivity", "Error testing Gemini", e)
            }
        }
    }
    
    private fun checkCache() {
        appendLog("Checking cache...")
        
        val flaggedMessages = FactCheckCache.getFlaggedMessages()
        
        if (flaggedMessages.isEmpty()) {
            appendLog("❌ Cache is empty")
        } else {
            appendLog("✅ Cache has ${flaggedMessages.size} messages:")
            flaggedMessages.forEach { msg ->
                appendLog("  - ${msg.take(50)}...")
            }
        }
    }
    
    private fun testSpecificMessage() {
        appendLog("Testing message: '2+2=5'")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Test Gemini
                appendLog("1. Calling Gemini API...")
                val response = withContext(Dispatchers.IO) {
                    GeminiFactChecker.analyzeMessage("2+2=5", emptyList())
                }
                
                if (response == null) {
                    appendLog("❌ Gemini returned null")
                    return@launch
                }
                
                appendLog("✅ Gemini response received")
                appendLog("   isMisinformation: ${response.isMisinformation}")
                
                // Test caching
                if (response.isMisinformation) {
                    appendLog("2. Adding to cache...")
                    val flaggedMessage = FlaggedMessage(
                        messageText = "2+2=5",
                        factCheckResponse = response
                    )
                    FactCheckCache.addFlaggedMessage(flaggedMessage)
                    appendLog("✅ Added to cache")
                    
                    // Verify cache
                    val isCached = FactCheckCache.isFlagged("2+2=5")
                    appendLog("3. Verifying cache...")
                    if (isCached) {
                        appendLog("✅ Message found in cache")
                    } else {
                        appendLog("❌ Message NOT in cache")
                    }
                } else {
                    appendLog("⚠️ Gemini says it's NOT misinformation")
                }
                
            } catch (e: Exception) {
                appendLog("❌ Error: ${e.message}")
                Log.e("DebugActivity", "Error in test", e)
            }
        }
    }
    
    private fun testBadgeDisplay() {
        appendLog("Testing badge display...")
        
        // Check overlay permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                appendLog("❌ Overlay permission not granted!")
                appendLog("Go to Settings → Display over other apps")
                return
            }
        }
        
        appendLog("✅ Overlay permission granted")
        
        // Create a test badge at center of screen
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2
        val centerY = displayMetrics.heightPixels / 2
        
        val rect = android.graphics.Rect(
            centerX - 25,
            centerY - 25,
            centerX + 25,
            centerY + 25
        )
        
        appendLog("Displaying test badge at center of screen...")
        appendLog("Position: ($centerX, $centerY)")
        
        try {
            com.antigravity.aimonitor.util.OverlayManager.showBadge(
                this,
                rect,
                "Test Badge - Click to dismiss"
            )
            appendLog("✅ Badge display requested")
            appendLog("You should see a red badge on your screen!")
        } catch (e: Exception) {
            appendLog("❌ Error: ${e.message}")
        }
    }
    
    private fun simulateRumor() {
        appendLog("Simulating rumor detection...")
        
        val message = "I heard they are canceling the exams next week."
        
        // Manually add to cache
        val response = com.antigravity.aimonitor.model.FactCheckResponse(
            isMisinformation = true,
            confidence = 0.8,
            label = "UNVERIFIED",
            explanation = "This is an unverified rumor about exam cancellations.",
            sources = emptyList(),
            severity = "LOW",
            isHumor = false
        )
        
        val flaggedMessage = FlaggedMessage(
            messageText = message,
            factCheckResponse = response
        )
        
        FactCheckCache.addFlaggedMessage(flaggedMessage)
        appendLog("✅ Added rumor to cache")
        appendLog("Now open Telegram and send/view this message:")
        appendLog("'$message'")
        appendLog("You should see a YELLOW badge.")
    }
    
    private fun clearCache() {
        FactCheckCache.clear()
        ProcessedMessagesCache.clear()
        appendLog("✅ Cache cleared successfully")
        appendLog("   - Flagged messages cache cleared")
        appendLog("   - Processed messages cache cleared")
        android.widget.Toast.makeText(this, "All Caches Cleared", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun appendLog(message: String) {
        val currentText = tvDebugOutput.text.toString()
        tvDebugOutput.text = if (currentText.isEmpty()) {
            message
        } else {
            "$currentText\n$message"
        }
        
        // Scroll to bottom
        tvDebugOutput.post {
            val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollView)
            scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }
}
