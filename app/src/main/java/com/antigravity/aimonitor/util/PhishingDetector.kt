package com.antigravity.aimonitor.util

import android.util.Log
import java.net.URL

/**
 * Local phishing URL detector
 * Detects suspicious URLs without API calls (instant detection)
 */
object PhishingDetector {
    
    private const val TAG = "PhishingDetector"
    
    // Known legitimate domains (whitelist)
    private val trustedDomains = setOf(
        "google.com", "youtube.com", "facebook.com", "twitter.com", "x.com",
        "instagram.com", "linkedin.com", "github.com", "stackoverflow.com",
        "wikipedia.org", "amazon.com", "apple.com", "microsoft.com",
        "telegram.org", "whatsapp.com", "reddit.com", "netflix.com"
    )
    
    // Suspicious TLDs (top-level domains)
    private val suspiciousTLDs = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", // Free domains often used for phishing
        ".xyz", ".top", ".work", ".click", ".link",
        ".pw", ".cc", ".ws", ".info", ".biz"
    )
    
    // Suspicious keywords in URLs
    private val suspiciousKeywords = listOf(
        "login", "signin", "verify", "account", "secure", "update",
        "confirm", "banking", "paypal", "wallet", "crypto", "bitcoin",
        "password", "suspended", "locked", "urgent", "action-required",
        "prize", "winner", "claim", "free", "gift", "reward"
    )
    
    // Common typosquatting patterns
    private val typosquattingPatterns = mapOf(
        "google" to listOf("g00gle", "gooogle", "googl3", "gogle"),
        "facebook" to listOf("faceb00k", "facebok", "faceboook", "fecebook"),
        "paypal" to listOf("paypai", "paypa1", "paypall", "paypa"),
        "amazon" to listOf("amaz0n", "amazom", "arnazon", "amazan"),
        "apple" to listOf("app1e", "appl3", "appie", "aple"),
        "microsoft" to listOf("micros0ft", "microsft", "microsfot", "micorsoft"),
        "instagram" to listOf("instgram", "instagran", "instagrarn", "instagramm"),
        "whatsapp" to listOf("whatsap", "whatsaap", "whatapp", "whatsup")
    )
    
    /**
     * Analyze a URL for phishing indicators
     * Returns PhishingResult with risk level and reasons
     */
    fun analyzeUrl(url: String): PhishingResult {
        try {
            val urlObj = URL(url)
            val domain = urlObj.host.lowercase()
            val path = urlObj.path.lowercase()
            val fullUrl = url.lowercase()
            
            val risks = mutableListOf<String>()
            var riskScore = 0
            
            // Check 1: Trusted domain (whitelist)
            if (isTrustedDomain(domain)) {
                return PhishingResult(
                    isPhishing = false,
                    riskLevel = RiskLevel.SAFE,
                    confidence = 0.95,
                    reasons = listOf("Trusted domain: $domain")
                )
            }
            
            // Check 2: Suspicious TLD
            if (hasSuspiciousTLD(domain)) {
                risks.add("Suspicious domain extension (${getTLD(domain)})")
                riskScore += 30
            }
            
            // Check 3: IP address instead of domain
            if (isIPAddress(domain)) {
                risks.add("Uses IP address instead of domain name")
                riskScore += 40
            }
            
            // Check 4: Excessive subdomains
            if (hasExcessiveSubdomains(domain)) {
                risks.add("Suspicious number of subdomains")
                riskScore += 20
            }
            
            // Check 5: Typosquatting
            val typosquatting = detectTyposquatting(domain)
            if (typosquatting != null) {
                risks.add("Possible typosquatting of: $typosquatting")
                riskScore += 50
            }
            
            // Check 6: Suspicious keywords
            val suspiciousWords = detectSuspiciousKeywords(fullUrl)
            if (suspiciousWords.isNotEmpty()) {
                risks.add("Contains suspicious keywords: ${suspiciousWords.joinToString(", ")}")
                riskScore += 15 * suspiciousWords.size
            }
            
            // Check 7: URL shortener
            if (isUrlShortener(domain)) {
                risks.add("URL shortener (hides real destination)")
                riskScore += 25
            }
            
            // Check 8: Homograph attack (Unicode lookalikes)
            if (hasHomographAttack(domain)) {
                risks.add("Contains lookalike characters (homograph attack)")
                riskScore += 60
            }
            
            // Check 9: Excessive hyphens or numbers
            if (hasExcessiveHyphensOrNumbers(domain)) {
                risks.add("Unusual domain pattern (many hyphens/numbers)")
                riskScore += 15
            }
            
            // Check 10: Long domain name
            if (domain.length > 50) {
                risks.add("Unusually long domain name")
                riskScore += 10
            }
            
            // Determine risk level
            val riskLevel = when {
                riskScore >= 70 -> RiskLevel.HIGH
                riskScore >= 40 -> RiskLevel.MEDIUM
                riskScore >= 20 -> RiskLevel.LOW
                else -> RiskLevel.SAFE
            }
            
            val isPhishing = riskScore >= 40
            val confidence = minOf(riskScore / 100.0, 0.95)
            
            Log.d(TAG, "URL Analysis: $url")
            Log.d(TAG, "Risk Score: $riskScore")
            Log.d(TAG, "Risk Level: $riskLevel")
            Log.d(TAG, "Reasons: ${risks.joinToString("; ")}")
            
            return PhishingResult(
                isPhishing = isPhishing,
                riskLevel = riskLevel,
                confidence = confidence,
                reasons = risks
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing URL: ${e.message}", e)
            return PhishingResult(
                isPhishing = false,
                riskLevel = RiskLevel.UNKNOWN,
                confidence = 0.0,
                reasons = listOf("Unable to analyze URL")
            )
        }
    }
    
    /**
     * Check if domain is in trusted whitelist
     */
    private fun isTrustedDomain(domain: String): Boolean {
        return trustedDomains.any { trusted ->
            domain == trusted || domain.endsWith(".$trusted")
        }
    }
    
    /**
     * Check for suspicious TLD
     */
    private fun hasSuspiciousTLD(domain: String): Boolean {
        return suspiciousTLDs.any { domain.endsWith(it) }
    }
    
    /**
     * Get TLD from domain
     */
    private fun getTLD(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) ".${parts.last()}" else ""
    }
    
    /**
     * Check if domain is an IP address
     */
    private fun isIPAddress(domain: String): Boolean {
        return domain.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
    }
    
    /**
     * Check for excessive subdomains
     */
    private fun hasExcessiveSubdomains(domain: String): Boolean {
        val parts = domain.split(".")
        return parts.size > 4 // More than 4 parts is suspicious
    }
    
    /**
     * Detect typosquatting attempts
     */
    private fun detectTyposquatting(domain: String): String? {
        for ((legitimate, typos) in typosquattingPatterns) {
            if (typos.any { domain.contains(it) }) {
                return legitimate
            }
        }
        return null
    }
    
    /**
     * Detect suspicious keywords in URL
     */
    private fun detectSuspiciousKeywords(url: String): List<String> {
        return suspiciousKeywords.filter { url.contains(it) }
    }
    
    /**
     * Check if domain is a URL shortener
     */
    private fun isUrlShortener(domain: String): Boolean {
        val shorteners = setOf(
            "bit.ly", "tinyurl.com", "goo.gl", "ow.ly", "t.co",
            "is.gd", "buff.ly", "adf.ly", "short.link"
        )
        return shorteners.any { domain.contains(it) }
    }
    
    /**
     * Detect homograph attacks (Unicode lookalikes)
     */
    private fun hasHomographAttack(domain: String): Boolean {
        // Check for non-ASCII characters that look like ASCII
        return domain.any { it.code > 127 }
    }
    
    /**
     * Check for excessive hyphens or numbers
     */
    private fun hasExcessiveHyphensOrNumbers(domain: String): Boolean {
        val hyphens = domain.count { it == '-' }
        val numbers = domain.count { it.isDigit() }
        return hyphens > 2 || numbers > 3
    }
}

/**
 * Result of phishing analysis
 */
data class PhishingResult(
    val isPhishing: Boolean,
    val riskLevel: RiskLevel,
    val confidence: Double,
    val reasons: List<String>
)

/**
 * Risk levels for URLs
 */
enum class RiskLevel {
    SAFE,       // Trusted or low risk
    LOW,        // Minor concerns
    MEDIUM,     // Moderate risk
    HIGH,       // High risk - likely phishing
    UNKNOWN     // Unable to determine
}
