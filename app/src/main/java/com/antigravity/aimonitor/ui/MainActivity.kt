package com.antigravity.aimonitor.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.service.TelegramAccessibilityService

/**
 * Main activity for permission setup and app status display
 * Guides users through enabling required permissions
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "TelegramMisinfoPrefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_MESSAGES_CHECKED = "messages_checked"
        
        /**
         * Get monitoring enabled state (called from services)
         */
        fun isMonitoringEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        }
        
        /**
         * Increment message count (called from services)
         */
        fun incrementMessageCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(KEY_MESSAGES_CHECKED, 0)
            val newCount = currentCount + 1
            prefs.edit().putInt(KEY_MESSAGES_CHECKED, newCount).apply()
            
            // Update notification if monitoring is enabled
            if (isMonitoringEnabled(context)) {
                com.antigravity.aimonitor.util.MonitoringNotification.updateNotification(context, newCount)
            }
        }
    }
    
    private lateinit var tvNotificationStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvMonitoringStatus: TextView
    private lateinit var tvMessagesChecked: TextView
    
    private lateinit var btnNotification: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnStartMonitoring: Button
    private lateinit var monitoringStatusCard: LinearLayout
    
    private lateinit var prefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
                Log.d(TAG, "📢 Requesting notification permission")
            }
        }
        
        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Initialize views
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)
        tvMonitoringStatus = findViewById(R.id.tvMonitoringStatus)
        tvMessagesChecked = findViewById(R.id.tvMessagesChecked)
        
        btnNotification = findViewById(R.id.btnNotification)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        btnOverlay = findViewById(R.id.btnOverlay)
        btnStartMonitoring = findViewById(R.id.btnStartMonitoring)
        monitoringStatusCard = findViewById(R.id.monitoringStatusCard)
        
        // Set up button click listeners
        btnNotification.setOnClickListener {
            openNotificationSettings()
        }
        
        btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        btnOverlay.setOnClickListener {
            openOverlaySettings()
        }
        
        btnStartMonitoring.setOnClickListener {
            toggleMonitoring()
        }
        
        // Add debug button
        findViewById<Button>(R.id.btnDebug)?.setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
        }
        
        // Add steganography detector button
        findViewById<Button>(R.id.btnSteganography)?.setOnClickListener {
            startActivity(Intent(this, SteganographyActivity::class.java))
        }
        
        // Check if we should show bottom sheet
        handleBottomSheetIntent()
    }
    
    override fun onResume() {
        super.onResume()
        // Update permission status when activity resumes
        updatePermissionStatus()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleBottomSheetIntent()
    }
    
    /**
     * Handle intent to show bottom sheet for a specific message
     */
    private fun handleBottomSheetIntent() {
        val messageText = intent?.getStringExtra(FactCheckBottomSheet.EXTRA_MESSAGE_TEXT)
        if (!messageText.isNullOrBlank()) {
            val bottomSheet = FactCheckBottomSheet.newInstance(messageText)
            bottomSheet.show(supportFragmentManager, "FactCheckBottomSheet")
            // Clear the intent extra
            intent?.removeExtra(FactCheckBottomSheet.EXTRA_MESSAGE_TEXT)
        }
    }
    
    /**
     * Update UI to reflect current permission status
     */
    private fun updatePermissionStatus() {
        val notificationEnabled = checkNotificationListenerPermission()
        val accessibilityEnabled = checkAccessibilityPermission()
        val overlayEnabled = checkOverlayPermission()
        
        // Update notification status
        tvNotificationStatus.text = if (notificationEnabled) {
            getString(R.string.enabled)
        } else {
            getString(R.string.disabled)
        }
        tvNotificationStatus.setTextColor(
            if (notificationEnabled) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )
        
        // Update accessibility status
        tvAccessibilityStatus.text = if (accessibilityEnabled) {
            getString(R.string.enabled)
        } else {
            getString(R.string.disabled)
        }
        tvAccessibilityStatus.setTextColor(
            if (accessibilityEnabled) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )
        
        // Update overlay status
        tvOverlayStatus.text = if (overlayEnabled) {
            getString(R.string.enabled)
        } else {
            getString(R.string.disabled)
        }
        tvOverlayStatus.setTextColor(
            if (overlayEnabled) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )
        
        // Check if all permissions are enabled
        val allPermissionsEnabled = notificationEnabled && accessibilityEnabled && overlayEnabled
        
        // Get monitoring state
        val isMonitoring = prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        
        // Update overall status message
        if (allPermissionsEnabled) {
            tvStatusMessage.text = "✅ All permissions enabled - Ready to monitor!"
            tvStatusMessage.setTextColor(0xFF4CAF50.toInt())
            
            // Show start/stop button
            btnStartMonitoring.visibility = View.VISIBLE
            btnStartMonitoring.text = if (isMonitoring) "Stop Monitoring" else "Start Monitoring"
            btnStartMonitoring.setBackgroundColor(
                if (isMonitoring) 0xFFF44336.toInt() else 0xFF4CAF50.toInt()
            )
            
            // Show monitoring status card if monitoring is active
            if (isMonitoring) {
                monitoringStatusCard.visibility = View.VISIBLE
                
                // Update message count
                val messagesChecked = prefs.getInt(KEY_MESSAGES_CHECKED, 0)
                tvMonitoringStatus.text = "🔴 Monitoring Active"
                tvMonitoringStatus.setTextColor(0xFF4CAF50.toInt())
                tvMessagesChecked.text = "Messages analyzed: $messagesChecked"
            } else {
                monitoringStatusCard.visibility = View.GONE
            }
        } else {
            tvStatusMessage.text = "⚠️ Enable all permissions to start monitoring"
            tvStatusMessage.setTextColor(0xFFFF9800.toInt())
            
            // Hide monitoring controls
            monitoringStatusCard.visibility = View.GONE
            btnStartMonitoring.visibility = View.GONE
        }
    }
    
    /**
     * Toggle monitoring on/off
     */
    private fun toggleMonitoring() {
        val isCurrentlyMonitoring = prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        val newState = !isCurrentlyMonitoring
        
        // Save new state
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, newState).apply()
        
        // Show/hide monitoring notification
        if (newState) {
            val messagesChecked = prefs.getInt(KEY_MESSAGES_CHECKED, 0)
            com.antigravity.aimonitor.util.MonitoringNotification.showMonitoringNotification(this, messagesChecked)
            Log.d(TAG, "📢 Monitoring notification shown")
        } else {
            com.antigravity.aimonitor.util.MonitoringNotification.hideMonitoringNotification(this)
            Log.d(TAG, "🔕 Monitoring notification hidden")
        }
        
        // Update UI
        updatePermissionStatus()
        
        // Show feedback
        val message = if (newState) {
            "✅ Monitoring started! Badges will appear in Telegram chats."
        } else {
            "⏸️ Monitoring stopped. Badges will not appear."
        }
        
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        
        Log.d(TAG, "Monitoring ${if (newState) "started" else "stopped"}")
    }
    

    
    /**
     * Check if notification listener permission is granted
     */
    private fun checkNotificationListenerPermission(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledPackages.contains(packageName)
    }
    
    /**
     * Check if accessibility service is enabled
     * Uses AccessibilityManager API for reliable detection
     */
    private fun checkAccessibilityPermission(): Boolean {
        try {
            val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            
            val expectedComponentName = ComponentName(
                this,
                TelegramAccessibilityService::class.java
            )
            
            Log.d(TAG, "Expected component: ${expectedComponentName.flattenToString()}")
            
            val isEnabled = enabledServices.any { service ->
                val serviceInfo = service.resolveInfo.serviceInfo
                val componentName = ComponentName(serviceInfo.packageName, serviceInfo.name)
                Log.d(TAG, "Found service: ${componentName.flattenToString()}")
                componentName == expectedComponentName
            }
            
            Log.d(TAG, "Accessibility service enabled: $isEnabled")
            return isEnabled
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility permission", e)
            return false
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // Permission not required on older versions
        }
    }
    
    /**
     * Open notification listener settings
     */
    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    /**
     * Open accessibility settings
     */
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    /**
     * Open overlay permission settings
     */
    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
