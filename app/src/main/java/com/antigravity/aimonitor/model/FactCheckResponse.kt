package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Response model from fact-checking API
 * @property isMisinformation Whether the message contains misinformation
 * @property confidence Confidence score (0.0 to 1.0)
 * @property label Classification label (e.g., "FALSE", "MISLEADING", "TRUE")
 * @property explanation Brief explanation of the fact-check result
 * @property sources List of authoritative sources used for verification
 */
data class FactCheckResponse(
    @SerializedName("isMisinformation")
    val isMisinformation: Boolean,
    
    @SerializedName("confidence")
    val confidence: Double,
    
    @SerializedName("label")
    val label: String,
    
    @SerializedName("explanation")
    val explanation: String,
    
    @SerializedName("sources")
    val sources: List<String>,

    @SerializedName("severity")
    val severity: String = "NONE", // LOW, MEDIUM, HIGH, NONE

    @SerializedName("isHumor")
    val isHumor: Boolean = false
)
