package com.nudge.engine

/**
 * Cheap first-pass filter used before scheduling background parsing.
 *
 * This intentionally does not depend on sender IDs or app package names: banks and UPI apps
 * frequently change those identifiers. The strict semantic classifier remains the authority
 * before anything is written to the database.
 */
object FinancialMessageCandidate {
    fun looksLikeCompletedMovement(text: String): Boolean {
        val value = text.lowercase().replace(Regex("""\s+"""), " ").trim()
        if (value.isBlank() || TransactionMessageGuard.isNonTransaction(value)) return false
        val hasAmount = CURRENCY_AMOUNT.containsMatchIn(value) ||
            (PLAIN_AMOUNT.containsMatchIn(value) && MOVEMENT.containsMatchIn(value))
        return hasAmount && MOVEMENT.containsMatchIn(value)
    }

    private val CURRENCY_AMOUNT = Regex(
        """(?:₹|rs\.?|inr|usd|\$|eur|€|gbp|£)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?""",
        RegexOption.IGNORE_CASE,
    )
    private val PLAIN_AMOUNT = Regex("""\b[0-9][0-9,]*(?:\.[0-9]{1,2})?\b""")
    private val MOVEMENT = Regex(
        """\b(?:debit(?:ed)?|credit(?:ed)?|spent|paid|payment|purchase|charged|deducted|withdrawn|received|sent|transferred|transfer|refund(?:ed)?|reversal|reversed|deposited|cashback|money\s+(?:in|out)|upi\s*[/|:-]?\s*(?:dr|cr)|auto[- ]?debit(?:ed)?|added|loaded|top(?:ped)?\s*up)\b""",
        RegexOption.IGNORE_CASE,
    )
}
