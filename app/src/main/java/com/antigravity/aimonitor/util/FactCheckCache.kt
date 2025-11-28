package com.antigravity.aimonitor.util

import com.antigravity.aimonitor.model.FlaggedMessage

/**
 * Thread-safe singleton cache for storing flagged messages
 * Messages are stored in memory and expire after 24 hours
 */
object FactCheckCache {
    private const val MAX_CACHE_SIZE = 100
    private const val EXPIRY_TIME_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    private val cache = mutableMapOf<String, FlaggedMessage>()
    private val lock = Any()
    
    /**
     * Add a flagged message to the cache
     * Automatically clears expired entries and enforces size limit
     * @param message The flagged message to store
     */
    fun addFlaggedMessage(message: FlaggedMessage) {
        synchronized(lock) {
            // Clear expired entries before adding
            clearExpired()
            
            // Enforce cache size limit
            if (cache.size >= MAX_CACHE_SIZE) {
                // Remove oldest entry
                val oldestKey = cache.entries.minByOrNull { it.value.timestamp }?.key
                oldestKey?.let { cache.remove(it) }
            }
            
            cache[message.messageText] = message
        }
    }
    
    /**
     * Check if a message text is flagged as misinformation
     * @param text The message text to check
     * @return True if the message is in the cache, false otherwise
     */
    fun isFlagged(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        
        synchronized(lock) {
            return cache.containsKey(text)
        }
    }
    
    /**
     * Retrieve a flagged message from the cache
     * @param text The message text to look up
     * @return The FlaggedMessage if found, null otherwise
     */
    fun getFlaggedMessage(text: String?): FlaggedMessage? {
        if (text.isNullOrBlank()) return null
        
        synchronized(lock) {
            return cache[text]
        }
    }
    
    /**
     * Remove messages older than 24 hours from the cache
     */
    fun clearExpired() {
        synchronized(lock) {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = cache.filter { (_, message) ->
                currentTime - message.timestamp > EXPIRY_TIME_MS
            }.keys
            
            expiredKeys.forEach { cache.remove(it) }
        }
    }
    
    /**
     * Clear all messages from the cache
     */
    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
    }

    /**
     * Retrieve all flagged message texts from the cache
     * @return A set of all flagged message texts
     */
    fun getFlaggedMessages(): Set<String> {
        synchronized(lock) {
            return cache.keys.toSet()
        }
    }
}
