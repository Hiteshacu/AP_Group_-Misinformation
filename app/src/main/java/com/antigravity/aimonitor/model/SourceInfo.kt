package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Information about a single source found during web scanning
 */
data class SourceInfo(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("snippet")
    val snippet: String,
    
    @SerializedName("credibility")
    val credibility: String = "MEDIUM" // HIGH, MEDIUM, LOW
)

/**
 * Result of scanning multiple web sources
 */
data class ScanResult(
    @SerializedName("verdict")
    val verdict: String, // TRUE, FALSE, MISLEADING, UNVERIFIED
    
    @SerializedName("confidence")
    val confidence: Double,
    
    @SerializedName("summary")
    val summary: String,
    
    @SerializedName("sources")
    val sources: List<SourceInfo>
)
