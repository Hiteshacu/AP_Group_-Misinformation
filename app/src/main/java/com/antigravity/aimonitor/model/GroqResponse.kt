package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for Groq API
 */
data class GroqResponse(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("choices")
    val choices: List<Choice>?,
    
    @SerializedName("model")
    val model: String?,
    
    @SerializedName("usage")
    val usage: Usage?
) {
    data class Choice(
        @SerializedName("index")
        val index: Int?,
        
        @SerializedName("message")
        val message: Message?,
        
        @SerializedName("finish_reason")
        val finishReason: String?
    )
    
    data class Message(
        @SerializedName("role")
        val role: String?,
        
        @SerializedName("content")
        val content: String?
    )
    
    data class Usage(
        @SerializedName("prompt_tokens")
        val promptTokens: Int?,
        
        @SerializedName("completion_tokens")
        val completionTokens: Int?,
        
        @SerializedName("total_tokens")
        val totalTokens: Int?
    )
    
    /**
     * Extract text response from Groq response
     */
    fun getTextResponse(): String? {
        return choices?.firstOrNull()?.message?.content
    }
}
