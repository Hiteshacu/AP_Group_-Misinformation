package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for fact-checking API
 * @property text The message text to analyze
 * @property links List of URLs extracted from the message
 */
data class FactCheckRequest(
    @SerializedName("text")
    val text: String,
    
    @SerializedName("links")
    val links: List<String> = emptyList()
)
