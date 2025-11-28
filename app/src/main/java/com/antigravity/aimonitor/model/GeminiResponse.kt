package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Response model from Gemini AI API
 */
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>?
) {
    data class Candidate(
        @SerializedName("content")
        val content: Content?
    )
    
    data class Content(
        @SerializedName("parts")
        val parts: List<Part>?
    )
    
    data class Part(
        @SerializedName("text")
        val text: String?
    )
    
    /**
     * Extract the text response from Gemini
     */
    fun getTextResponse(): String? {
        return candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    }
}
