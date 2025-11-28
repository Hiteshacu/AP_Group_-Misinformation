package com.antigravity.aimonitor.util

import android.util.Log
import com.antigravity.aimonitor.data.ApiClient
import com.antigravity.aimonitor.model.FactCheckResponse
import com.antigravity.aimonitor.model.GeminiRequest
import com.antigravity.aimonitor.model.GroqRequest
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

/**
 * Utility class for fact-checking messages using Gemini AI with Groq fallback
 */
object GeminiFactChecker {
    
    private const val TAG = "GeminiFactChecker"
    
    /**
     * Test function to diagnose API issues
     * Call this from DebugActivity to see detailed error information
     */
    suspend fun testAPIs(): String {
        val results = StringBuilder()
        results.appendLine("========================================")
        results.appendLine("🧪 API DIAGNOSTIC TEST")
        results.appendLine("========================================")
        
        // Test Gemini
        results.appendLine("\n📍 Testing Gemini API...")
        results.appendLine("Key: ${ApiClient.GEMINI_API_KEY.take(20)}...")
        val geminiResult = tryGemini("Test message for misinformation detection", emptyList())
        if (geminiResult != null) {
            results.appendLine("✅ Gemini: SUCCESS")
            results.appendLine("   Result: ${geminiResult.label}")
        } else {
            results.appendLine("❌ Gemini: FAILED (check logs for details)")
        }
        
        // Test Groq
        results.appendLine("\n📍 Testing Groq API...")
        results.appendLine("Key: ${ApiClient.GROQ_API_KEY.take(20)}...")
        val groqResult = tryGroq("Test message for misinformation detection", emptyList())
        if (groqResult != null) {
            results.appendLine("✅ Groq: SUCCESS")
            results.appendLine("   Result: ${groqResult.label}")
        } else {
            results.appendLine("❌ Groq: FAILED (check logs for details)")
        }
        
        results.appendLine("\n========================================")
        results.appendLine("📊 SUMMARY")
        results.appendLine("Gemini: ${if (geminiResult != null) "✅ Working" else "❌ Failed"}")
        results.appendLine("Groq: ${if (groqResult != null) "✅ Working" else "❌ Failed"}")
        results.appendLine("========================================")
        
        val summary = results.toString()
        Log.d(TAG, summary)
        return summary
    }
    
    // FOCUSED system prompt for IMPORTANT misinformation detection only
    // RED BADGES ONLY for: Phishing URLs, Fake Domains, Scientific Misinformation
    private const val SYSTEM_PROMPT = """You are an EXPERT misinformation detection AI specialized in detecting CRITICAL threats.

⚠️ CRITICAL: ONLY flag messages that contain IMPORTANT/DANGEROUS misinformation.

🔴 FLAG AS HIGH SEVERITY (Show Red Badge) ONLY FOR:
1. PHISHING URLs & SCAMS:
   - Suspicious URLs (iplogger, grabify, bit.ly leading to scams)
   - Fake login pages (fake-paypal.com, g00gle.com)
   - Financial scams ("Send money", "Free money", "Crypto investment")
   - Prize/lottery scams ("You won", "Claim prize")

2. SCIENTIFIC MISINFORMATION:
   - False scientific claims ("Earth is flat", "Vaccines cause autism")
   - Medical misinformation ("Drink bleach cures cancer", "COVID is fake")
   - Climate denial ("Climate change is hoax")

3. DANGEROUS FALSE NEWS:
   - Major false events ("War declared", "President arrested")
   - Public safety threats ("Bomb threat", "Terrorist attack")
   - Emergency hoaxes ("Earthquake coming", "Tsunami warning")

4. HEALTH RISKS:
   - Dangerous medical advice ("Don't take medicine", "Essential oils cure disease")
   - Deadly food misinformation ("Eating X will kill you")

❌ DO NOT FLAG (Ignore completely):
- Humor, jokes, sarcasm, memes with 😂🤣 emojis
- Casual exaggerations ("I'm starving", "I'm dying of boredom")
- Unverified rumors ("I heard...", "Someone said...")
- Gossip or hearsay
- Opinions ("This movie sucks")
- Minor inaccuracies or typos
- Regular messages without misinformation

JSON FORMAT (respond with ONLY this):
{"isMisinformation":true/false,"confidence":0.0-1.0,"label":"FALSE/SCAM/TRUE","explanation":"brief explanation","sources":["source1"],"severity":"HIGH/NONE","isHumor":false}

STRICT RULES:
- severity MUST be either "HIGH" or "NONE" (NO other values!)
- ONLY set severity="HIGH" for CRITICAL threats listed above
- For everything else (humor, rumors, harmless content): severity="NONE", isMisinformation=false

EXAMPLES:
✅ URL "http://iplogger.org/abc123" → {"isMisinformation":true,"severity":"HIGH","label":"SCAM","isHumor":false,"confidence":0.99,"explanation":"Phishing URL that steals IP addresses.","sources":["Security databases"]}
✅ "The earth is flat" → {"isMisinformation":true,"severity":"HIGH","label":"FALSE","isHumor":false,"confidence":0.99,"explanation":"Scientific misinformation contradicting established science.","sources":["NASA","Scientific consensus"]}
✅ "I'm so hungry I could eat a horse 😂" → {"isMisinformation":false,"severity":"NONE","label":"TRUE","isHumor":false,"confidence":0.95,"explanation":"Harmless exaggeration, not misinformation.","sources":[]}
✅ "I heard the exam is cancelled" → {"isMisinformation":false,"severity":"NONE","label":"TRUE","isHumor":false,"confidence":0.9,"explanation":"Unverified rumor, not critical misinformation.","sources":[]}
✅ "Click here for free iPhone: bit.ly/scam123" → {"isMisinformation":true,"severity":"HIGH","label":"SCAM","isHumor":false,"confidence":0.98,"explanation":"Phishing scam using shortened URL.","sources":["Scam detection"]}

Message to analyze: """
    
    /**
     * Analyze MULTIPLE messages efficiently
     * Uses parallel processing for speed while maintaining accuracy
     * @param messages List of messages to check
     * @return Map of message text to FactCheckResponse
     */
    suspend fun analyzeMessagesBatch(messages: List<String>): Map<String, FactCheckResponse> {
        if (messages.isEmpty()) return emptyMap()
        
        Log.d(TAG, "")
        Log.d(TAG, "========================================")
        Log.d(TAG, "📦 BATCH ANALYSIS STARTED")
        Log.d(TAG, "📊 Analyzing ${messages.size} messages")
        Log.d(TAG, "🚀 Using parallel processing for speed")
        Log.d(TAG, "========================================")
        
        // Analyze messages in parallel (faster than sequential)
        return coroutineScope {
            val results = mutableMapOf<String, FactCheckResponse>()
            
            // Process in chunks of 5 for optimal performance
            messages.chunked(5).forEach { chunk ->
                val chunkResults: List<Pair<String, FactCheckResponse?>> = chunk.map { message ->
                    async {
                        Pair(message, analyzeMessage(message, emptyList()))
                    }
                }.awaitAll()
                
                // Add successful results
                for (pair in chunkResults) {
                    val (message, response) = pair
                    if (response != null) {
                        results[message] = response
                    }
                }
            }
            
            Log.d(TAG, "✅ Batch analysis complete: ${results.size}/${messages.size} analyzed")
            results
        }
    }
    

    
    /**
     * Analyze a message using BOTH Gemini and Groq simultaneously
     * Returns the result from whichever API responds first
     * OPTIMIZED: Faster racing logic with immediate return
     * @param messageText The message to fact-check
     * @param links Optional list of URLs in the message
     * @return FactCheckResponse with analysis results, or null if both fail
     */
    suspend fun analyzeMessage(messageText: String, links: List<String> = emptyList()): FactCheckResponse? {
        return try {
            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "🏁 DUAL API RACING STARTED")
            Log.d(TAG, "📝 Message: ${messageText.take(50)}...")
            Log.d(TAG, "🔄 Both Gemini & Groq running simultaneously")
            Log.d(TAG, "⚡ Fastest response wins!")
            Log.d(TAG, "========================================")
            val startTime = System.currentTimeMillis()
            
            // Race both APIs simultaneously with optimized logic
            coroutineScope {
                val geminiDeferred = async { tryGemini(messageText, links) }
                val groqDeferred = async { tryGroq(messageText, links) }
                
                // Use select to get the FIRST non-null result
                var result: FactCheckResponse? = null
                var winner = ""
                
                // Try to get first successful response
                try {
                    result = select<FactCheckResponse?> {
                        geminiDeferred.onAwait { res ->
                            if (res != null) {
                                winner = "Gemini"
                                res
                            } else null
                        }
                        groqDeferred.onAwait { res ->
                            if (res != null) {
                                winner = "Groq"
                                res
                            } else null
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Select failed: ${e.message}")
                }
                
                // If select returned null, wait for both and take first non-null
                if (result == null) {
                    Log.d(TAG, "⏳ First API returned null, checking second API...")
                    val results = listOf(geminiDeferred.await(), groqDeferred.await())
                    result = results.firstOrNull { it != null }
                    winner = if (results[0] != null) "Gemini (fallback)" else if (results[1] != null) "Groq (fallback)" else "None"
                }
                
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "")
                Log.d(TAG, "========================================")
                if (result != null) {
                    Log.d(TAG, "🏆 WINNER: $winner API")
                    Log.d(TAG, "⏱️ Response time: ${elapsed}ms")
                    Log.d(TAG, "✅ Misinformation: ${result.isMisinformation}")
                    Log.d(TAG, "📊 Confidence: ${String.format("%.0f%%", result.confidence * 100)}")
                    Log.d(TAG, "🏷️ Label: ${result.label}")
                } else {
                    Log.e(TAG, "❌ BOTH APIs FAILED")
                    Log.e(TAG, "⏱️ Total time: ${elapsed}ms")
                    Log.e(TAG, "⚠️ Check API keys and internet connection")
                }
                Log.d(TAG, "========================================")
                Log.d(TAG, "")
                
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in API race: ${e.message}", e)
            null
        }
    }
    
    /**
     * Try to analyze message using Gemini AI
     */
    private suspend fun tryGemini(messageText: String, links: List<String>): FactCheckResponse? {
        try {
            val fullPrompt = buildPrompt(messageText, links)
            
            Log.d(TAG, "Trying Gemini AI: ${messageText.take(100)}...")
            Log.d(TAG, "Gemini API Key: ${ApiClient.GEMINI_API_KEY.take(10)}...")
            
            val request = GeminiRequest(
                contents = listOf(
                    GeminiRequest.Content(
                        parts = listOf(
                            GeminiRequest.Part(text = fullPrompt)
                        )
                    )
                )
            )
            
            Log.d(TAG, "Calling Gemini API (Flash)...")
            val response = ApiClient.geminiApi.generateContent(
                model = "gemini-1.5-flash", // Use Flash for speed
                apiKey = ApiClient.GEMINI_API_KEY,
                request = request
            )
            
            val textResponse = response.getTextResponse()
            
            if (textResponse.isNullOrBlank()) {
                Log.e(TAG, "❌ Empty response from Gemini")
                return null
            }
            
            Log.d(TAG, "✅ Gemini response received: ${textResponse.take(200)}...")
            return parseResponse(textResponse, messageText)
            
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ Gemini: No internet connection")
            return null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Gemini: Request timeout")
            return null
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Gemini HTTP error: ${e.code()} - ${e.message()}")
            Log.e(TAG, "Response body: $errorBody")
            
            // Check for quota exceeded error
            if (e.code() == 429 || errorBody?.contains("quota", ignoreCase = true) == true) {
                Log.w(TAG, "⚠️ Gemini QUOTA EXCEEDED - Groq will handle this request")
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Gemini API error: ${e.javaClass.simpleName} - ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Try to analyze message using Groq AI (optimized for speed)
     */
    private suspend fun tryGroq(messageText: String, links: List<String>): FactCheckResponse? {
        try {
            val fullPrompt = buildPrompt(messageText, links)
            
            Log.d(TAG, "Trying Groq AI: ${messageText.take(50)}...")
            Log.d(TAG, "Groq API Key: ${ApiClient.GROQ_API_KEY.take(10)}...")
            
            val request = GroqRequest(
                model = "llama-3.1-8b-instant", // Fastest model
                messages = listOf(
                    GroqRequest.Message(
                        role = "user",
                        content = fullPrompt
                    )
                ),
                temperature = 0.0, // Zero for maximum speed and determinism
                maxTokens = 256 // Reduced further for speed
            )
            
            Log.d(TAG, "Calling Groq API with model: llama-3.1-8b-instant...")
            val response = ApiClient.groqApi.chatCompletion(
                authorization = "Bearer ${ApiClient.GROQ_API_KEY}",
                request = request
            )
            
            val textResponse = response.getTextResponse()
            
            if (textResponse.isNullOrBlank()) {
                Log.e(TAG, "❌ Empty response from Groq")
                return null
            }
            
            Log.d(TAG, "✅ Groq response received: ${textResponse.take(200)}...")
            return parseResponse(textResponse, messageText)
            
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ Groq: No internet connection")
            return null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Groq: Request timeout")
            return null
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Groq HTTP error: ${e.code()} - ${e.message()}")
            Log.e(TAG, "❌ Groq error body: $errorBody")
            Log.e(TAG, "❌ Groq request URL: ${e.response()?.raw()?.request?.url}")
            
            // Check for quota/rate limit errors
            if (e.code() == 429 || errorBody?.contains("rate limit", ignoreCase = true) == true) {
                Log.w(TAG, "⚠️ Groq RATE LIMITED - Gemini will handle this request")
            } else if (e.code() == 401) {
                Log.e(TAG, "⚠️ Groq AUTHENTICATION FAILED - Check API key")
            } else if (e.code() == 400) {
                Log.e(TAG, "⚠️ Groq BAD REQUEST - Check request format")
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Groq API error: ${e.javaClass.simpleName} - ${e.message}", e)
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Build the complete prompt for Gemini
     */
    private fun buildPrompt(messageText: String, links: List<String>): String {
        val prompt = StringBuilder(SYSTEM_PROMPT)
        prompt.append("\"$messageText\"")
        
        if (links.isNotEmpty()) {
            prompt.append("\n\nLinks in message: ${links.joinToString(", ")}")
        }
        
        val finalPrompt = prompt.toString()
        Log.d(TAG, "🔍 DEBUG: Prompt being sent (first 500 chars):")
        Log.d(TAG, finalPrompt.take(500))
        return finalPrompt
    }
    
    /**
     * Parse AI response into FactCheckResponse (works for both Gemini and Groq)
     * @param textResponse The AI's JSON response
     * @param originalMessage The original message that was analyzed
     */
    private fun parseResponse(textResponse: String, originalMessage: String): FactCheckResponse? {
        try {
            // Extract JSON from response (AI might add extra text)
            val jsonStart = textResponse.indexOf("{")
            val jsonEnd = textResponse.lastIndexOf("}") + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                Log.e(TAG, "No JSON found in response")
                return null
            }
            
            val jsonString = textResponse.substring(jsonStart, jsonEnd)
            Log.d(TAG, "🔍 Extracted JSON: $jsonString")
            
            // Parse JSON
            val gson = Gson()
            val rawResponse = gson.fromJson(jsonString, FactCheckResponse::class.java)
            
            // Log the raw AI response for debugging
            Log.d(TAG, "")
            Log.d(TAG, "📋 RAW AI RESPONSE:")
            Log.d(TAG, "   isMisinformation: ${rawResponse.isMisinformation}")
            Log.d(TAG, "   severity: '${rawResponse.severity}'")
            Log.d(TAG, "   label: ${rawResponse.label}")
            Log.d(TAG, "   confidence: ${rawResponse.confidence}")
            Log.d(TAG, "   originalMessage: ${originalMessage.take(50)}...")
            
            // Post-processing: Ensure severity is only HIGH or NONE
            var finalSeverity = rawResponse.severity?.uppercase() ?: "NONE"
            
            // STRICT: Only allow HIGH or NONE (map everything else to NONE)
            if (finalSeverity !in listOf("HIGH", "NONE")) {
                Log.d(TAG, "   ⚠️ Invalid severity '$finalSeverity', defaulting to NONE")
                finalSeverity = "NONE"
            }
            
            // If not misinformation, severity MUST be NONE
            if (!rawResponse.isMisinformation) {
                finalSeverity = "NONE"
                Log.d(TAG, "   ✅ Not misinformation - forcing severity to NONE")
            }
            
            // If misinformation but severity is NONE, upgrade to HIGH (safety measure)
            if (rawResponse.isMisinformation && finalSeverity == "NONE") {
                Log.d(TAG, "   ⚠️ Misinformation with NONE severity - upgrading to HIGH")
                finalSeverity = "HIGH"
            }
            
            val finalResponse = rawResponse.copy(severity = finalSeverity, isHumor = false)
            Log.d(TAG, "📤 FINAL RESPONSE:")
            Log.d(TAG, "   severity: ${finalResponse.severity}")
            Log.d(TAG, "   isMisinformation: ${finalResponse.isMisinformation}")
            
            return finalResponse
            
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing JSON response", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error processing response", e)
            return null
        }
    }
}
