package com.antigravity.aimonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.ui.MainActivity

/**
 * Foreground service that monitors for new images and detects steganography
 */
class SteganographyMonitorService : Service() {
    
    private var mediaObserver: TelegramMediaContentObserver? = null
    private var workerThread: HandlerThread? = null
    
    companion object {
        private const val TAG = "SteganographyMonitor"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "steganography_monitor_channel"
        var isRunning = false
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Create notification channel
        createNotificationChannel()
        
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start monitoring
        startMonitoring()
        isRunning = true
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Steganography Monitor"
            val descriptionText = "Monitors for steganography in Telegram images"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Steganography Detector")
            .setContentText("Monitoring Telegram images...")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun startMonitoring() {
        // Create worker thread for content observer
        workerThread = HandlerThread("SteganographyWorker").apply {
            start()
        }
        
        val handler = Handler(workerThread!!.looper)
        mediaObserver = TelegramMediaContentObserver(handler, this)
        
        // Register content observer
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver!!
        )
        
        Log.d(TAG, "Monitoring started")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister observer
        mediaObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        
        // Quit worker thread
        workerThread?.quitSafely()
        
        isRunning = false
        Log.d(TAG, "Service destroyed")
    }
}
