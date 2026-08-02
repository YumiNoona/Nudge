package com.nudge.android.data

/** Stable, local merchant identity used by capture rules and review corrections. */
object CaptureLearning {
    const val REJECTED_SUGGESTION = "__nudge_rejected__"

    private val ignoredTokens = setOf(
        "the", "a", "an", "at", "on", "via", "payment", "purchase", "transaction",
        "private", "pvt", "limited", "ltd", "inc", "llp", "india", "online", "your",
        "successfully", "successful",
    )

    private val messageNoiseTokens = ignoredTokens + setOf(
        "and", "are", "but", "for", "from", "has", "have", "into", "not", "that", "this",
        "was", "were", "will", "with", "you", "inr", "rs", "rupees", "amount", "available",
        "balance", "avl", "bal", "reference", "number", "date", "time", "thank", "thanks",
    )

    fun canonicalMerchant(value: String): String = value
        .lowercase()
        .replace(Regex("""\b\d{4}[-/]\d{1,2}[-/]\d{1,2}\b.*$"""), " ")
        .replace(Regex("""\b(?:ref|utr|txn|order)\s*[a-z0-9-]+\b"""), " ")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
        .split(Regex("""\s+"""))
        .filter { it.length > 1 && it !in ignoredTokens }
        .take(7)
        .joinToString(" ")

    fun tolerantPattern(value: String): String {
        val tokens = canonicalMerchant(value).split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return "(?!)"
        return "(?<![a-z0-9])${tokens.joinToString(".*?") { Regex.escape(it) }}(?![a-z0-9])"
    }

    /**
     * Builds a durable rejection template without storing message content. Amounts,
     * dates, account digits and reference IDs are discarded; only stable words and
     * the sender identity are retained as regex lookaheads.
     */
    fun rejectionPattern(sender: String?, rawText: String?, merchant: String): String {
        val senderTokens = stableTokens(sender.orEmpty(), limit = 3)
        val messageTokens = stableTokens(rawText.orEmpty(), limit = 10)
        val merchantTokens = stableTokens(merchant, limit = 5)
        val tokens = (senderTokens + messageTokens + merchantTokens).distinct().take(12)
        if (tokens.size < 2) return tolerantPattern(merchant)
        return tokens.joinToString(prefix = "(?s)", separator = "") { token ->
            "(?=.*(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9]))"
        } + ".*"
    }

    fun learnedRuleId(merchant: String): String =
        "learned_${canonicalMerchant(merchant).hashCode().toString().replace('-', 'n')}"

    fun rejectedRuleId(sender: String?, merchant: String): String {
        val identity = "${canonicalMerchant(sender.orEmpty())}|${canonicalMerchant(merchant)}"
        return "rejected_${identity.hashCode().toString().replace('-', 'n')}"
    }

    fun isRejectedSuggestion(value: String?): Boolean = value == REJECTED_SUGGESTION

    fun similarity(left: String, right: String): Float {
        val a = canonicalMerchant(left)
        val b = canonicalMerchant(right)
        if (a.isBlank() || b.isBlank() || a == "unknown merchant" || b == "unknown merchant") return 0f
        if (a == b) return 1f
        if (minOf(a.length, b.length) >= 4 && (a.contains(b) || b.contains(a))) return .94f

        val aTokens = a.split(' ').toSet()
        val bTokens = b.split(' ').toSet()
        val intersection = aTokens.intersect(bTokens).size.toFloat()
        val dice = (2f * intersection) / (aTokens.size + bTokens.size).coerceAtLeast(1)
        if (dice >= .66f) return (.72f + dice * .24f).coerceAtMost(.96f)

        val maxLength = maxOf(a.length, b.length).coerceAtLeast(1)
        val editScore = 1f - levenshtein(a, b).toFloat() / maxLength
        return maxOf(dice, editScore)
    }

    fun sameMerchant(left: String, right: String): Boolean = similarity(left, right) >= .72f

    private fun stableTokens(value: String, limit: Int): List<String> = value
        .lowercase()
        .replace(Regex("""https?://\S+"""), " ")
        .replace(Regex("""\b\S*@\S*\.\S*\b"""), " ")
        .split(Regex("""[^a-z0-9]+"""))
        .asSequence()
        .filter { it.length >= 3 }
        .filterNot { token -> token.any(Char::isDigit) }
        .filterNot { it in messageNoiseTokens }
        .distinct()
        .take(limit)
        .toList()

    private fun levenshtein(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (left[i] == right[j]) 0 else 1,
                )
            }
            previous = current
        }
        return previous[right.length]
    }
}
