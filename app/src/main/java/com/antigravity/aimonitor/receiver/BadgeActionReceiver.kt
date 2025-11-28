package com.antigravity.aimonitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antigravity.aimonitor.util.MonitoringNotification
import com.antigravity.aimonitor.util.OverlayManager

/**
 * Handles actions from the monitoring notification
 */
class BadgeActionReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BadgeActionReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MonitoringNotification.ACTION_CLOSE_ALL_BADGES -> {
                Log.d(TAG, "========================================")
                Log.d(TAG, "🗑️ CLOSE ALL BADGES ACTION")
                Log.d(TAG, "User requested to close all current badges")
                Log.d(TAG, "========================================")
                
                // Remove all current badges
                val removedCount = OverlayManager.removeAllBadges()
                
                Log.d(TAG, "✅ Removed $removedCount badge(s)")
                Log.d(TAG, "🔄 Badges may reappear if you scroll (you can close again)")
                Log.d(TAG, "🆕 New misinformation will ALWAYS show badges")
                Log.d(TAG, "========================================")
                
                // Update notification to reflect action
                MonitoringNotification.updateNotification(context, 0)
            }
        }
    }
}
