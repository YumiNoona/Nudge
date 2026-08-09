package com.nudge.engine

import com.nudge.model.*
import kotlin.math.min

/**
 * On-device merchant name normalization engine.
 * Maps raw SMS strings like "AMAZON PAY IN*ORDR8827" → "Amazon"
 * using a bundled alias dictionary + fuzzy matching.
 *
 * Continuously improved from user corrections stored locally.
 * No cloud dependency — all matching happens on-device.
 */
object MerchantNormalizer {

    private val aliases = mutableMapOf<String, String>()
    private val categoryHints = mutableMapOf<String, String>()

    /**
     * Add a merchant alias mapping (raw pattern → normalized name)
     */
    fun addAlias(rawPattern: String, normalizedName: String, suggestedCategoryId: String? = null) {
        val pattern = rawPattern.lowercase().trim()
        aliases[pattern] = normalizedName
        if (suggestedCategoryId != null) {
            categoryHints[normalizedName] = suggestedCategoryId
        }
    }

    /**
     * Normalize a raw merchant string.
     * 1. Try exact alias match
     * 2. Try fuzzy match against known aliases
     * 3. Return cleaned version of original
     */
    fun normalize(raw: String): NormalizationResult {
        val cleaned = clean(raw)
        val lowered = cleaned.lowercase()
        if (cleaned == "Unknown merchant") {
            return NormalizationResult(
                normalized = cleaned,
                confidence = 0.15f,
                source = NormalizationSource.HEURISTIC,
            )
        }

        // 1. Exact alias match
        aliases[lowered]?.let { normalized ->
            return NormalizationResult(
                normalized = normalized,
                confidence = 1.0f,
                source = NormalizationSource.ALIAS_EXACT,
                suggestedCategoryId = categoryHints[normalized]
            )
        }

        // 1b. Partial match — check if any known alias is contained within the raw string
        for ((pattern, normalized) in aliases) {
            if (lowered.contains(pattern) || pattern.contains(lowered)) {
                return NormalizationResult(
                    normalized = normalized,
                    confidence = 0.85f,
                    source = NormalizationSource.ALIAS_PARTIAL,
                    suggestedCategoryId = categoryHints[normalized]
                )
            }
        }

        // 2. Fuzzy match
        val fuzzyResult = fuzzyMatch(lowered)
        if (fuzzyResult != null) {
            return fuzzyResult
        }

        // 3. Return cleaned original — apply common cleanup rules
        return NormalizationResult(
            normalized = applyHeuristics(cleaned),
            confidence = 0.3f,
            source = NormalizationSource.HEURISTIC
        )
    }

    /**
     * Clean raw merchant string — remove transaction IDs, ref numbers, etc.
     */
    private fun clean(raw: String): String {
        var result = raw.trim().trim('"', '\'', ' ')
        // Payment gateways and notification payload metadata are not part of a merchant name.
        // District messages, for example, may arrive as "District Dining CYBS on <timestamp>.Not...".
        result = result.replace(Regex("""(?i)\s+\bcybs\b.*$"""), "")
        result = result.replace(Regex("""(?i)\s+\bon\s+\d{4}-\d{1,2}-\d{1,2}(?::|T|\s).*?$"""), "")
        result = result.replace(Regex("""(?i)\.not(?:ification)?\b.*$"""), "")
        // Stop at transaction metadata accidentally captured after the merchant.
        result = result.replace(
            Regex("""(?i)\s+\b(?:on|at)\s+(?:\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}-[A-Za-z]{3}-\d{2,4})(?::\d+)?(?:\s.*)?$"""),
            ""
        )
        result = result.replace(
            Regex("""(?i)\s+\b(?:via\s+(?:upi|imps|neft|rtgs)|ref(?:erence)?|utr|txn(?:\s*id)?|transaction\s*id|a/c|acct|account|card\s+x{2,}|available\s+limit|avl\.?\s+limit|balance)\b.*$"""),
            ""
        )
        // Remove trailing reference numbers (common in bank SMS)
        result = result.replace(Regex("""\b(Ref|Txn|Order|Trf)#?\s*[\dA-Z]+""", RegexOption.IGNORE_CASE), "")
        // Remove "IN*" or "WWW*" prefixes common in credit card transactions
        result = result.replace(Regex("""^[A-Z]{2,4}\*"""), "")
        // Remove trailing dots, commas, colons
        result = result.replace(Regex("""[:#-]?\d+$"""), "")
        result = result.replace(Regex("""[.,:;|\-]+$"""), "").replace(Regex("""\s+"""), " ").trim()
        val noise = result.lowercase()
        if (result.length < 2 || noise.startsWith("using ") || noise in setOf(
                "unknown", "bank", "bank card", "credit card", "debit card", "upi", "payment", "purchase"
            ) || noise == "ee" || Regex("""your card ending\s+\d+\s+on""", RegexOption.IGNORE_CASE).matches(result)
        ) return "Unknown merchant"
        return result.take(64)
    }

    /**
     * Heuristic cleanup for unmatched merchants
     */
    private fun applyHeuristics(raw: String): String {
        // Capitalize first letter of each word
        if (raw == "Unknown merchant") return raw
        val words = raw.split(" ").map { word ->
            if (word.uppercase() in setOf("HDFC", "ICICI", "SBI", "IDFC", "UPI", "ATM")) {
                word.uppercase()
            } else if (word.length > 2 && word.all { it.isUpperCase() }) {
                word.lowercase().replaceFirstChar { it.titlecaseChar() }
            } else {
                word.replaceFirstChar { it.titlecaseChar() }
            }
        }
        return words.joinToString(" ")
    }

    /**
     * Levenshtein distance for fuzzy matching
     */
    fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[m][n]
    }

    /**
     * Fuzzy match against known aliases using Levenshtein distance
     */
    private fun fuzzyMatch(raw: String): NormalizationResult? {
        var bestMatch: String? = null
        var bestScore = 0.0
        val threshold = 0.65 // must be at least 65% similar

        for ((pattern, normalized) in aliases) {
            val maxLen = maxOf(raw.length, pattern.length).toDouble()
            if (maxLen == 0.0) continue
            val distance = levenshteinDistance(raw, pattern).toDouble()
            val similarity = 1.0 - (distance / maxLen)

            if (similarity > threshold && similarity > bestScore) {
                bestScore = similarity
                bestMatch = normalized
            }
        }

        return if (bestMatch != null) {
            NormalizationResult(
                normalized = bestMatch,
                confidence = bestScore.toFloat().coerceIn(0f, 1f),
                source = NormalizationSource.FUZZY_MATCH,
                suggestedCategoryId = categoryHints[bestMatch]
            )
        } else null
    }

    /**
     * Soundex-like simplified phonetic hash for additional matching
     */
    fun soundexHash(word: String): String {
        val upper = word.uppercase().filter { it.isLetter() }
        if (upper.isEmpty()) return "0000"

        val first = upper[0]
        val mapped = buildString {
            for (c in upper.drop(1)) {
                when (c) {
                    'B', 'F', 'P', 'V' -> append('1')
                    'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> append('2')
                    'D', 'T' -> append('3')
                    'L' -> append('4')
                    'M', 'N' -> append('5')
                    'R' -> append('6')
                    else -> {} // skip
                }
            }
        }
        // Remove consecutive duplicates
        val deduped = mapped.fold("") { acc, c ->
            if (acc.isNotEmpty() && acc.last() == c) acc else acc + c
        }

        val code = "$first${deduped}0000".take(4)
        return code
    }

    /**
     * Check if two words are phonetically similar
     */
    fun isPhoneticMatch(a: String, b: String): Boolean {
        return soundexHash(a) == soundexHash(b)
    }

    /**
     * Load bundled alias pack
     */
    fun loadBundledAliases() {
        // Major Indian merchants
        addAlias("AMAZON PAY IN", "Amazon", suggestedCategoryId = null)
        addAlias("amazon.in", "Amazon", suggestedCategoryId = null)
        addAlias("amazon", "Amazon", suggestedCategoryId = null)
        addAlias("FLIPKART", "Flipkart", suggestedCategoryId = null)
        addAlias("MYNTRA", "Myntra", suggestedCategoryId = null)
        addAlias("SWIGGY", "Swiggy", suggestedCategoryId = null)
        addAlias("ZOMATO", "Zomato", suggestedCategoryId = null)
        addAlias("UBER", "Uber", suggestedCategoryId = null)
        addAlias("OLA", "Ola", suggestedCategoryId = null)
        addAlias("BIGBASKET", "BigBasket", suggestedCategoryId = null)
        addAlias("BLINKIT", "Blinkit", suggestedCategoryId = null)
        addAlias("ZEPTO", "Zepto", suggestedCategoryId = null)
        addAlias("DMART", "DMart", suggestedCategoryId = null)
        addAlias("NETFLIX", "Netflix", suggestedCategoryId = null)
        addAlias("HOTSTAR", "Hotstar", suggestedCategoryId = null)
        addAlias("JIO", "Jio", suggestedCategoryId = null)
        addAlias("AIRTEL", "Airtel", suggestedCategoryId = null)
        addAlias("VI ", "Vodafone Idea", suggestedCategoryId = null)
        addAlias("TATASKY", "Tata Sky", suggestedCategoryId = null)
        addAlias("IRCTC", "IRCTC", suggestedCategoryId = null)
        addAlias("makemytrip", "MakeMyTrip", suggestedCategoryId = null)
        addAlias("GOIBIBO", "Goibibo", suggestedCategoryId = null)
        addAlias("OYO", "OYO", suggestedCategoryId = null)
        addAlias("LIC", "LIC", suggestedCategoryId = null)
        addAlias("ICICI PRU", "ICICI Prudential", suggestedCategoryId = null)
        addAlias("HDFC LIFE", "HDFC Life", suggestedCategoryId = null)
        addAlias("UPI-", "UPI Transfer", suggestedCategoryId = null)
        addAlias("NEFT-", "NEFT Transfer", suggestedCategoryId = null)
        addAlias("IMPS-", "IMPS Transfer", suggestedCategoryId = null)
        addAlias("RTGS-", "RTGS Transfer", suggestedCategoryId = null)
        addAlias("ATM WDL", "ATM Withdrawal", suggestedCategoryId = null)
        addAlias("CASH WDL", "Cash Withdrawal", suggestedCategoryId = null)

        // US merchants
        addAlias("AMAZON.COM", "Amazon", suggestedCategoryId = null)
        addAlias("WALMART", "Walmart", suggestedCategoryId = null)
        addAlias("TARGET", "Target", suggestedCategoryId = null)
        addAlias("COSTCO", "Costco", suggestedCategoryId = null)
        addAlias("WALGREENS", "Walgreens", suggestedCategoryId = null)
        addAlias("CVS", "CVS", suggestedCategoryId = null)
        addAlias("STARBUCKS", "Starbucks", suggestedCategoryId = null)
        addAlias("MCDONALD", "McDonald's", suggestedCategoryId = null)
        addAlias("MCD", "McDonald's", suggestedCategoryId = null)
        addAlias("CHIPOTLE", "Chipotle", suggestedCategoryId = null)
        addAlias("DOORDASH", "DoorDash", suggestedCategoryId = null)
        addAlias("UBER EATS", "Uber Eats", suggestedCategoryId = null)
        addAlias("LYFT", "Lyft", suggestedCategoryId = null)
        addAlias("SPOTIFY", "Spotify", suggestedCategoryId = null)
        addAlias("APPLE.COM/BILL", "Apple", suggestedCategoryId = null)
        addAlias("GOOGLE YOUTUBE", "YouTube", suggestedCategoryId = null)
        addAlias("GOOGLE PLAY", "Google Play", suggestedCategoryId = null)
        addAlias("PAYPAL", "PayPal", suggestedCategoryId = null)
        addAlias("VENMO", "Venmo", suggestedCategoryId = null)

        // UK merchants
        addAlias("TESCO", "Tesco", suggestedCategoryId = null)
        addAlias("SAINSBURY", "Sainsbury's", suggestedCategoryId = null)
        addAlias("ASDA", "Asda", suggestedCategoryId = null)
        addAlias("MORRISONS", "Morrisons", suggestedCategoryId = null)
        addAlias("ALDI", "Aldi", suggestedCategoryId = null)
        addAlias("LIDL", "Lidl", suggestedCategoryId = null)
        addAlias("BOOTS", "Boots", suggestedCategoryId = null)
        addAlias("PRET A MANGER", "Pret A Manger", suggestedCategoryId = null)
        addAlias("GREGGS", "Greggs", suggestedCategoryId = null)
        addAlias("TFL", "Transport for London", suggestedCategoryId = null)
        addAlias("OYSTER", "Oyster Card", suggestedCategoryId = null)
        addAlias("SKY ", "Sky TV", suggestedCategoryId = null)
        addAlias("BT GROUP", "BT", suggestedCategoryId = null)
        addAlias("VIRGIN MEDIA", "Virgin Media", suggestedCategoryId = null)
        addAlias("EE ", "EE", suggestedCategoryId = null)
        addAlias("VODAFONE", "Vodafone UK", suggestedCategoryId = null)
    }
}

data class NormalizationResult(
    val normalized: String,
    val confidence: Float, // 0.0 - 1.0
    val source: NormalizationSource,
    val suggestedCategoryId: String? = null
)

enum class NormalizationSource {
    ALIAS_EXACT,
    ALIAS_PARTIAL,
    FUZZY_MATCH,
    HEURISTIC
}
