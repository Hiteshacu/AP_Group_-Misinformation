package com.antigravity.aimonitor.data

import com.antigravity.aimonitor.model.GeminiRequest
import com.antigravity.aimonitor.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API interface for Google Gemini AI
 */
interface GeminiApi {
    /**
     * Generate content using Gemini AI
     * @param model The model version to use (e.g., "gemini-2.5-flash", "gemini-1.5-pro")
     * @param apiKey Your Gemini API key
     * @param request The Gemini request with prompt
     * @return Gemini response with generated content
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
