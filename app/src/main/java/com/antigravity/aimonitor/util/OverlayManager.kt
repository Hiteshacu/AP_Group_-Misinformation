package com.antigravity.aimonitor.util

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.ui.FactCheckBottomSheet

/**
 * Manages floating overlay badges for flagged messages
 * Uses WindowManager to display badges at specific screen coordinates
 */
object OverlayManager {
    
    private const val TAG = "OverlayManager"
    
    // Track active badges to prevent duplicates
    private val activeBadges = mutableMapOf<String, BadgeInfo>()
    
    // Track active alert popup
    private var activeAlertView: View? = null
    private var activeAlertWindowManager: WindowManager? = null
    
    private data class BadgeInfo(
        val view: View,
        val windowManager: WindowManager,
        val params: WindowManager.LayoutParams,
        var lastClickTime: Long = 0,
        var lastX: Int = 0,
        var lastY: Int = 0
    )
    
    /**
     * Display a warning badge at the specified screen location
     * @param context Application context
     * @param bounds Screen bounds of the message
     * @param messageText The message text (used as key for tracking)
     */
    fun showBadge(context: Context, bounds: Rect, messageText: String) {
        Log.d(TAG, "=== OVERLAY MANAGER DEBUG ===")
        Log.d(TAG, "🔥🔥🔥 NEW BUILD v2.0 - 2025-11-20 20:02 🔥🔥🔥")
        Log.d(TAG, "showBadge called for: ${messageText.take(50)}...")
        
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.e(TAG, "❌ Overlay permission not granted!")
                return
            } else {
                Log.d(TAG, "✅ Overlay permission granted")
            }
        }
        
        // Remove existing badge for this message if present
        removeBadge(messageText)
        
        try {
            // Get WindowManager
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            Log.d(TAG, "WindowManager obtained")
            
            // Inflate badge layout
            val inflater = LayoutInflater.from(context)
            val badgeView = inflater.inflate(R.layout.badge_layout, null)
            
            // Get flagged message to check severity
            val flaggedMessage = FactCheckCache.getFlaggedMessage(messageText)
            val severity = flaggedMessage?.factCheckResponse?.severity ?: "NONE"
            
            // Update badge appearance - ONLY RED badges (all flagged messages are HIGH severity)
            val badgeBackground = badgeView.findViewById<View>(R.id.badge_background)
            val badgeIcon = badgeView.findViewById<android.widget.ImageView>(R.id.badge_icon)
            
            Log.d(TAG, "Badge styling - Severity: $severity (ONLY HIGH severity flagged)")
            
            // ⚠️ SIMPLIFIED: All badges are RED (only HIGH severity messages are flagged)
            val badgeColor = 0xFFFF1744.toInt() // RED
            val iconResource = R.drawable.ic_danger
            
            Log.d(TAG, "🔴 Applying RED badge (High Severity)")
            
            // Apply icon and color
            badgeIcon.setImageResource(iconResource)
            badgeIcon.setColorFilter(0xFFFFFFFF.toInt()) // Icon is always WHITE
            
            // Apply background color
            val backgroundDrawable = badgeBackground.background as? android.graphics.drawable.GradientDrawable
            if (backgroundDrawable != null) {
                backgroundDrawable.setColor(badgeColor)
            } else {
                // Fallback if background is not a GradientDrawable (shouldn't happen with shape)
                badgeBackground.setBackgroundColor(badgeColor)
            }
            
            Log.d(TAG, "Badge applied: RED, Color=${String.format("#%06X", 0xFFFFFF and badgeColor)}")
            Log.d(TAG, "Badge view inflated")
            
            // Configure layout parameters
            val params = WindowManager.LayoutParams(
                dpToPx(context, 50), // width: 50dp
                dpToPx(context, 50), // height: 50dp
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            
            // Position badge at message location
            params.gravity = Gravity.TOP or Gravity.START
            params.x = bounds.left
            params.y = bounds.top
            
            Log.d(TAG, "Badge position: x=${params.x}, y=${params.y}")
            
            // Add view to window first
            windowManager.addView(badgeView, params)
            
            // Track this badge with its position
            val badgeInfo = BadgeInfo(badgeView, windowManager, params, lastX = bounds.left, lastY = bounds.top)
            activeBadges[messageText] = badgeInfo
            
            // Set click listener with double-click detection
            badgeView.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - badgeInfo.lastClickTime
                
                if (timeDiff < 300) { // Double-click within 300ms
                    Log.d(TAG, "Double-click detected! Dismissing badge for: ${messageText.take(50)}...")
                    removeBadge(messageText)
                } else {
                    Log.d(TAG, "Single click - Opening bottom sheet for: ${messageText.take(50)}...")
                    openFactCheckBottomSheet(context, messageText)
                }
                
                badgeInfo.lastClickTime = currentTime
            }
            
            Log.d(TAG, "✅ Badge displayed successfully at (${bounds.left}, ${bounds.top})")
            Log.d(TAG, "Total active badges: ${activeBadges.size}")
            
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "❌ Error adding overlay view - bad token", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error displaying badge: ${e.message}", e)
        }
    }
    
    /**
     * Update badge position if message has moved
     * @param context Application context
     * @param bounds New screen bounds of the message
     * @param messageText The message text key
     * @return True if position was updated, false if no change
     */
    fun updateBadgePosition(context: Context, bounds: Rect, messageText: String): Boolean {
        val badgeInfo = activeBadges[messageText]
        
        if (badgeInfo == null) {
            // Badge doesn't exist yet, create it
            showBadge(context, bounds, messageText)
            return true
        }
        
        // Check if position has changed significantly (more than 10 pixels)
        val deltaX = kotlin.math.abs(bounds.left - badgeInfo.lastX)
        val deltaY = kotlin.math.abs(bounds.top - badgeInfo.lastY)
        
        if (deltaX > 10 || deltaY > 10) {
            // Position changed, update badge
            try {
                badgeInfo.params.x = bounds.left
                badgeInfo.params.y = bounds.top
                badgeInfo.windowManager.updateViewLayout(badgeInfo.view, badgeInfo.params)
                
                // Update tracked position
                badgeInfo.lastX = bounds.left
                badgeInfo.lastY = bounds.top
                
                Log.d(TAG, "Badge position updated: (${bounds.left}, ${bounds.top})")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating badge position", e)
            }
        }
        
        return false
    }
    
    /**
     * Remove a specific badge from the screen
     * @param messageText The message text key
     */
    fun removeBadge(messageText: String) {
        activeBadges[messageText]?.let { badgeInfo ->
            try {
                badgeInfo.windowManager.removeView(badgeInfo.view)
                activeBadges.remove(messageText)
                Log.d(TAG, "Badge removed for message: ${messageText.take(50)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing badge", e)
            }
        }
    }
    
    /**
     * Remove all active badges from the screen
     */
    fun removeAllBadges() {
        val messagesToRemove = activeBadges.keys.toList()
        messagesToRemove.forEach { removeBadge(it) }
        Log.d(TAG, "All badges removed")
    }
    
    /**
     * Get the number of active badges
     */
    fun getActiveBadgeCount(): Int {
        return activeBadges.size
    }
    
    /**
     * Show alert popup overlay with fact-check details
     */
    private fun openFactCheckBottomSheet(context: Context, messageText: String) {
        try {
            showAlertPopup(context, messageText)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alert popup", e)
        }
    }
    
    /**
     * Display alert popup positioned at message location
     */
    private fun showAlertPopup(context: Context, messageText: String) {
        Log.d(TAG, "=== SHOWING ALERT POPUP ===")
        
        // Remove any existing alert first
        dismissAlert()
        
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.e(TAG, "❌ Overlay permission not granted for alert popup")
                return
            }
        }
        
        // Get flagged message details
        val flaggedMessage = FactCheckCache.getFlaggedMessage(messageText)
        if (flaggedMessage == null) {
            Log.e(TAG, "❌ Message not found in cache")
            return
        }
        
        // Get badge position for this message
        val badgeInfo = activeBadges[messageText]
        if (badgeInfo == null) {
            Log.e(TAG, "❌ Badge not found for positioning")
            return
        }
        
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = LayoutInflater.from(context)
            val alertView = inflater.inflate(R.layout.alert_popup_overlay, null)
            
            // Get screen dimensions
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            // Calculate alert dimensions (80% of screen width, wrap content height)
            val alertWidth = (screenWidth * 0.85).toInt()
            
            // Configure window parameters for positioned overlay
            val params = WindowManager.LayoutParams(
                alertWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            // Position alert near the message
            // Get badge view location
            val location = IntArray(2)
            badgeInfo.view.getLocationOnScreen(location)
            val badgeX = location[0]
            val badgeY = location[1]
            
            // Position alert to the right of badge, or centered if not enough space
            params.gravity = Gravity.TOP or Gravity.START
            params.x = maxOf(dpToPx(context, 8), minOf(badgeX + dpToPx(context, 60), screenWidth - alertWidth - dpToPx(context, 8)))
            params.y = maxOf(dpToPx(context, 8), badgeY - dpToPx(context, 20))
            
            Log.d(TAG, "Alert position: x=${params.x}, y=${params.y}, width=$alertWidth")
            
            // Populate alert with data
            val response = flaggedMessage.factCheckResponse
            
            alertView.findViewById<android.widget.TextView>(R.id.tvAlertMessage).text = messageText
            
            // Customize alert - ONLY RED for HIGH severity (all flagged messages are HIGH)
            val alertTitle = alertView.findViewById<android.widget.TextView>(R.id.tvAlertTitle)
            val alertContainer = alertView.findViewById<android.widget.LinearLayout>(R.id.alert_container)
            
            // ⚠️ SIMPLIFIED: All alerts are RED (only HIGH severity messages are flagged)
            alertTitle.text = "🚨 IMPORTANT ALERT"
            val backgroundColor = 0xDDFF1744.toInt() // Semi-transparent RED
            
            // Apply the background color with rounded corners
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(backgroundColor)
            drawable.cornerRadius = dpToPx(context, 16).toFloat() // 16dp rounded corners
            alertContainer.background = drawable
            
            Log.d(TAG, "Alert background color: RED (High Severity)")
            
            val severityText = "${response.severity} SEVERITY"
            alertView.findViewById<android.widget.TextView>(R.id.tvAlertLabel).text = "${response.label} • $severityText"
            alertView.findViewById<android.widget.TextView>(R.id.tvAlertConfidence).text = 
                String.format("%.0f%%", response.confidence * 100)
            alertView.findViewById<android.widget.TextView>(R.id.tvAlertExplanation).text = response.explanation
            
            // Format sources
            val sourcesText = if (response.sources.isNotEmpty()) {
                response.sources.joinToString("\n") { "• $it" }
            } else {
                "No sources available"
            }
            alertView.findViewById<android.widget.TextView>(R.id.tvAlertSources).text = sourcesText
            
            // Set label text color - WHITE for better contrast on colored backgrounds
            alertView.findViewById<android.widget.TextView>(R.id.tvAlertLabel).setTextColor(0xFFFFFFFF.toInt())
            
            // Add view to window
            windowManager.addView(alertView, params)
            
            // Track the alert
            activeAlertView = alertView
            activeAlertWindowManager = windowManager
            
            // Fade in animation
            alertView.alpha = 0f
            alertView.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            
            // Style buttons to match the alert color scheme
            val btnDismiss = alertView.findViewById<android.widget.Button>(R.id.btnAlertDismiss)
            val btnGotIt = alertView.findViewById<android.widget.Button>(R.id.btnAlertGotIt)
            
            // Make buttons semi-transparent with white text
            val buttonDrawableDismiss = android.graphics.drawable.GradientDrawable()
            buttonDrawableDismiss.setColor(0x33FFFFFF.toInt()) // 20% white
            buttonDrawableDismiss.cornerRadius = dpToPx(context, 8).toFloat()
            btnDismiss.background = buttonDrawableDismiss
            btnDismiss.setTextColor(0xFFFFFFFF.toInt())
            
            val buttonDrawableGotIt = android.graphics.drawable.GradientDrawable()
            buttonDrawableGotIt.setColor(0xFFFFFFFF.toInt()) // Solid white
            buttonDrawableGotIt.cornerRadius = dpToPx(context, 8).toFloat()
            btnGotIt.background = buttonDrawableGotIt
            btnGotIt.setTextColor(0xFF000000.toInt()) // Black text on white
            
            // Dismiss button - closes alert, removes badge, and marks as dismissed
            btnDismiss.setOnClickListener {
                Log.d(TAG, "Dismiss button clicked - removing alert and badge permanently")
                dismissAlert()
                removeBadge(messageText)
                // Mark message as dismissed so it won't show badge again
                DismissedMessagesCache.dismissMessage(messageText)
            }
            
            // Got It button - dismisses alert and removes badge TEMPORARILY
            // Badge WILL reappear if same/new misinformation arrives again
            btnGotIt.setOnClickListener {
                Log.d(TAG, "Got It button clicked - removing alert and badge (NOT permanently dismissed)")
                dismissAlert()
                removeBadge(messageText)
                // NOTE: NOT calling DismissedMessagesCache.dismissMessage()
                // This allows the badge to reappear if new misinformation arrives
            }
            
            // Detail button - launches detailed sources activity
            val btnDetail = alertView.findViewById<android.widget.Button>(R.id.btnAlertDetail)
            btnDetail.setOnClickListener {
                Log.d(TAG, "========================================")
                Log.d(TAG, "🔍 DETAIL BUTTON CLICKED")
                Log.d(TAG, "   Message: \"${messageText.take(50)}...\"")
                Log.d(TAG, "   Launching DetailSourcesActivity...")
                
                try {
                    val intent = android.content.Intent(context, com.antigravity.aimonitor.ui.DetailSourcesActivity::class.java)
                    intent.putExtra("message_text", messageText)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d(TAG, "✅ Intent sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error launching DetailSourcesActivity", e)
                }
            }
            
            Log.d(TAG, "✅ Alert popup displayed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error displaying alert popup: ${e.message}", e)
        }
    }
    
    /**
     * Dismiss the active alert popup with fade-out animation
     */
    private fun dismissAlert() {
        activeAlertView?.let { view ->
            activeAlertWindowManager?.let { wm ->
                try {
                    Log.d(TAG, "Dismissing alert popup with animation")
                    
                    // Fade out animation
                    view.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            try {
                                wm.removeView(view)
                                activeAlertView = null
                                activeAlertWindowManager = null
                                Log.d(TAG, "✅ Alert popup dismissed successfully")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing alert view", e)
                            }
                        }
                        .start()
                } catch (e: Exception) {
                    Log.e(TAG, "Error animating alert dismissal", e)
                    // Fallback: remove without animation
                    try {
                        wm.removeView(view)
                        activeAlertView = null
                        activeAlertWindowManager = null
                    } catch (e2: Exception) {
                        Log.e(TAG, "Error removing alert view (fallback)", e2)
                    }
                }
            }
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
