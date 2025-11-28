package com.antigravity.aimonitor.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.util.SourceScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen activity that scans web sources and displays results
 * in Perplexity-style UI
 */
class DetailSourcesActivity : AppCompatActivity() {
    
    private companion object {
        const val TAG = "DetailSourcesActivity"
        const val EXTRA_MESSAGE_TEXT = "message_text"
    }
    
    private lateinit var btnBack: ImageButton
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingMessage: TextView
    private lateinit var verdictCard: CardView
    private lateinit var tvVerdict: TextView
    private lateinit var tvConfidenceScore: TextView
    private lateinit var tvSummary: TextView
    private lateinit var rvSources: RecyclerView
    private lateinit var errorContainer: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button
    
    private var messageText: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "========================================")
        Log.d(TAG, "🚀 DetailSourcesActivity onCreate called")
        setContentView(R.layout.activity_detail_sources)
        
        // Get message text from intent
        messageText = intent.getStringExtra(EXTRA_MESSAGE_TEXT)
        Log.d(TAG, "📩 Message text from intent: $messageText")
        
        if (messageText.isNullOrBlank()) {
            Log.e(TAG, "❌ Message text is null or blank - finishing activity")
            finish()
            return
        }
        
        Log.d(TAG, "✅ Message text received, initializing views...")
        initViews()
        Log.d(TAG, "🔍 Starting source scan...")
        scanSources()
    }
    
    private fun initViews() {
        Log.d(TAG, "Initializing UI views...")
        btnBack = findViewById(R.id.btnBack)
        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage)
        verdictCard = findViewById(R.id.verdictCard)
        tvVerdict = findViewById(R.id.tvVerdict)
        tvConfidenceScore = findViewById(R.id.tvConfidenceScore)
        tvSummary = findViewById(R.id.tvSummary)
        rvSources = findViewById(R.id.rvSources)
        errorContainer = findViewById(R.id.errorContainer)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
        
        btnBack.setOnClickListener {
            finish()
        }
        
        btnRetry.setOnClickListener {
            scanSources()
        }
        
        // Setup RecyclerView
        rvSources.layoutManager = LinearLayoutManager(this)
    }
    
    private fun scanSources() {
        Log.d(TAG, "📡 scanSources() called")
        showLoading()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "🌐 Starting web source scan...")
                Log.d(TAG, "   Message: \"${messageText?.take(100)}...\"")
                
                val result = withContext(Dispatchers.IO) {
                    Log.d(TAG, "   Calling SourceScanner.scanSources()...")
                    SourceScanner.scanSources(messageText!!)
                }
                
                Log.d(TAG, "📥 Scan completed, result: ${if (result == null) "NULL" else "SUCCESS"}")
                
                if (result == null) {
                    Log.e(TAG, "❌ SourceScanner returned null")
                    showError("Failed to scan sources. Please try again.")
                    return@launch
                }
                
                Log.d(TAG, "✅ Got ${result.sources.size} sources, displaying results...")
                // Display results
                showResults(result)
                 
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in scanSources", e)
                Log.e(TAG, "   Error message: ${e.message}")
                Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
                showError("An error occurred: ${e.message}")
            }
        }
    }
    
    private fun showLoading() {
        loadingContainer.visibility = View.VISIBLE
        verdictCard.visibility = View.GONE
        rvSources.visibility = View.GONE
        errorContainer.visibility = View.GONE
    }
    
    private fun showResults(result: com.antigravity.aimonitor.model.ScanResult) {
        loadingContainer.visibility = View.GONE
        verdictCard.visibility = View.VISIBLE
        rvSources.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
        
        // Set verdict
        tvVerdict.text = "VERDICT: ${result.verdict}"
        
        // Set verdict color
        val verdictColor = when (result.verdict.uppercase()) {
            "TRUE" -> getColor(android.R.color.holo_green_dark)
            "FALSE" -> getColor(android.R.color.holo_red_dark)
            "MISLEADING" -> getColor(android.R.color.holo_orange_dark)
            else -> getColor(android.R.color.darker_gray)
        }
        tvVerdict.setTextColor(verdictColor)
        
        // Set confidence
        tvConfidenceScore.text = String.format("Confidence: %.0f%%", result.confidence * 100)
        
        // Set summary
        tvSummary.text = result.summary
        
        // Set sources
        val adapter = SourcesAdapter(result.sources)
        rvSources.adapter = adapter
        
        Log.d(TAG, "Displayed ${result.sources.size} sources")
    }
    
    private fun showError(message: String) {
        loadingContainer.visibility = View.GONE
        verdictCard.visibility = View.GONE
        rvSources.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
        
        tvErrorMessage.text = message
    }
}
