package com.antigravity.aimonitor.model

/**
 * A class to hold the results of a steganography scan.
 */
class DetectionResult(
    private val positive: Boolean,
    private val method: String?,
    private val message: String
) {
    
    /**
     * @return True if steganography was detected, false otherwise.
     */
    fun isPositive(): Boolean = positive
    
    /**
     * @return The name of the detection method that yielded a positive result.
     */
    fun getMethod(): String? = method
    
    /**
     * @return A descriptive message about the detection result.
     */
    fun getMessage(): String = message
    
    companion object {
        /**
         * Creates a negative result object.
         */
        fun createNegativeResult(): DetectionResult {
            return DetectionResult(false, null, "No steganography detected.")
        }
    }
}
