package com.antigravity.aimonitor.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.service.SteganographyMonitorService

/**
 * Activity for steganography detection monitoring
 * Allows users to start/stop monitoring and configure settings
 */
class SteganographyActivity : AppCompatActivity() {
    
    private lateinit var toggleServiceButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var deepScanSwitch: Switch
    private lateinit var sharedPreferences: android.content.SharedPreferences
    
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> = 
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            
            if (allGranted) {
                checkOverlayPermission()
            } else {
                Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_LONG).show()
            }
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steganography)
        
        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        statusTextView = findViewById(R.id.statusTextView)
        deepScanSwitch = findViewById(R.id.deepScanSwitch)
        
        sharedPreferences = getSharedPreferences("StegDetectorPrefs", MODE_PRIVATE)
        
        // Load and set switch state
        val deepScanEnabled = sharedPreferences.getBoolean("deep_scan_enabled", false)
        deepScanSwitch.isChecked = deepScanEnabled
        
        // Save switch state on change
        deepScanSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("deep_scan_enabled", isChecked).apply()
        }
        
        toggleServiceButton.setOnClickListener {
            toggleService()
        }
        
        checkAndRequestPermissions()
        checkStoragePermission()
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }
    
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:${applicationContext.packageName}")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivity(intent)
                }
            }
        }
    }
    
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
    
    private fun toggleService() {
        val serviceIntent = Intent(this, SteganographyMonitorService::class.java)
        if (SteganographyMonitorService.isRunning) {
            stopService(serviceIntent)
            updateUI(false)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            updateUI(true)
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUI(SteganographyMonitorService.isRunning)
    }
    
    private fun updateUI(isServiceRunning: Boolean) {
        if (isServiceRunning) {
            statusTextView.text = "MONITORING ACTIVE"
            toggleServiceButton.text = "STOP MONITORING"
        } else {
            statusTextView.text = "MONITORING INACTIVE"
            toggleServiceButton.text = "START MONITORING"
        }
    }
}
