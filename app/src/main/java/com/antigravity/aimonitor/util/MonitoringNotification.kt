package com.antigravity.aimonitor.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.ui.MainActivity

/**
 * Manages the persistent monitoring notification
 */
object MonitoringNotification {
    
    private const val CHANNEL_ID = "monitoring_channel"
    private const val NOTIFICATION_ID = 1001
    const val ACTION_CLOSE_ALL_BADGES = "com.antigravity.aimonitor.CLOSE_ALL_BADGES"
    
    /**
     * Create notification channel (required for Android O+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Misinformation Monitoring"
            val descriptionText = "Shows when misinformation detection is active"
            val importance = NotificationManager.IMPORTANCE_LOW // Low priority, no sound
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show monitoring notification
     */
    fun showMonitoringNotification(context: Context, flaggedCount: Int = 0) {
        try {
            android.util.Log.d("MonitoringNotification", "========================================")
            android.util.Log.d("MonitoringNotification", "📢 SHOWING MONITORING NOTIFICATION")
            android.util.Log.d("MonitoringNotification", "Flagged count: $flaggedCount")
            
            createNotificationChannel(context)
            
            // Intent to open app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // Intent to close all badges
            val closeAllIntent = Intent(ACTION_CLOSE_ALL_BADGES).apply {
                setPackage(context.packageName)
            }
            val closeAllPendingIntent = PendingIntent.getBroadcast(
                context, 1, closeAllIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // Build notification
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // System icon
                .setContentTitle("🔍 Misinformation Detection Active")
                .setContentText("All messages are being checked for misinformation")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(buildNotificationText(flaggedCount)))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Can't be dismissed by swipe
                .setContentIntent(openAppPendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Close All Badges",
                    closeAllPendingIntent
                )
                .build()
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            android.util.Log.d("MonitoringNotification", "✅ Notification posted successfully")
            android.util.Log.d("MonitoringNotification", "========================================")
            
        } catch (e: Exception) {
            android.util.Log.e("MonitoringNotification", "❌ Error showing notification: ${e.message}", e)
        }
    }
    
    /**
     * Update notification with current stats
     */
    fun updateNotification(context: Context, flaggedCount: Int) {
        showMonitoringNotification(context, flaggedCount)
    }
    
    /**
     * Hide monitoring notification
     */
    fun hideMonitoringNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    /**
     * Build notification text with stats
     */
    private fun buildNotificationText(flaggedCount: Int): String {
        return when {
            flaggedCount == 0 -> "All messages are being checked for misinformation.\nNo suspicious content detected yet."
            flaggedCount == 1 -> "All messages are being checked.\n1 suspicious message detected."
            else -> "All messages are being checked.\n$flaggedCount suspicious messages detected."
        }
    }
}
