package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for Groq API
 */
data class GroqRequest(
    @SerializedName("model")
    val model: String = "llama-3.1-70b-versatile",
    
    @SerializedName("messages")
    val messages: List<Message>,
    
    @SerializedName("temperature")
    val temperature: Double = 0.3,
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024
) {
    data class Message(
        @SerializedName("role")
        val role: String,
        
        @SerializedName("content")
        val content: String
    )
}
