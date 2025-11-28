package com.antigravity.aimonitor.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.antigravity.aimonitor.model.DetectionResult
import java.io.InputStream

/**
 * Steganography detector with multiple detection methods
 * Detects hidden data in images using:
 * 1. Metadata scanning (EXIF data)
 * 2. Chi-Square statistical analysis
 * 3. LSB (Least Significant Bit) analysis
 */
class SteganographyDetector(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("StegDetectorPrefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "SteganographyDetector"
        private const val STEG_TAG = "Stego"
    }
    
    /**
     * Detect steganography in an image
     * First checks metadata (fast), then performs deep scan if enabled
     */
    fun detect(imageUri: Uri): DetectionResult {
        // First, check metadata (fast and reliable)
        val metadataResult = detectViaMetadata(imageUri)
        if (metadataResult.isPositive()) {
            return metadataResult
        }
        
        // If metadata check is negative, proceed to deep scan if enabled
        val deepScanEnabled = sharedPreferences.getBoolean("deep_scan_enabled", false)
        if (deepScanEnabled) {
            // Perform Chi-Square Test
            val chiSquareResult = detectViaChiSquare(imageUri)
            if (chiSquareResult.isPositive()) {
                return chiSquareResult
            }
            
            // Perform LSB Analysis
            val lsbResult = detectViaLsb(imageUri)
            if (lsbResult.isPositive()) {
                return lsbResult
            }
        }
        
        // If all checks are negative
        return DetectionResult.createNegativeResult()
    }
    
    /**
     * Detect steganography via EXIF metadata
     */
    private fun detectViaMetadata(uri: Uri): DetectionResult {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return DetectionResult.createNegativeResult()
            
            inputStream.use { stream ->
                val exifInterface = ExifInterface(stream)
                val userComment = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
                
                if (userComment != null && userComment.contains(STEG_TAG)) {
                    return DetectionResult(
                        true,
                        "Metadata",
                        "Steganography signature found in EXIF UserComment."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during metadata scan", e)
        }
        return DetectionResult.createNegativeResult()
    }
    
    /**
     * Detect steganography via Chi-Square statistical analysis
     */
    private fun detectViaChiSquare(uri: Uri): DetectionResult {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) return DetectionResult.createNegativeResult()
            
            if (analyzeChiSquare(bitmap)) {
                bitmap.recycle()
                return DetectionResult(
                    true,
                    "Chi-Square",
                    "Statistical anomalies detected in pixel distribution."
                )
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Chi-Square analysis", e)
        }
        return DetectionResult.createNegativeResult()
    }
    
    /**
     * Detect steganography via LSB (Least Significant Bit) analysis
     */
    private fun detectViaLsb(uri: Uri): DetectionResult {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) return DetectionResult.createNegativeResult()
            
            if (analyzeLsb(bitmap)) {
                bitmap.recycle()
                return DetectionResult(
                    true,
                    "LSB Analysis",
                    "Non-random patterns detected in Least Significant Bits."
                )
            }
            
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error during LSB analysis", e)
        }
        
        return DetectionResult.createNegativeResult()
    }
    
    /**
     * Performs a Chi-Square test on the image's pixel value distribution.
     * A simple implementation checks for unusually flat distributions.
     */
    private fun analyzeChiSquare(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val histogram = LongArray(256) // Grayscale histogram
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                histogram[gray]++
            }
        }
        
        // A very basic test: check if the histogram is unnaturally uniform.
        val totalPixels = width * height.toLong()
        val expected = totalPixels / 256
        var chiSquare = 0.0
        
        for (observed in histogram) {
            chiSquare += Math.pow((observed - expected).toDouble(), 2.0) / expected
        }
        
        // An arbitrary threshold indicating significant deviation
        val threshold = 293.0 // Corresponds to p-value of ~0.05 for 255 degrees of freedom
        Log.d(TAG, "Chi-Square value: $chiSquare")
        return chiSquare < threshold // A low Chi-Square value can indicate an unnaturally flat distribution
    }
    
    /**
     * Analyzes the Least Significant Bits for non-random patterns.
     * This simple version checks for long, uninterrupted sequences of 0s or 1s.
     */
    private fun analyzeLsb(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        var maxRun = 0
        var currentRun = 0
        var lastLsb = -1
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val lsb = pixel and 1 // Check LSB of the blue channel (often used)
                
                if (lsb == lastLsb) {
                    currentRun++
                } else {
                    maxRun = maxOf(maxRun, currentRun)
                    currentRun = 1
                    lastLsb = lsb
                }
            }
        }
        maxRun = maxOf(maxRun, currentRun)
        
        // If there's a very long, unbroken run of the same bit, it might be suspicious.
        val threshold = 500 // Arbitrary threshold for a suspicious run length
        Log.d(TAG, "LSB max run: $maxRun")
        return maxRun > threshold
    }
}
