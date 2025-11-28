package com.antigravity.aimonitor.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.util.FactCheckCache
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog that displays detailed fact-check information
 * for a flagged message
 */
class FactCheckBottomSheet : BottomSheetDialogFragment() {
    
    companion object {
        const val EXTRA_MESSAGE_TEXT = "message_text"
        
        /**
         * Factory method to create a new instance
         * @param messageText The message text to display fact-check info for
         */
        fun newInstance(messageText: String): FactCheckBottomSheet {
            return FactCheckBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_MESSAGE_TEXT, messageText)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Set transparent background for dialog window
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return inflater.inflate(R.layout.fragment_fact_check_bottom_sheet, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get message text from arguments
        val messageText = arguments?.getString(EXTRA_MESSAGE_TEXT)
        
        if (messageText.isNullOrBlank()) {
            dismiss()
            return
        }
        
        // Retrieve flagged message from cache
        val flaggedMessage = FactCheckCache.getFlaggedMessage(messageText)
        
        if (flaggedMessage == null) {
            dismiss()
            return
        }
        
        // Populate views with fact-check data
        val response = flaggedMessage.factCheckResponse
        
        view.findViewById<TextView>(R.id.tvMessage).text = messageText
        
        // Set label with color coding
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val severityText = if (response.isHumor) "HUMOR / SATIRE" else "${response.severity} SEVERITY"
        tvLabel.text = "${response.label} • $severityText"
        tvLabel.setTextColor(getLabelColor(response.label))
        
        // Format confidence as percentage
        val tvConfidence = view.findViewById<TextView>(R.id.tvConfidence)
        tvConfidence.text = String.format("%.1f%% confidence", response.confidence * 100)
        
        // Set explanation
        view.findViewById<TextView>(R.id.tvExplanation).text = response.explanation
        
        // Format sources as bulleted list
        val tvSources = view.findViewById<TextView>(R.id.tvSources)
        if (response.sources.isNotEmpty()) {
            val sourcesText = response.sources.joinToString("\n") { "• $it" }
            tvSources.text = sourcesText
        } else {
            tvSources.text = "No sources available"
        }
        
        // Detail button - launches detailed source scanner
        view.findViewById<android.widget.Button>(R.id.btnDetailSources).setOnClickListener {
            val intent = android.content.Intent(requireContext(), DetailSourcesActivity::class.java)
            intent.putExtra("message_text", messageText)
            startActivity(intent)
        }
    }
    
    /**
     * Get color for label based on classification
     * @param label The classification label (FALSE, MISLEADING, TRUE, etc.)
     * @return Color integer for the label
     */
    private fun getLabelColor(label: String): Int {
        return when (label.uppercase()) {
            "FALSE" -> Color.parseColor("#F44336") // Red
            "MISLEADING" -> Color.parseColor("#FF9800") // Orange
            "TRUE" -> Color.parseColor("#4CAF50") // Green
            else -> Color.parseColor("#9E9E9E") // Gray
        }
    }
}
