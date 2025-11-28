package com.antigravity.aimonitor.data

import com.antigravity.aimonitor.model.TavilyRequest
import com.antigravity.aimonitor.model.TavilyResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface for Tavily Search API
 */
interface TavilyApi {
    
    @POST("search")
    suspend fun search(
        @Body request: TavilyRequest
    ): TavilyResponse
}
