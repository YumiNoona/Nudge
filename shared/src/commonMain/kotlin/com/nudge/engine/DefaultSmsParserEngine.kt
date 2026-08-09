package com.nudge.engine

import com.nudge.model.*
import kotlin.math.roundToLong

/**
 * Full SMS/Notification parsing pipeline.
 *
 * Pipeline flow:
 * 1. Sender filter — check whitelist
 * 2. Regex + template layer — match against bundled bank rules
 * 3. Fallback NLP/heuristic layer — for unmatched formats
 * 4. Merchant normalization — clean and normalize raw merchant
 * 5. Auto-categorization — if confidence high enough
 *
 * All processing is on-device. Nothing is uploaded.
 */
class DefaultSmsParserEngine : SmsParserEngine {

    private val rules = mutableListOf<ParserRule>()
    private val senderWhitelist = mutableSetOf<String>()
    private val merchantNormalizer = MerchantNormalizer
    private val categorizer = DefaultCategorizationEngine()

    init {
        // Load bundled rules
        for (rule in BundledRulePack.getRules()) {
            rules.add(rule)
        }
        for (sender in BundledRulePack.getSenderWhitelist()) {
            senderWhitelist.add(sender.senderId.uppercase())
        }
        merchantNormalizer.loadBundledAliases()
    }

    // --- Public API ---

    override fun parse(rawText: String, senderId: String): ParsedTransaction? {
        // Normalize the rupee glyph up front so templates work consistently
        // even when an OEM/SMS provider changes text encoding.
        val cleaned = rawText.trim().replace("\u20B9", "Rs.")
        if (cleaned.isEmpty()) return null
        if (TransactionMessageGuard.isNonTransaction(cleaned)) return null

        // Step 1: Identify the bank
        val bankName = identifyBank(senderId)

        // Step 2: Try regex patterns (bank-specific first, then generic)
        val result = tryRegexPatterns(cleaned, bankName)
        if (result != null) {
            // Step 4: Normalize merchant
            val semanticType = resolveTransactionType(cleaned, result.type)
            var normResult = merchantNormalizer.normalize(result.merchantRaw)
            if (normResult.normalized == "Unknown merchant") {
                val extractedMerchant = extractMerchant(cleaned)
                if (!extractedMerchant.equals("Unknown", ignoreCase = true)) {
                    normResult = merchantNormalizer.normalize(extractedMerchant)
                }
            }
            if (semanticType == TransactionType.TRANSFER && isCreditCardPayment(cleaned)) {
                normResult = merchantNormalizer.normalize(creditCardPaymentMerchant(cleaned, senderId, bankName))
            }
            return result.copy(
                type = semanticType,
                merchantNormalized = normResult.normalized.takeUnless { it.equals("EE", ignoreCase = true) } ?: "Unknown merchant",
                confidenceScore = (result.confidenceScore * 0.8f + normResult.confidence * 0.2f).coerceIn(0f, 1f)
            )
        }

        // Step 3: Fallback heuristic extraction
        val fallback = heuristicExtract(cleaned)
        if (fallback != null) {
            val semanticType = resolveTransactionType(cleaned, fallback.type)
            val merchant = if (semanticType == TransactionType.TRANSFER && isCreditCardPayment(cleaned)) {
                creditCardPaymentMerchant(cleaned, senderId, bankName)
            } else fallback.merchantRaw
            val normResult = merchantNormalizer.normalize(merchant)
            return fallback.copy(
                type = semanticType,
                merchantRaw = merchant,
                merchantNormalized = normResult.normalized.takeUnless { it.equals("EE", ignoreCase = true) } ?: "Unknown merchant",
                confidenceScore = (fallback.confidenceScore * 0.8f + normResult.confidence * 0.2f).coerceIn(0f, 1f)
            )
        }

        // Not a recognizable transaction
        return null
    }

    override fun getRulesForBank(bankName: String): List<ParserRule> {
        return rules.filter { it.bankName.equals(bankName, ignoreCase = true) }
    }

    override fun registerRule(rule: ParserRule) {
        // Remove any existing rule with same ID
        rules.removeAll { it.id == rule.id }
        rules.add(rule)
    }

    override fun isWhitelisted(senderId: String): Boolean {
        val upper = senderId.uppercase()
        if (senderWhitelist.any { upper.contains(it) }) return true
        // Also check against all rules' bank names as fallback
        return rules.any { upper.contains(it.bankName.uppercase()) }
    }

    override fun normalizeMerchant(rawMerchant: String): String {
        return merchantNormalizer.normalize(rawMerchant).normalized
    }

    override fun addMerchantAlias(rawPattern: String, normalizedName: String, suggestedCategoryId: String?) {
        merchantNormalizer.addAlias(rawPattern, normalizedName, suggestedCategoryId)
    }

    override fun fuzzyMatchMerchant(raw: String, knownMerchants: List<String>): List<FuzzyMatchResult> {
        val results = mutableListOf<FuzzyMatchResult>()
        for (merchant in knownMerchants) {
            val distance = merchantNormalizer.levenshteinDistance(
                raw.lowercase().trim(),
                merchant.lowercase().trim()
            )
            val maxLen = maxOf(raw.length, merchant.length).toDouble()
            val score = if (maxLen > 0) (1.0 - distance / maxLen).toFloat() else 0f
            if (score > 0.6f) {
                results.add(
                    FuzzyMatchResult(
                        merchant = merchant,
                        score = score,
                        categoryId = categorizer.autoCategorize(merchant, 0).categoryId
                    )
                )
            }
        }
        return results.sortedByDescending { it.score }
    }

    // Learn from user corrections
    fun learnCategorization(merchantNormalized: String, categoryId: String) {
        categorizer.learn(merchantNormalized, categoryId)
    }

    fun autoCategorize(merchantNormalized: String, amount: Long): CategorizationResult {
        return categorizer.autoCategorize(merchantNormalized, amount)
    }

    // --- Private helpers ---

    private fun identifyBank(senderId: String): String? {
        val upper = senderId.uppercase()
        for (sender in BundledRulePack.getSenderWhitelist()) {
            if (upper.contains(sender.senderId.uppercase())) {
                return sender.bankName
            }
        }
        return null
    }

    private fun tryRegexPatterns(text: String, bankName: String?): ParsedTransaction? {
        // First: bank-specific rules (sorted by specificity — verified first)
        val applicableRules = if (bankName != null) {
            rules.filter { it.bankName.equals(bankName, ignoreCase = true) && it.isVerified }.sortedByDescending { it.regexPattern.length }
        } else {
            emptyList()
        }

        // Try bank-specific rules
        for (rule in applicableRules) {
            val result = applyRule(text, rule)
            if (result != null) return result
        }

        // Try generic rules
        val genericRules = rules.filter {
            it.bankName.startsWith("Generic") && it.isVerified
        }.sortedByDescending { it.regexPattern.length }

        for (rule in genericRules) {
            val result = applyRule(text, rule)
            if (result != null) return result
        }

        // Try all remaining verified rules
        val remaining = rules.filter {
            it.isVerified && !applicableRules.contains(it) && !genericRules.contains(it)
        }.sortedByDescending { it.regexPattern.length }

        for (rule in remaining) {
            val result = applyRule(text, rule)
            if (result != null) return result
        }

        return null
    }

    private fun applyRule(text: String, rule: ParserRule): ParsedTransaction? {
        val regex = try {
            Regex(rule.regexPattern, RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            return null
        }

        val match = regex.find(text) ?: return null
        val groups = match.groupValues

        // Extract amount
        val amountStr = if (rule.fieldMapping.amountGroup < groups.size) {
            groups[rule.fieldMapping.amountGroup]
        } else null ?: return null

        val amount = parseAmount(amountStr)
        if (amount <= 0) return null

        // Extract merchant
        val merchant = if (rule.fieldMapping.merchantGroup > 0 && rule.fieldMapping.merchantGroup < groups.size) {
            groups[rule.fieldMapping.merchantGroup].trim()
        } else {
            extractMerchant(text)
        }

        // Determine transaction type
        val type = resolveTransactionType(text, rule.fieldMapping.transactionTypeHint)

        // Confidence: higher for bank-specific verified rules, lower for generic
        val confidence = when {
            rule.isVerified && !rule.bankName.startsWith("Generic") -> 0.85f
            rule.isVerified -> 0.70f
            else -> 0.50f
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchantRaw = merchant,
            merchantNormalized = null, // will be normalized in the pipeline
            confidenceScore = confidence,
            matchedRuleId = rule.id
        )
    }

    private fun heuristicExtract(text: String): ParsedTransaction? {
        // Try multiple strategies to find amount and merchant

        // Strategy 1: Find any currency amount
        val amountRegex = Regex("""(?:₹|Rs\.?|INR|\$|£|EUR|€)\s*([\d,]+\.?\d{0,2})""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(text)

        if (amountMatch == null) {
            // Try "X.XX" pattern near keywords
            val looseAmountRegex = Regex("""([\d,]+\.?\d{0,2})\s*(?:debited|credited|spent|paid|withdrawn|deposited)""", RegexOption.IGNORE_CASE)
            val looseMatch = looseAmountRegex.find(text)
            if (looseMatch == null) return null

            val amount = parseAmount(looseMatch.groupValues[1])
            if (amount <= 0) return null

            val type = inferTransactionType(text)
            val merchant = extractMerchant(text)

            return ParsedTransaction(
                amount = amount,
                type = type,
                merchantRaw = merchant,
                merchantNormalized = null,
                confidenceScore = 0.35f,
                matchedRuleId = null
            )
        }

        val amount = parseAmount(amountMatch.groupValues[1])
        if (amount <= 0) return null

        val type = inferTransactionType(text)
        val merchant = extractMerchant(text)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchantRaw = merchant,
            merchantNormalized = null,
            confidenceScore = 0.40f,
            matchedRuleId = null
        )
    }

    private fun parseAmount(raw: String): Long {
        val cleaned = raw.replace(",", "").replace(" ", "").trim()
        val doubleVal = cleaned.toDoubleOrNull() ?: return 0L
        // If the value already has decimals and looks like a dollar/rupee amount
        return if (cleaned.contains(".") && cleaned.split(".")[1].length <= 2) {
            // Has 0-2 decimal places — multiply by 100 to get cents
            (doubleVal * 100).roundToLong()
        } else {
            // No decimal or more than 2 decimal places — treat as whole units
            (doubleVal * 100).roundToLong()
        }
    }

    private fun extractMerchant(text: String): String {
        // Try to find merchant after common prepositions
        val patterns = listOf(
            Regex("""on\s+\d{1,2}-[A-Za-z]{3}-\d{2,4}\s+on\s+([A-Za-z0-9][A-Za-z0-9 &.'_-]{1,48})""", RegexOption.IGNORE_CASE),
            Regex("""(?:at|to|from|via|towards)\s+(\S+(?:\s+\S+){0,4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:for)\s+(\S+(?:\s+\S+){0,4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:Trf|Transfer|Payment)\s+(?:to|for)?\s*(\S+(?:\s+\S+){0,4})""", RegexOption.IGNORE_CASE),
        )

        for (p in patterns) {
            val match = p.find(text)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                // Filter out dates, ref numbers, amounts
                if (!extracted.matches(Regex("""\d{1,2}[-/]\d{1,2}[-/]\d{2,4}""")) &&
                    !extracted.matches(Regex("""[\d,]+\.?\d*""")) &&
                    !Regex("""[A-Za-z0-9._%+*\-]+@[A-Za-z0-9.*\-]+\.[A-Za-z]{2,}""").containsMatchIn(extracted) &&
                    !extracted.matches(Regex("""Ref|Txn#?\s*[\dA-Z]+""", RegexOption.IGNORE_CASE))
                ) {
                    return extracted.trim()
                }
            }
        }

        return "Unknown"
    }

    private fun inferTransactionType(text: String): TransactionType {
        val lowered = text.lowercase()
        return when {
            lowered.contains("refund") || lowered.contains("reversal") || lowered.contains("reversed") ->
                TransactionType.REFUND
            isCreditCardPayment(lowered) ->
                TransactionType.TRANSFER
            lowered.contains("transfer") || lowered.contains("transferred") ->
                TransactionType.TRANSFER
            lowered.contains("credited") || lowered.contains("deposited") ||
            lowered.contains("added") || lowered.contains("salary") ->
                TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }
    }

    /**
     * A bank rule is only a structural hint. Strong semantic phrases must win so a rule that sees
     * "payment received" cannot turn a credit-card repayment into income.
     */
    private fun resolveTransactionType(text: String, hint: TransactionType?): TransactionType {
        val lowered = text.lowercase()
        return when {
            lowered.contains("refund") || lowered.contains("reversal") || lowered.contains("reversed") ->
                TransactionType.REFUND
            isCreditCardPayment(lowered) -> TransactionType.TRANSFER
            else -> hint ?: inferTransactionType(lowered)
        }
    }

    private fun isCreditCardPayment(text: String): Boolean {
        val lowered = text.lowercase()
        val cardContext = lowered.contains("credit card") ||
            Regex("""\bcard\s+(?:ending|ending\s+in|xx|x{2,})""").containsMatchIn(lowered) ||
            lowered.contains("card bill")
        val settlementContext = lowered.contains("payment received") ||
            lowered.contains("received towards") ||
            lowered.contains("received for") ||
            lowered.contains("payment towards") ||
            lowered.contains("bill payment") ||
            lowered.contains("paid your") ||
            lowered.contains("paid towards")
        return cardContext && settlementContext
    }

    private fun creditCardPaymentMerchant(text: String, senderId: String, bankName: String?): String {
        val combined = "$senderId $bankName $text".uppercase()
        val bank = when {
            "HDFC" in combined -> "HDFC"
            "ICICI" in combined -> "ICICI"
            "SBI" in combined -> "SBI"
            "AXIS" in combined -> "Axis"
            "KOTAK" in combined -> "Kotak"
            "IDFC" in combined -> "IDFC"
            "INDUSIND" in combined -> "IndusInd"
            "YES BANK" in combined || "YESBNK" in combined -> "YES Bank"
            else -> null
        }
        return listOfNotNull(bank, "Credit Card Payment").joinToString(" ")
    }
}
