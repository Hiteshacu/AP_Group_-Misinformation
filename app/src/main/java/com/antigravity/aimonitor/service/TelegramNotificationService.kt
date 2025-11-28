package com.antigravity.aimonitor.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.antigravity.aimonitor.model.FlaggedMessage
import com.antigravity.aimonitor.util.FactCheckCache
import com.antigravity.aimonitor.util.GeminiFactChecker
import com.antigravity.aimonitor.util.ProcessedMessagesCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Service that listens to Telegram notifications and analyzes them for misinformation
 * Requires BIND_NOTIFICATION_LISTENER_SERVICE permission
 */
class TelegramNotificationService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "TelegramNotifService"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private val URL_REGEX = Regex("https?://[^\\s]+")
        
        // Supported messaging apps
        private val SUPPORTED_PACKAGES = setOf(
            TELEGRAM_PACKAGE,
            WHATSAPP_PACKAGE,
            WHATSAPP_BUSINESS_PACKAGE
        )
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    /**
     * Called when a new notification is posted
     * Filters for Telegram notifications and analyzes message content
     * NOTE: This service ALWAYS listens when enabled in settings
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (sbn == null) return
        
        Log.d(TAG, "=== NOTIFICATION RECEIVED ===")
        Log.d(TAG, "Package: ${sbn.packageName}")
        
        // Filter for supported messaging apps only
        if (sbn.packageName !in SUPPORTED_PACKAGES) {
            return
        }
        
        val appName = when (sbn.packageName) {
            TELEGRAM_PACKAGE -> "Telegram"
            WHATSAPP_PACKAGE -> "WhatsApp"
            WHATSAPP_BUSINESS_PACKAGE -> "WhatsApp Business"
            else -> "Unknown"
        }
        
        Log.d(TAG, "✅ $appName notification detected!")
        
        try {
            // Check if monitoring is enabled
            if (!com.antigravity.aimonitor.ui.MainActivity.isMonitoringEnabled(this)) {
                Log.d(TAG, "⏸️ Monitoring is disabled - skipping notification")
                return
            }
            
            // Extract message text from notification
            val extras = sbn.notification?.extras
            val messageText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            
            if (messageText.isNullOrBlank()) {
                Log.d(TAG, "No message text found in notification")
                return
            }
            
            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ MESSAGE RECEIVED")
            Log.d(TAG, "📱 Message: \"${messageText}\"")
            Log.d(TAG, "🔍 Sending to AI for analysis...")
            Log.d(TAG, "========================================")
            
            // Extract URLs from message
            val links = extractUrls(messageText)
            
            // Analyze message asynchronously
            analyzeMessage(messageText, links)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    /**
     * Extract URLs from message text using regex
     * @param text The message text to parse
     * @return List of URLs found in the text
     */
    private fun extractUrls(text: String): List<String> {
        return URL_REGEX.findAll(text)
            .map { it.value }
            .toList()
    }
    
    /**
     * Send message to Gemini AI for fact-checking
     * If flagged as misinformation, store in cache
     * @param messageText The message content to analyze
     * @param links List of URLs extracted from the message
     */
    private fun analyzeMessage(messageText: String, links: List<String>) {
        serviceScope.launch {
            try {
                // Check if we've already processed this message
                if (ProcessedMessagesCache.isProcessed(messageText)) {
                    Log.d(TAG, "")
                    Log.d(TAG, "⏭️ SKIPPING - Message already processed")
                    Log.d(TAG, "   Message: \"${messageText.take(50)}...\"")
                    Log.d(TAG, "   ${ProcessedMessagesCache.getStats()}")
                    Log.d(TAG, "")
                    return@launch
                }
                
                Log.d(TAG, "")
                Log.d(TAG, "🤖 Analyzing with AI...")
                
                // Call Gemini AI for fact-checking
                val response = GeminiFactChecker.analyzeMessage(
                    messageText = messageText,
                    links = links
                )
                
                // Mark as processed IMMEDIATELY after analysis (whether flagged or not)
                ProcessedMessagesCache.markAsProcessed(messageText)
                Log.d(TAG, "   ✅ Marked as processed - won't scan again")
                Log.d(TAG, "   ${ProcessedMessagesCache.getStats()}")
                
                if (response == null) {
                    Log.e(TAG, "❌ Failed to get response from Gemini AI")
                    return@launch
                }
                
                Log.d(TAG, "")
                Log.d(TAG, "✅ AI RESPONSE RECEIVED:")
                Log.d(TAG, "   📊 Is Misinformation: ${response.isMisinformation}")
                Log.d(TAG, "   📈 Confidence: ${String.format("%.0f%%", response.confidence * 100)}")
                Log.d(TAG, "   🏷️ Label: ${response.label}")
                Log.d(TAG, "   💬 Explanation: ${response.explanation.take(100)}...")
                
                // Increment message count (always count, even if not flagged)
                com.antigravity.aimonitor.ui.MainActivity.incrementMessageCount(this@TelegramNotificationService)
                Log.d(TAG, "   Message count incremented")
                
                // ⚠️ NEW LOGIC: ONLY show RED badges for HIGH severity
                // NO yellow badges, NO humor badges, ONLY important misinformation
                val needsBadge = response.isMisinformation && response.severity.uppercase() == "HIGH"
                
                Log.d(TAG, "")
                Log.d(TAG, "🎯 BADGE DECISION (NEW LOGIC):")
                Log.d(TAG, "   isMisinformation: ${response.isMisinformation}")
                Log.d(TAG, "   severity: ${response.severity}")
                Log.d(TAG, "   needsBadge: $needsBadge (ONLY HIGH severity gets badge)")
                
                if (needsBadge) {
                    val flaggedMessage = FlaggedMessage(
                        messageText = messageText,
                        factCheckResponse = response
                    )
                    
                    FactCheckCache.addFlaggedMessage(flaggedMessage)
                    Log.d(TAG, "🔴 RED BADGE - High severity misinformation flagged: ${messageText.take(50)}...")
                    Log.d(TAG, "   Severity: ${response.severity}")
                    
                    // Verify it was cached
                    val isCached = FactCheckCache.isFlagged(messageText)
                    Log.d(TAG, "   Cache verification: $isCached")
                    
                    // Log all cached messages for debugging
                    val allCached = FactCheckCache.getFlaggedMessages()
                    Log.d(TAG, "   Total cached messages: ${allCached.size}")
                    allCached.forEachIndexed { index, msg ->
                        Log.d(TAG, "   [$index] ${msg.take(50)}...")
                    }
                    
                    Log.d(TAG, "")
                    Log.d(TAG, "🎨 BADGE COLOR: 🔴 RED (High Severity Only)")
                    Log.d(TAG, "⚠️ NEXT STEP: Open Telegram/WhatsApp to view this message and see RED badge")
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "")
                } else {
                    Log.d(TAG, "")
                    Log.d(TAG, "✅ No badge needed - Not high severity misinformation")
                    Log.d(TAG, "   Reason: ${if (!response.isMisinformation) "Not misinformation" else "Severity is ${response.severity} (only HIGH gets badges)"}")
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error calling Gemini AI: ${e.message}", e)
                // Still mark as processed to avoid retry loops
                ProcessedMessagesCache.markAsProcessed(messageText)
            }
        }
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "🔌 Notification service connected")
        Log.d(TAG, "📢 ALL messages will be analyzed - no duplicate filtering")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 Notification service destroyed")
        // Cancel all coroutines when service is destroyed
        serviceScope.coroutineContext[Job]?.cancel()
    }
}
