package com.antigravity.aimonitor.util

import android.util.Log
import com.antigravity.aimonitor.data.ApiClient
import com.antigravity.aimonitor.model.ScanResult
import com.google.gson.Gson
import kotlinx.coroutines.withTimeout

/**
 * Scans multiple web sources using Gemini API with Google Search Grounding
 */
object SourceScanner {
    
    private const val TAG = "SourceScanner"
    private const val TIMEOUT_MS = 60000L // 60 seconds (web scanning needs more time)
    
    /**
     * Scan web sources for a given message
     * @param messageText The message to fact-check by scanning web sources
     * @return ScanResult with verdict, summary, and sources
     */
    suspend fun scanSources(messageText: String): ScanResult? {
        // Try 1: Google Search Grounding (Best, Native)
        try {
            Log.d(TAG, "🔍 Attempting Google Search Grounding for: ${messageText.take(50)}...")
            return scanWithGrounding(messageText)
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Google Search Grounding failed: ${e.message}")
        }

        // Try 2: Tavily Search API (Reliable Fallback for Real Sources)
        try {
            Log.d(TAG, "🔍 Attempting Tavily Search for: ${messageText.take(50)}...")
            return scanWithTavily(messageText)
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Tavily Search failed: ${e.message}")
        }

        // Try 3: Fallback to Reasoning (No Search, Just Analysis)
        Log.d(TAG, "🔄 Falling back to standard AI analysis...")
        return try {
            scanWithReasoning(messageText)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fallback analysis failed", e)
            null
        }
    }

    private suspend fun scanWithGrounding(messageText: String): ScanResult? {
        return withTimeout(TIMEOUT_MS) {
            val prompt = buildScanPrompt(messageText)
            
            // Create request with Google Search Grounding tool
            val request = com.antigravity.aimonitor.model.GeminiRequest(
                contents = listOf(
                    com.antigravity.aimonitor.model.GeminiRequest.Content(
                        parts = listOf(com.antigravity.aimonitor.model.GeminiRequest.Part(prompt))
                    )
                ),
                tools = listOf(
                    com.antigravity.aimonitor.model.GeminiRequest.Tool(
                        googleSearchRetrieval = com.antigravity.aimonitor.model.GeminiRequest.GoogleSearchRetrieval(
                            dynamicRetrievalConfig = com.antigravity.aimonitor.model.GeminiRequest.DynamicRetrievalConfig(
                                mode = "dynamic",
                                dynamicThreshold = 0.3
                            )
                        )
                    )
                )
            )

            val response = ApiClient.geminiApi.generateContent(
                model = "gemini-2.5-pro", // Use Pro for grounding
                apiKey = ApiClient.GEMINI_DETAIL_API_KEY,
                request = request
            )
            
            val aiResponse = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
            
            if (aiResponse.isNullOrBlank()) {
                throw Exception("Empty response from Gemini Grounding")
            }
            
            Log.d(TAG, "✅ Received Grounding response")
            parseResponse(aiResponse)
        }
    }
    
    private suspend fun scanWithTavily(messageText: String): ScanResult? {
        return withTimeout(TIMEOUT_MS) {
            // 1. Search Tavily
            val tavilyRequest = com.antigravity.aimonitor.model.TavilyRequest(
                query = "fact check $messageText",
                apiKey = ApiClient.TAVILY_API_KEY,
                searchDepth = "basic",
                maxResults = 5,
                includeAnswer = true
            )
            
            val tavilyResponse = ApiClient.tavilyApi.search(tavilyRequest)
            
            if (tavilyResponse.results.isEmpty()) {
                throw Exception("No results from Tavily")
            }
            
            // 2. Analyze results with Gemini (Reasoning)
            val sourcesText = tavilyResponse.results.joinToString("\n\n") { 
                "Source: ${it.title}\nURL: ${it.url}\nContent: ${it.content}" 
            }
            
            val analysisPrompt = """
                Analyze these search results to fact-check this claim: "$messageText"
                
                Search Results:
                $sourcesText
                
                Provide a JSON response with:
                1. Verdict (TRUE/FALSE/MISLEADING/UNVERIFIED)
                2. Confidence (0.0-1.0)
                3. Summary
                4. Sources (map the search results to the required format)
                
                Return ONLY valid JSON:
                {
                  "verdict": "TRUE/FALSE/MISLEADING/UNVERIFIED",
                  "confidence": 0.95,
                  "summary": "Based on the search results...",
                  "sources": [
                    {
                      "title": "Actual Title from search results",
                      "url": "Actual URL from search results",
                      "snippet": "Relevant excerpt",
                      "credibility": "HIGH"
                    }
                  ]
                }
            """.trimIndent()
            
            val geminiRequest = com.antigravity.aimonitor.model.GeminiRequest(
                contents = listOf(
                    com.antigravity.aimonitor.model.GeminiRequest.Content(
                        parts = listOf(com.antigravity.aimonitor.model.GeminiRequest.Part(analysisPrompt))
                    )
                )
            )
            
            val geminiResponse = ApiClient.geminiApi.generateContent(
                model = "gemini-2.5-pro", // Use Pro for reasoning
                apiKey = ApiClient.GEMINI_DETAIL_API_KEY,
                request = geminiRequest
            )
            
            val aiResponse = geminiResponse.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                
            if (aiResponse.isNullOrBlank()) {
                throw Exception("Empty analysis from Gemini")
            }
            
            Log.d(TAG, "✅ Received Tavily+Gemini analysis")
            parseResponse(aiResponse)
        }
    }

    private suspend fun scanWithReasoning(messageText: String): ScanResult? {
        return withTimeout(TIMEOUT_MS) {
            val prompt = buildReasoningPrompt(messageText)
            
            // Request WITHOUT tools
            val request = com.antigravity.aimonitor.model.GeminiRequest(
                contents = listOf(
                    com.antigravity.aimonitor.model.GeminiRequest.Content(
                        parts = listOf(com.antigravity.aimonitor.model.GeminiRequest.Part(prompt))
                    )
                )
            )

            val response = ApiClient.geminiApi.generateContent(
                model = "gemini-1.5-pro", // Use Pro for reasoning
                apiKey = ApiClient.GEMINI_DETAIL_API_KEY,
                request = request
            )
            
            val aiResponse = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
            
            if (aiResponse.isNullOrBlank()) {
                throw Exception("Empty response from Gemini Reasoning")
            }
            
            Log.d(TAG, "✅ Received Reasoning response")
            parseResponse(aiResponse)
        }
    }
    
    /**
     * Build prompt for Gemini with Google Search Grounding
     */
    private fun buildScanPrompt(messageText: String): String {
        return """Fact-check this claim using Google Search:

"$messageText"

Analyze the search results and provide:
1. Verdict: TRUE, FALSE, MISLEADING, or UNVERIFIED
2. Confidence score (0.0 to 1.0)
3. Summary explaining the verdict
4. The sources you used

Return ONLY valid JSON:
{
  "verdict": "TRUE/FALSE/MISLEADING/UNVERIFIED",
  "confidence": 0.95,
  "summary": "Based on search results...",
  "sources": [
    {
      "title": "Actual Page Title",
      "url": "https://actual-url.com",
      "snippet": "Relevant excerpt",
      "credibility": "HIGH"
    }
  ]
}"""
    }

    /**
     * Build prompt for detailed AI analysis (Fallback)
     */
    private fun buildReasoningPrompt(messageText: String): String {
        return """Provide a detailed fact-check analysis of this claim:

"$messageText"

Provide a thorough analysis with:
1. Verdict: TRUE, FALSE, MISLEADING, or UNVERIFIED
2. Confidence score (0.0 to 1.0)
3. Detailed summary (4-5 sentences minimum) explaining:
   - Why you reached this verdict
   - Key facts that support or refute the claim
   - Any context or nuance needed
4. Multiple reasoning points to support your analysis

Return ONLY valid JSON in this format:
{
  "verdict": "TRUE/FALSE/MISLEADING/UNVERIFIED",
  "confidence": 0.95,
  "summary": "Detailed explanation of the verdict with facts and context...",
  "sources": [
    {
      "title": "Key Point 1: Why this claim is [verdict]",
      "url": "reasoning",
      "snippet": "Detailed explanation of this reasoning point with facts and context",
      "credibility": "HIGH"
    },
    {
      "title": "Key Point 2: Additional context",
      "url": "reasoning",
      "snippet": "Another aspect of the analysis with supporting information",
      "credibility": "HIGH"
    }
  ]
}"""
    }
    
    /**
     * Parse Gemini response into ScanResult
     */
    private fun parseResponse(response: String): ScanResult? {
        return try {
            // Extract JSON from response
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}") + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.e(TAG, "No JSON found in response")
                return null
            }
            
            val jsonString = response.substring(jsonStart, jsonEnd)
            Log.d(TAG, "Parsing JSON: ${jsonString.take(200)}...")
            
            val gson = Gson()
            val result = gson.fromJson(jsonString, ScanResult::class.java)
            
            Log.d(TAG, "✅ Parsed scan result:")
            Log.d(TAG, "   Verdict: ${result.verdict}")
            Log.d(TAG, "   Confidence: ${result.confidence}")
            Log.d(TAG, "   Sources found: ${result.sources.size}")
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing scan response", e)
            null
        }
    }
}
