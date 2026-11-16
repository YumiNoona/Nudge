package com.nudge.android.data

import org.json.JSONObject
import kotlin.math.abs

data class CaptureAssessment(
    val amount: Float,
    val direction: Float,
    val merchant: Float,
    val account: Float,
    val category: Float,
    val possibleDuplicateId: String? = null,
    val relatedExpenseId: String? = null,
) {
    val overall: Float get() = minOf(amount, direction, merchant, account).coerceIn(0f, 1f)
    val needsReview: Boolean get() = amount < .8f || direction < .9f || merchant < .75f || account < .85f || category < .75f || possibleDuplicateId != null
    val reasons: List<String> get() = buildList {
        if (amount < .8f) add("Check the amount")
        if (direction < .9f) add("Check money direction")
        if (merchant < .75f) add("Check the merchant")
        if (account < .85f) add("Confirm the account")
        if (category < .75f) add("Choose a category")
        if (possibleDuplicateId != null) add("Possible duplicate — compare before keeping")
    }
    fun toJson(): String = JSONObject().put("amount", amount).put("direction", direction)
        .put("merchant", merchant).put("account", account).put("category", category)
        .put("possibleDuplicateId", possibleDuplicateId).put("relatedExpenseId", relatedExpenseId).toString()

    companion object {
        fun fromJson(value: String?): CaptureAssessment? = value?.let { runCatching {
            val json = JSONObject(it)
            CaptureAssessment(json.getDouble("amount").toFloat(), json.getDouble("direction").toFloat(),
                json.getDouble("merchant").toFloat(), json.getDouble("account").toFloat(), json.getDouble("category").toFloat(),
                json.optString("possibleDuplicateId").takeIf { id -> id.isNotBlank() && id != "null" },
                json.optString("relatedExpenseId").takeIf { id -> id.isNotBlank() && id != "null" })
        }.getOrNull() }
    }
}

object CaptureAccountMatcher {
    data class Match(val account: AccountEntity?, val confidence: Float)
    fun resolve(text: String, accounts: List<AccountEntity>): Match {
        val active = accounts.filter { it.isActive && !it.isArchived }
        val suffixes = Regex("""(?i)(?:[x*•]{2,}[- ]*|(?:ending|ends?\s+with)\s*(?:in\s*)?|(?:a/?c|acct|account|card)\s*(?:no\.?\s*)?[:#-]?\s*)(\d{4})(?!\d)""")
            .findAll(text).map { it.groupValues[1] }.toSet()
        val suffixMatches = active.filter { it.last4Digits != null && it.last4Digits in suffixes }
        val bankMatches = active.filter { !it.bankName.isNullOrBlank() && text.contains(it.bankName!!, true) }
        val combined = suffixMatches.intersect(bankMatches.toSet())
        if (suffixes.size == 1 && combined.size == 1) return Match(combined.single(), .99f)
        if (suffixMatches.size == 1 && suffixes.size == 1 && bankMatches.isEmpty()) return Match(suffixMatches.single(), .95f)
        // A named bank alone cannot choose between several cards/accounts at that bank.
        if (suffixes.isEmpty() && bankMatches.size == 1) return Match(bankMatches.single(), .86f)
        return Match(active.firstOrNull { it.isDefault } ?: active.firstOrNull(), .25f)
    }
}

object CaptureDuplicateMatcher {
    enum class Decision { DISTINCT, DUPLICATE, POSSIBLE }
    data class Evidence(val accountId: String, val accountConfidence: Float, val merchant: String,
        val source: String, val timestamp: Long, val fingerprint: String? = null, val referenceHash: String? = null)
    fun family(source: String) = when {
        source.startsWith("sms") -> "sms"
        source.startsWith("mms") -> "mms"
        source == "notification" -> "notification"
        else -> source
    }
    /** Called only for candidates with equal amount, currency and direction. */
    fun compare(incoming: Evidence, previous: Evidence): Decision {
        if (incoming.fingerprint != null && incoming.fingerprint == previous.fingerprint) return Decision.DUPLICATE
        if (incoming.referenceHash != null && previous.referenceHash != null && incoming.referenceHash != previous.referenceHash) return Decision.DISTINCT
        if (incoming.accountId != previous.accountId) return Decision.DISTINCT
        val strongAccount = incoming.accountConfidence >= .85f && previous.accountConfidence >= .85f
        if (incoming.referenceHash != null && incoming.referenceHash == previous.referenceHash) {
            return if (strongAccount) Decision.DUPLICATE else Decision.POSSIBLE
        }
        val elapsed = abs(incoming.timestamp - previous.timestamp)
        val merchantMatch = CaptureLearning.similarity(incoming.merchant, previous.merchant) >= .94f
        val crossSource = family(incoming.source) != family(previous.source) &&
            family(incoming.source) in setOf("sms", "notification", "mms") && family(previous.source) in setOf("sms", "notification", "mms")
        // Without a payment reference these may be two genuine purchases, even across sources.
        if (elapsed <= 90_000 && merchantMatch && crossSource && strongAccount) return Decision.POSSIBLE
        if (elapsed <= 90_000 && merchantMatch) return Decision.POSSIBLE
        return Decision.DISTINCT
    }
}
