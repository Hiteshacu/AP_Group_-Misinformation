package com.antigravity.aimonitor.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.antigravity.aimonitor.util.FactCheckCache
import com.antigravity.aimonitor.util.OverlayManager
import com.antigravity.aimonitor.util.GeminiFactChecker
import com.antigravity.aimonitor.util.DismissedMessagesCache
import com.antigravity.aimonitor.model.FlaggedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Accessibility service that monitors Telegram UI to detect flagged messages
 * and display warning badges at their screen positions
 * Requires BIND_ACCESSIBILITY_SERVICE permission
 */
class TelegramAccessibilityService : AccessibilityService() {
    // List of domains that should trigger a red‑alert badge
    private val BLACKLISTED_DOMAINS = setOf(
        "iplogger.com",
        "maper.info",
        "iplogger.ru",
        "iplogger.co",
        "2no.co",
        "yip.su",
        "iplogger.info",
        "iplis.ru",
        "ezstat.ru",
        "iplog.co",
        "iplogger.cn",
        "grabify.link",
        "gg.gg",
        "shorte.st",
        "shorturl.at",
        "adf.ly",
        "bc.vc",
        "ouo.io",
        "adfoc.us",
        "goo.gl"
    )
    
    companion object {
        private const val TAG = "TelegramA11yService"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        private const val DEBOUNCE_DELAY_MS = 10L // Ultra-fast response - 10ms for instant badges
        
        // Supported messaging apps
        private val SUPPORTED_PACKAGES = setOf(
            TELEGRAM_PACKAGE,
            WHATSAPP_PACKAGE,
            WHATSAPP_BUSINESS_PACKAGE
        )
    }
    
    private var lastProcessTime = 0L
    
    // Track which messages have badges to update their positions
    private val trackedMessages = mutableSetOf<String>()
    
    // Track which chat we're currently in (to detect chat changes)
    private var currentChatId: String? = null
    
    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    /**
     * Called when an accessibility event occurs
     * Monitors Telegram window changes to detect flagged messages
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        Log.d(TAG, "=== ACCESSIBILITY EVENT ===")
        Log.d(TAG, "Package: ${event.packageName}")
        Log.d(TAG, "Event type: ${event.eventType}")
        
        // If event is from a different app (not our supported apps), remove badges
        if (event.packageName !in SUPPORTED_PACKAGES) {
            // User switched to a different app or closed Telegram
            if (OverlayManager.getActiveBadgeCount() > 0) {
                Log.d(TAG, "📴 Left messaging app - Removing all badges")
                OverlayManager.removeAllBadges()
                trackedMessages.clear()
            }
            return
        }
        
        val appName = when (event.packageName) {
            TELEGRAM_PACKAGE -> "Telegram"
            WHATSAPP_PACKAGE -> "WhatsApp"
            WHATSAPP_BUSINESS_PACKAGE -> "WhatsApp Business"
            else -> "Unknown"
        }
        
        Log.d(TAG, "✅ $appName event detected")
        
        // Handle window state changes (app opened/closed, chat opened/closed)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleWindowStateChange(event)
        }
        
        // Filter for relevant event types for message detection
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return
        }
        
        // Light debounce: prevent excessive processing
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime < DEBOUNCE_DELAY_MS) {
            return
        }
        lastProcessTime = currentTime
        
        try {
            // Check if monitoring is enabled
            if (!com.antigravity.aimonitor.ui.MainActivity.isMonitoringEnabled(this)) {
                Log.d(TAG, "⏸️ Monitoring is disabled - skipping scan")
                // Remove any existing badges
                OverlayManager.removeAllBadges()
                return
            }
            
            // Check cache before scanning
            val cachedMessages = FactCheckCache.getFlaggedMessages()
            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "📦 Cache has ${cachedMessages.size} flagged messages")
            if (cachedMessages.isNotEmpty()) {
                Log.d(TAG, "📋 Cached messages:")
                cachedMessages.forEachIndexed { index, msg ->
                    Log.d(TAG, "   [$index] \"${msg.take(50)}...\"")
                }
            } else {
                Log.d(TAG, "⚠️ Cache is EMPTY - no messages to show badges for")
            }
            Log.d(TAG, "========================================")
            
            // Get root node of active window
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.d(TAG, "❌ Root node is null")
                return
            }
            
            Log.d(TAG, "🔍 Starting UI scan...")
            Log.d(TAG, "Looking for ${cachedMessages.size} flagged messages...")
            
            // Traverse node tree to find flagged messages
            traverseNodeTree(rootNode)
            
            // Log summary after scan
            val activeBadges = OverlayManager.getActiveBadgeCount()
            Log.d(TAG, "")
            Log.d(TAG, "📊 SCAN COMPLETE:")
            Log.d(TAG, "   Flagged messages in cache: ${cachedMessages.size}")
            Log.d(TAG, "   Active badges on screen: $activeBadges")
            if (activeBadges < cachedMessages.size) {
                Log.w(TAG, "   ⚠️ WARNING: Not all messages have badges!")
                Log.w(TAG, "   Missing: ${cachedMessages.size - activeBadges} badges")
            } else if (activeBadges == cachedMessages.size) {
                Log.d(TAG, "   ✅ All messages have badges!")
            }
            Log.d(TAG, "========================================")
            
            // Recycle node to free resources
            rootNode.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }
    
    /**
     * Recursively traverse the accessibility node tree to find text nodes
     * @param node The current node to process
     */
    private fun traverseNodeTree(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        try {
            // Check if this node contains text
            val nodeText = node.text?.toString()
            
            if (!nodeText.isNullOrBlank()) {
                // Get all flagged messages from the cache
                val flaggedMessages = FactCheckCache.getFlaggedMessages()
                
                // Log every text node we find (for debugging)
                Log.v(TAG, "📝 Found text node: ${nodeText.take(50)}...")

                // First, check for phishing URLs (instant local detection)
                val urls = extractUrls(nodeText)
                if (urls.isNotEmpty()) {
                    checkForPhishingUrls(nodeText, urls)
                }
                
                // Then, check if this is a NEW message that needs AI analysis
                // Only analyze NEW messages (not in ProcessedMessagesCache)
                if (shouldAnalyzeMessage(nodeText)) {
                    Log.d(TAG, "🆕 NEW MESSAGE DETECTED IN UI: ${nodeText.take(50)}...")
                    // Analyze immediately (no batch processing)
                    analyzeNewMessage(nodeText)
                }

                // Then, check if any flagged message is contained in the node's text
                for (flaggedMessage in flaggedMessages) {
                    // Skip if user has dismissed this message
                    if (com.antigravity.aimonitor.util.DismissedMessagesCache.isDismissed(flaggedMessage)) {
                        continue
                    }
                    
                    // Normalize texts for comparison
                    val normalizedNodeText = normalizeText(nodeText)
                    val normalizedFlaggedMsg = normalizeText(flaggedMessage)
                    
                    // 1. Exact match (normalized)
                    if (normalizedNodeText == normalizedFlaggedMsg) {
                        Log.d(TAG, "✅ EXACT MATCH (Normalized): ${flaggedMessage.take(30)}...")
                        showBadgeForMessage(node, flaggedMessage)
                    }
                    // 2. Contains match (normalized)
                    else if (normalizedNodeText.contains(normalizedFlaggedMsg)) {
                        Log.d(TAG, "✅ PARTIAL MATCH (Normalized): ${flaggedMessage.take(30)}...")
                        showBadgeForMessage(node, flaggedMessage)
                    }
                    // 3. Reverse contains (normalized) - for when flagged message has extra info (sender)
                    else if (normalizedFlaggedMsg.contains(normalizedNodeText) && normalizedNodeText.length > 10) {
                        Log.d(TAG, "✅ REVERSE MATCH (Normalized): Node text found in flagged message")
                        showBadgeForMessage(node, flaggedMessage)
                    }
                    // 4. Fuzzy match (Levenshtein distance or high overlap)
                    // Simple overlap check: if 80% of words match
                    else if (calculateWordOverlap(normalizedNodeText, normalizedFlaggedMsg) > 0.8) {
                        Log.d(TAG, "✅ FUZZY MATCH: High word overlap")
                        showBadgeForMessage(node, flaggedMessage)
                    }
                }
            }
            
            // Recursively process child nodes
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                traverseNodeTree(childNode)
                childNode?.recycle()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error traversing node", e)
        }
    }
    
    /**
     * Check if a message should be analyzed
     * Filters out system messages, timestamps, and already processed messages
     */
    private fun shouldAnalyzeMessage(text: String): Boolean {
        // Skip if too short (likely not a real message)
        if (text.length < 3) return false
        
        // Skip if already processed (checked by API before)
        if (com.antigravity.aimonitor.util.ProcessedMessagesCache.isProcessed(text)) {
            return false
        }
        
        // Skip if already tracked (currently being processed)
        if (trackedMessages.contains(text)) return false
        
        // Skip common UI elements and timestamps
        val skipPatterns = listOf(
            "^\\d{1,2}:\\d{2}$", // Time like "12:34"
            "^\\d{1,2}:\\d{2} [AP]M$", // Time like "12:34 PM"
            "^[A-Z][a-z]+ \\d{1,2}$", // Date like "Jan 15"
            "^Typing\\.\\.\\.$", // Typing indicator
            "^Online$", // Online status
            "^Last seen", // Last seen status
            "^\\d+ unread", // Unread count
            "^Pinned message$", // Pinned message label
            "^Reply$", // Reply button
            "^Forward$", // Forward button
            "^Edit$", // Edit button
            "^Delete$" // Delete button
        )
        
        for (pattern in skipPatterns) {
            if (text.matches(Regex(pattern))) {
                return false
            }
        }
        
        // This looks like a real message that hasn't been processed yet
        return true
    }

    
    /**
     * Analyze a new message detected in the UI
     * OPTIMIZED: Checks ProcessedMessagesCache to avoid duplicate scans
     */
    private fun analyzeNewMessage(messageText: String) {
        // Check ProcessedMessagesCache first
        if (com.antigravity.aimonitor.util.ProcessedMessagesCache.isProcessed(messageText)) {
            Log.d(TAG, "⏭️ Message already processed, skipping analysis")
            trackedMessages.add(messageText)
            return
        }
        
        // Mark as tracked
        trackedMessages.add(messageText)
        
        // Launch coroutine to analyze
        serviceScope.launch {
            try {
                val links = extractUrls(messageText)
                
                Log.d(TAG, "")
                Log.d(TAG, "🔍 ANALYZING NEW MESSAGE")
                Log.d(TAG, "   Message: \"${messageText.take(50)}...\"")
                
                // Analyze with AI
                val response = GeminiFactChecker.analyzeMessage(messageText, links)
                
                // Mark as processed
                com.antigravity.aimonitor.util.ProcessedMessagesCache.markAsProcessed(messageText)
                
                if (response == null) {
                    Log.e(TAG, "❌ Failed to get AI response")
                    return@launch
                }
                
                Log.d(TAG, "✅ ${response.label} (${String.format("%.0f%%", response.confidence * 100)})")
                
                // Increment message count
                try {
                    com.antigravity.aimonitor.ui.MainActivity.incrementMessageCount(this@TelegramAccessibilityService)
                } catch (e: Exception) {
                    Log.e(TAG, "Error incrementing count", e)
                }
                
                // ⚠️ NEW LOGIC: ONLY show badges for HIGH severity
                val needsBadge = response.isMisinformation && response.severity.uppercase() == "HIGH"
                
                if (needsBadge) {
                    val flaggedMessage = FlaggedMessage(
                        messageText = messageText,
                        factCheckResponse = response
                    )
                    
                    FactCheckCache.addFlaggedMessage(flaggedMessage)
                    Log.d(TAG, "🚨 MESSAGE FLAGGED! Triggering immediate UI update...")
                    
                    // FORCE UI UPDATE: Re-scan screen to show badge immediately
                    forceRescan()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error analyzing message: ${e.message}", e)
                com.antigravity.aimonitor.util.ProcessedMessagesCache.markAsProcessed(messageText)
            }
        }
    }
    
    /**
     * Force a re-scan of the current window to update badges
     * Called when a new message is flagged asynchronously
     */
    private fun forceRescan() {
        try {
            val root = rootInActiveWindow
            if (root != null) {
                Log.d(TAG, "🔄 Forcing UI rescan to show new badges...")
                traverseNodeTree(root)
                root.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in forceRescan", e)
        }
    }
    
    /**
     * Extract URLs from message text
     */
    private fun extractUrls(text: String): List<String> {
        val urlRegex = Regex("https?://[^\\s]+")
        return urlRegex.findAll(text).map { it.value }.toList()
    }
    
    /**
     * Check for phishing URLs in message (instant local detection)
     * Note: ALL URLs (phishing or not) will also be sent to AI for content analysis
     * This function only provides instant phishing warnings
     */
    private fun checkForPhishingUrls(messageText: String, urls: List<String>) {
        Log.d(TAG, "🔗 Checking ${urls.size} URL(s) for phishing...")
        
        urls.forEach { url ->
            // First, check if the URL belongs to a known blacklisted domain
            val host = try {
                java.net.URI(url).host?.lowercase()?.removePrefix("www.") ?: ""
            } catch (e: Exception) {
                ""
            }
            if (host in BLACKLISTED_DOMAINS) {
                Log.w(TAG, "⚠️ Blacklisted domain detected: $host")
                val phishingResponse = com.antigravity.aimonitor.model.FactCheckResponse(
                    isMisinformation = true,
                    confidence = 1.0,
                    label = "PHISHING",
                    explanation = "Blacklisted domain detected: $host",
                    sources = listOf(url),
                    severity = "HIGH" // ⚠️ Critical phishing threat
                )
                val flaggedMessage = FlaggedMessage(
                    messageText = messageText,
                    factCheckResponse = phishingResponse
                )
                FactCheckCache.addFlaggedMessage(flaggedMessage)
                // Increment count and continue to next URL
                try {
                    com.antigravity.aimonitor.ui.MainActivity.incrementMessageCount(this@TelegramAccessibilityService)
                } catch (e: Exception) {
                    Log.e(TAG, "Error incrementing count", e)
                }
                // Skip further analysis for this URL
                return@forEach
            }
            val result = com.antigravity.aimonitor.util.PhishingDetector.analyzeUrl(url)
            
            if (result.isPhishing) {
                Log.w(TAG, "")
                Log.w(TAG, "========================================")
                Log.w(TAG, "🚨 PHISHING URL DETECTED (LOCAL)")
                Log.w(TAG, "🔗 URL: $url")
                Log.w(TAG, "⚠️ Risk Level: ${result.riskLevel}")
                Log.w(TAG, "📊 Confidence: ${String.format("%.0f%%", result.confidence * 100)}")
                Log.w(TAG, "📋 Reasons:")
                result.reasons.forEach { reason ->
                    Log.w(TAG, "   - $reason")
                }
                Log.w(TAG, "🤖 Sending to AI for additional verification...")
                Log.w(TAG, "========================================")
                Log.w(TAG, "")
                
                // Create immediate phishing response for instant badge
                val phishingResponse = com.antigravity.aimonitor.model.FactCheckResponse(
                    isMisinformation = true,
                    confidence = result.confidence,
                    label = "PHISHING",
                    explanation = "Suspicious URL detected: ${result.reasons.joinToString("; ")}",
                    sources = listOf(url),
                    severity = "HIGH" // ⚠️ Critical phishing threat
                )
                
                // Add to cache for immediate badge display
                val flaggedMessage = FlaggedMessage(
                    messageText = messageText,
                    factCheckResponse = phishingResponse
                )
                
                FactCheckCache.addFlaggedMessage(flaggedMessage)
                
                // Increment count
                try {
                    com.antigravity.aimonitor.ui.MainActivity.incrementMessageCount(this@TelegramAccessibilityService)
                } catch (e: Exception) {
                    Log.e(TAG, "Error incrementing count", e)
                }
                
                // ALSO send to AI for enhanced analysis
                analyzePhishingWithAI(messageText, url, result)
                
            } else if (result.riskLevel != com.antigravity.aimonitor.util.RiskLevel.SAFE) {
                Log.d(TAG, "⚠️ URL has ${result.riskLevel} risk: $url")
                Log.d(TAG, "   Reasons: ${result.reasons.joinToString("; ")}")
            }
        }
    }
    
    /**
     * Send phishing URL to AI for enhanced analysis
     * This provides additional context and verification
     */
    private fun analyzePhishingWithAI(messageText: String, url: String, localResult: com.antigravity.aimonitor.util.PhishingResult) {
        serviceScope.launch {
            try {
                Log.d(TAG, "")
                Log.d(TAG, "========================================")
                Log.d(TAG, "🤖 AI VERIFICATION OF PHISHING URL")
                Log.d(TAG, "🔗 URL: $url")
                Log.d(TAG, "📱 Message: \"${messageText.take(50)}...\"")
                Log.d(TAG, "========================================")
                
                // Create enhanced prompt for AI
                val enhancedPrompt = """
                    Analyze this message for phishing/scam attempt.
                    
                    Message: "$messageText"
                    
                    Suspicious URL detected: $url
                    Local analysis found: ${localResult.reasons.joinToString("; ")}
                    
                    Provide additional context about why this is dangerous and what the attacker might be trying to do.
                """.trimIndent()
                
                // Call AI
                val aiResponse = GeminiFactChecker.analyzeMessage(
                    messageText = enhancedPrompt,
                    links = listOf(url)
                )
                
                if (aiResponse != null) {
                    Log.d(TAG, "")
                    Log.d(TAG, "✅ AI VERIFICATION COMPLETE:")
                    Log.d(TAG, "   📊 AI Confirms: ${aiResponse.isMisinformation}")
                    Log.d(TAG, "   📈 AI Confidence: ${String.format("%.0f%%", aiResponse.confidence * 100)}")
                    Log.d(TAG, "   🏷️ AI Label: ${aiResponse.label}")
                    Log.d(TAG, "   💬 AI Explanation: ${aiResponse.explanation}")
                    
                    // Update cache with enhanced AI analysis
                    val enhancedResponse = com.antigravity.aimonitor.model.FactCheckResponse(
                        isMisinformation = true,
                        confidence = maxOf(localResult.confidence, aiResponse.confidence),
                        label = "PHISHING",
                        explanation = "Local: ${localResult.reasons.joinToString("; ")}. AI: ${aiResponse.explanation}",
                        sources = listOf(url),
                        severity = "HIGH" // ⚠️ Critical phishing threat
                    )
                    
                    val enhancedMessage = FlaggedMessage(
                        messageText = messageText,
                        factCheckResponse = enhancedResponse
                    )
                    
                    FactCheckCache.addFlaggedMessage(enhancedMessage)
                    Log.d(TAG, "✅ Cache updated with AI-enhanced analysis")
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "")
                } else {
                    Log.w(TAG, "⚠️ AI verification failed, using local analysis only")
                }
                
                // Mark as processed
                com.antigravity.aimonitor.util.ProcessedMessagesCache.markAsProcessed(messageText)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in AI verification: ${e.message}", e)
                // Local detection already flagged it, so this is just enhancement
            }
        }
    }
    
    /**
     * Show badge for a matched message
     */
    private fun showBadgeForMessage(node: AccessibilityNodeInfo, flaggedMessage: String) {
        val rect = Rect()
        try {
            node.getBoundsInScreen(rect)
            
            // Validate bounds are reasonable
            if (rect.left < 0 || rect.top < 0 || rect.width() <= 0 || rect.height() <= 0) {
                Log.w(TAG, "⚠️ Invalid bounds: $rect")
                return
            }
            
            Log.d(TAG, "📍 Badge bounds: left=${rect.left}, top=${rect.top}, width=${rect.width()}, height=${rect.height()}")
            
            // Check if position has changed significantly (message moved)
            val positionChanged = OverlayManager.updateBadgePosition(this, rect, flaggedMessage)
            
            if (positionChanged) {
                Log.d(TAG, "✅ Badge position updated for: ${flaggedMessage.take(30)}... at (${rect.left}, ${rect.top})")
            }
            
            // Track this message
            trackedMessages.add(flaggedMessage)

        } catch (e: IllegalStateException) {
            Log.e(TAG, "❌ Error getting bounds for node", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing badge", e)
        }
    }
    
    /**
     * Handle window state changes to detect when chat is opened/closed
     */
    private fun handleWindowStateChange(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""
        val packageName = event.packageName?.toString() ?: ""
        
        Log.d(TAG, "Window state changed: $className")
        Log.d(TAG, "Package: $packageName")
        
        // Check if we're still in a supported messaging app
        if (packageName !in SUPPORTED_PACKAGES) {
            // Left the messaging app completely
            Log.d(TAG, "📴 Left messaging app - Removing all badges")
            OverlayManager.removeAllBadges()
            trackedMessages.clear()
            currentChatId = null
            // Clear dismissed messages cache for fresh session
            DismissedMessagesCache.clear()
            return
        }
        
        // Check if we're in a chat window
        val isInChat = className.contains("ChatActivity") || 
                       className.contains("ConversationActivity") ||
                       className.contains("chat", ignoreCase = true)
        
        if (!isInChat) {
            // Not in a chat, remove all badges
            Log.d(TAG, "📴 Left chat - Removing all badges")
            OverlayManager.removeAllBadges()
            trackedMessages.clear()
            currentChatId = null
            // Clear dismissed messages for this chat session
            DismissedMessagesCache.clear()
        } else {
            // Detect if we switched to a different chat
            val newChatId = className // Use className as chat identifier
            if (newChatId != currentChatId) {
                Log.d(TAG, "")
                Log.d(TAG, "========================================")
                Log.d(TAG, "💬 ENTERED NEW CHAT")
                Log.d(TAG, "📝 Only NEW messages will be checked (no batch scan)")
                Log.d(TAG, "========================================")
                Log.d(TAG, "")
                
                currentChatId = newChatId
                trackedMessages.clear()
                OverlayManager.removeAllBadges()
            } else {
                Log.d(TAG, "💬 Same chat - Only new messages will be checked")
            }
        }
    }
    
    /**
     * Normalize text by removing timestamps, extra spaces, and punctuation
     */
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("\\d{1,2}:\\d{2}\\s?[ap]m?"), "") // Remove timestamps
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove punctuation
            .replace(Regex("\\s+"), " ") // Collapse spaces
            .trim()
    }

    /**
     * Calculate percentage of words from the shorter string that appear in the longer string
     */
    private fun calculateWordOverlap(text1: String, text2: String): Double {
        val words1 = text1.split(" ").filter { it.length > 2 }.toSet()
        val words2 = text2.split(" ").filter { it.length > 2 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val (smaller, larger) = if (words1.size < words2.size) words1 to words2 else words2 to words1
        val matchCount = smaller.count { larger.contains(it) }
        
        return matchCount.toDouble() / smaller.size
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "🔌 Accessibility service CONNECTED and READY")
        Log.d(TAG, "📱 Monitoring packages: ${SUPPORTED_PACKAGES.joinToString(", ")}")
        Log.d(TAG, "🎯 REAL-TIME MESSAGE DETECTION: Enabled")
        Log.d(TAG, "   - Detects messages when Telegram is OPEN")
        Log.d(TAG, "   - Analyzes new messages immediately")
        Log.d(TAG, "   - Shows badges automatically")
        Log.d(TAG, "   - Badges appear only in chat windows")
        Log.d(TAG, "   - Badges disappear when leaving chat")
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "⚠️ Service interrupted - Removing all badges")
        // Remove all badges when service is interrupted
        OverlayManager.removeAllBadges()
        trackedMessages.clear()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 Service destroyed - Cleaning up")
        // Cancel all coroutines
        serviceScope.coroutineContext[Job]?.cancel()
        // Clean up all overlays
        OverlayManager.removeAllBadges()
        trackedMessages.clear()
    }
}
