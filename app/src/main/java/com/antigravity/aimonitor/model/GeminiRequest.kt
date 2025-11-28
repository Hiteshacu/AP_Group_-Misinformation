package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for Gemini AI API
 */
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<Content>,
    @SerializedName("tools")
    val tools: List<Tool>? = null
) {
    data class Content(
        @SerializedName("parts")
        val parts: List<Part>
    )
    
    data class Part(
        @SerializedName("text")
        val text: String
    )
    
    data class Tool(
        @SerializedName("google_search_retrieval")
        val googleSearchRetrieval: GoogleSearchRetrieval? = null
    )
    
    data class GoogleSearchRetrieval(
        @SerializedName("dynamic_retrieval_config")
        val dynamicRetrievalConfig: DynamicRetrievalConfig? = null
    )
    
    data class DynamicRetrievalConfig(
        @SerializedName("mode")
        val mode: String = "dynamic",
        @SerializedName("dynamic_threshold")
        val dynamicThreshold: Double = 0.7
    )
}
