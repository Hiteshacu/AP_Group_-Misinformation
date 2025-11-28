package com.antigravity.aimonitor.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for Tavily Search API
 */
data class TavilyRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("search_depth")
    val searchDepth: String = "basic", // "basic" or "advanced"
    @SerializedName("include_answer")
    val includeAnswer: Boolean = true,
    @SerializedName("include_images")
    val includeImages: Boolean = false,
    @SerializedName("include_raw_content")
    val includeRawContent: Boolean = false,
    @SerializedName("max_results")
    val maxResults: Int = 5,
    @SerializedName("api_key")
    val apiKey: String
)

/**
 * Response model for Tavily Search API
 */
data class TavilyResponse(
    @SerializedName("answer")
    val answer: String?,
    @SerializedName("query")
    val query: String,
    @SerializedName("results")
    val results: List<TavilySearchResult>
)

data class TavilySearchResult(
    @SerializedName("title")
    val title: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("score")
    val score: Double
)
