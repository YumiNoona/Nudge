package com.nudge.engine

import com.nudge.model.*

/**
 * SMS/Notification parsing pipeline interface.
 * Platform-specific implementations handle the actual SMS reading
 * (Android: SmsManager + NotificationListenerService, Web: N/A).
 * The parsing logic itself runs on-device with no cloud dependency.
 */
interface SmsParserEngine {

    /**
     * Parse a raw SMS/notification body and extract transaction details.
     * Returns null if the message is not a recognizable transaction.
     */
    fun parse(rawText: String, senderId: String): ParsedTransaction?

    /**
     * Load parser rules for a given bank
     */
    fun getRulesForBank(bankName: String): List<ParserRule>

    /**
     * Register a new parser rule (user-created or from remote rule pack update)
     */
    fun registerRule(rule: ParserRule)

    /**
     * Check if a sender ID is in the whitelist
     */
    fun isWhitelisted(senderId: String): Boolean

    /**
     * Normalize a raw merchant string (e.g., "AMAZON PAY IN*ORDR8827" → "Amazon")
     */
    fun normalizeMerchant(rawMerchant: String): String

    /**
     * Add a merchant alias for normalization
     */
    fun addMerchantAlias(rawPattern: String, normalizedName: String, suggestedCategoryId: String?)

    /**
     * Fuzzy-match merchant name using Levenshtein/soundex-like matching
     */
    fun fuzzyMatchMerchant(raw: String, knownMerchants: List<String>): List<FuzzyMatchResult>
}

data class ParsedTransaction(
    val amount: Long, // cents
    val type: TransactionType,
    val merchantRaw: String,
    val merchantNormalized: String?,
    val confidenceScore: Float,
    val accountHint: String? = null,
    val timestampHint: String? = null, // extracted date string from message
    val matchedRuleId: String? = null
)

data class FuzzyMatchResult(
    val merchant: String,
    val score: Float, // 0.0 to 1.0 — higher = better match
    val categoryId: String?
)
