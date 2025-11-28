package com.antigravity.aimonitor.util

import android.util.Log

/**
 * Persistent cache to track ALL messages that have been processed
 * Prevents re-analyzing the same message multiple times
 * This is separate from FactCheckCache which only stores FLAGGED messages
 */
object ProcessedMessagesCache {
    private const val TAG = "ProcessedMsgCache"
    private const val MAX_CACHE_SIZE = 500 // Track more messages
    private const val EXPIRY_TIME_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    
    // Store message hash -> timestamp
    private val processedMessages = mutableMapOf<Int, Long>()
    private val lock = Any()
    
    /**
     * Mark a message as processed
     * @param messageText The message text that was analyzed
     */
    fun markAsProcessed(messageText: String) {
        synchronized(lock) {
            // Clear expired entries before adding
            clearExpired()
            
            // Enforce cache size limit
            if (processedMessages.size >= MAX_CACHE_SIZE) {
                // Remove oldest entry
                val oldestKey = processedMessages.entries.minByOrNull { it.value }?.key
                oldestKey?.let { processedMessages.remove(it) }
            }
            
            val hash = messageText.hashCode()
            processedMessages[hash] = System.currentTimeMillis()
            
            Log.v(TAG, "✅ Marked as processed: ${messageText.take(30)}... (hash: $hash)")
        }
    }
    
    /**
     * Check if a message has already been processed
     * @param messageText The message text to check
     * @return True if already processed, false if new
     */
    fun isProcessed(messageText: String): Boolean {
        synchronized(lock) {
            val hash = messageText.hashCode()
            val exists = processedMessages.containsKey(hash)
            
            if (exists) {
                Log.v(TAG, "⏭️ Already processed: ${messageText.take(30)}...")
            }
            
            return exists
        }
    }
    
    /**
     * Remove messages older than 7 days from the cache
     */
    private fun clearExpired() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = processedMessages.filter { (_, timestamp) ->
            currentTime - timestamp > EXPIRY_TIME_MS
        }.keys
        
        if (expiredKeys.isNotEmpty()) {
            expiredKeys.forEach { processedMessages.remove(it) }
            Log.d(TAG, "🧹 Cleared ${expiredKeys.size} expired entries")
        }
    }
    
    /**
     * Clear all processed messages from the cache
     * Use with caution - will cause all messages to be re-analyzed
     */
    fun clear() {
        synchronized(lock) {
            val count = processedMessages.size
            processedMessages.clear()
            Log.d(TAG, "🗑️ Cleared all $count processed messages")
        }
    }
    
    /**
     * Get statistics about the cache
     */
    fun getStats(): String {
        synchronized(lock) {
            return "Processed messages: ${processedMessages.size}/$MAX_CACHE_SIZE"
        }
    }
}
