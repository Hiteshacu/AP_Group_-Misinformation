package com.antigravity.aimonitor.util

import android.util.Log

/**
 * Cache for tracking dismissed messages
 * Messages that user has dismissed should not show badges again
 */
object DismissedMessagesCache {
    private const val TAG = "DismissedMessagesCache"
    
    private val dismissedMessages = mutableSetOf<String>()
    private val lock = Any()
    
    /**
     * Mark a message as dismissed
     */
    fun dismissMessage(messageText: String) {
        synchronized(lock) {
            dismissedMessages.add(messageText)
            Log.d(TAG, "Message dismissed: ${messageText.take(50)}...")
            Log.d(TAG, "Total dismissed messages: ${dismissedMessages.size}")
        }
    }
    
    /**
     * Check if a message has been dismissed
     */
    fun isDismissed(messageText: String): Boolean {
        synchronized(lock) {
            return dismissedMessages.contains(messageText)
        }
    }
    
    /**
     * Clear all dismissed messages
     */
    fun clear() {
        synchronized(lock) {
            dismissedMessages.clear()
            Log.d(TAG, "Cleared all dismissed messages")
        }
    }
    
    /**
     * Get count of dismissed messages
     */
    fun getCount(): Int {
        synchronized(lock) {
            return dismissedMessages.size
        }
    }
}
