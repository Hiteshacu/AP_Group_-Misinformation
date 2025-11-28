package com.antigravity.aimonitor.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.antigravity.aimonitor.R
import java.io.File

/**
 * Alert service that shows overlay alert for steganography detection
 * Displays analysis scores and allows user to block or close the alert
 */
class SteganographyAlertService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var alertView: View? = null
    
    companion object {
        private const val TAG = "SteganographyAlert"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Inflate the custom alert layout
        alertView = LayoutInflater.from(this).inflate(R.layout.steganography_alert_dialog, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        // Get views
        val blockButton = alertView?.findViewById<Button>(R.id.blockButton)
        val closeButton = alertView?.findViewById<Button>(R.id.closeButton)
        val alertMessage = alertView?.findViewById<TextView>(R.id.alertMessage)
        val metadataScoreView = alertView?.findViewById<TextView>(R.id.metadataAnalysisScore)
        val statisticalScoreView = alertView?.findViewById<TextView>(R.id.statisticalAnalysisScore)
        val pixelScoreView = alertView?.findViewById<TextView>(R.id.pixelAnalysisScore)
        
        // Get intent data
        val filePath = intent?.getStringExtra("file_path")
        val message = intent?.getStringExtra("alert_message") ?: "Suspicious file detected."
        val metadataScore = intent?.getStringExtra("metadata_score") ?: "METADATA ANALYSIS: N/A"
        val statisticalScore = intent?.getStringExtra("statistical_score") ?: "STATISTICAL ANALYSIS: N/A"
        val pixelScore = intent?.getStringExtra("pixel_score") ?: "PIXEL-LEVEL ANALYSIS: N/A"
        
        // Set text
        alertMessage?.text = message
        metadataScoreView?.text = metadataScore
        statisticalScoreView?.text = statisticalScore
        pixelScoreView?.text = pixelScore
        
        // Close button
        closeButton?.setOnClickListener {
            stopSelf()
        }
        
        // Block button
        blockButton?.setOnClickListener {
            if (!filePath.isNullOrEmpty()) {
                val fileToDelete = File(filePath)
                if (fileToDelete.exists()) {
                    if (fileToDelete.delete()) {
                        Log.d(TAG, "File deleted: $filePath")
                        Toast.makeText(this, "Image Blocked", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to delete file: $filePath")
                        Toast.makeText(this, "Error: Could not block image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "File not found for deletion: $filePath")
                    Toast.makeText(this, "Image already removed", Toast.LENGTH_SHORT).show()
                }
            }
            stopSelf()
        }
        
        // Add view to window
        windowManager.addView(alertView, params)
        
        return START_NOT_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        alertView?.let {
            windowManager.removeView(it)
        }
    }
}
