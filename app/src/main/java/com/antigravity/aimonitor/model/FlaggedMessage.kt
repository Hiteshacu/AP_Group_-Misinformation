package com.antigravity.aimonitor.model

/**
 * Internal model for storing flagged messages in cache
 * @property messageText The original message text
 * @property factCheckResponse The fact-check result from API
 * @property timestamp When the message was flagged (milliseconds since epoch)
 */
data class FlaggedMessage(
    val messageText: String,
    val factCheckResponse: FactCheckResponse,
    val timestamp: Long = System.currentTimeMillis()
)
