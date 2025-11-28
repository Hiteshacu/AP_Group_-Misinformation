package com.antigravity.aimonitor.data

import com.antigravity.aimonitor.model.FactCheckRequest
import com.antigravity.aimonitor.model.FactCheckResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for fact-checking service
 */
interface FactCheckApi {
    /**
     * Analyze message content for misinformation
     * @param request The fact-check request containing message text and links
     * @return Fact-check response with analysis results
     */
    @POST("analyze")
    suspend fun analyze(@Body request: FactCheckRequest): FactCheckResponse
}
