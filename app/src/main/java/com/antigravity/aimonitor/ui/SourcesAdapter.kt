package com.antigravity.aimonitor.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.antigravity.aimonitor.R
import com.antigravity.aimonitor.model.SourceInfo

/**
 * Adapter for displaying scanned sources in RecyclerView
 */
class SourcesAdapter(
    private val sources: List<SourceInfo>
) : RecyclerView.Adapter<SourcesAdapter.SourceViewHolder>() {

    class SourceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSourceTitle: TextView = view.findViewById(R.id.tvSourceTitle)
        val tvSourceSnippet: TextView = view.findViewById(R.id.tvSourceSnippet)
        val tvSourceUrl: TextView = view.findViewById(R.id.tvSourceUrl)
        val tvCredibility: TextView = view.findViewById(R.id.tvCredibility)
        val btnOpenSource: Button = view.findViewById(R.id.btnOpenSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source_card, parent, false)
        return SourceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val source = sources[position]
        
        holder.tvSourceTitle.text = source.title
        holder.tvSourceSnippet.text = source.snippet
        
        // Extract domain from URL for display
        val domain = try {
            Uri.parse(source.url).host ?: source.url
        } catch (e: Exception) {
            source.url
        }
        holder.tvSourceUrl.text = domain
        
        // Set credibility badge
        val credibilityText = when (source.credibility.uppercase()) {
            "HIGH" -> "✓ HIGH"
            "MEDIUM" -> "~ MEDIUM"
            "LOW" -> "! LOW"
            else -> "~ UNKNOWN"
        }
        holder.tvCredibility.text = credibilityText
        
        // Set credibility badge color
        val credibilityColor = when (source.credibility.uppercase()) {
            "HIGH" -> 0xFF4CAF50.toInt() // Green
            "MEDIUM" -> 0xFFFF9800.toInt() // Orange
            "LOW" -> 0xFFF44336.toInt() // Red
            else -> 0xFF9E9E9E.toInt() // Gray
        }
        holder.tvCredibility.setBackgroundColor(credibilityColor)
        
        // Open source URL when button clicked
        holder.btnOpenSource.setOnClickListener {
            try {
                var urlToOpen = source.url.trim()
                
                // Ensure URL has protocol
                if (!urlToOpen.startsWith("http://") && !urlToOpen.startsWith("https://")) {
                    urlToOpen = "https://$urlToOpen"
                }
                
                Log.d("SourcesAdapter", "Opening URL: $urlToOpen")
                
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                holder.itemView.context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("SourcesAdapter", "Error opening URL: ${source.url}", e)
                Toast.makeText(
                    holder.itemView.context,
                    "Cannot open URL: ${source.url}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount() = sources.size
}
