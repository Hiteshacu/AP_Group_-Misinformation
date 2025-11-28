package com.antigravity.aimonitor.data

import com.antigravity.aimonitor.model.GroqRequest
import com.antigravity.aimonitor.model.GroqResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API interface for Groq AI
 */
interface GroqApi {
    /**
     * Generate chat completion using Groq AI
     * @param authorization Bearer token with API key
     * @param request The Groq request with messages
     * @return Groq response with generated content
     */
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): GroqResponse
}
