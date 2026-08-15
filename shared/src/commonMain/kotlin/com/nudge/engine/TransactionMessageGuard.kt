package com.nudge.engine

/**
 * Rejects financial messages that contain amounts but do not describe completed money movement.
 *
 * Statement totals and minimum-due reminders are especially dangerous: counting them would add
 * the same card purchases a second time. Keep this check before every regex/heuristic parser.
 */
object TransactionMessageGuard {
    fun isNonTransaction(text: String): Boolean {
        val normalized = text.lowercase().replace(Regex("""\s+"""), " ").trim()
        if (normalized.isBlank()) return false

        val cardContext = listOf(
            "credit card", "card statement", "cc statement", "card bill",
        ).any(normalized::contains)
        val statementContext = listOf(
            "statement is sent", "statement was sent", "statement has been sent",
            "statement generated", "statement is generated", "statement available",
            "statement is ready", "e-statement", "monthly statement",
        ).any(normalized::contains)
        val dueContext = listOf(
            "minimum amount due", "minimum due", "minimum of rs", "minimum of inr",
            "total amount due", "amount due", "payment due", "due by", "due date",
            "outstanding amount", "outstanding balance",
        ).any(normalized::contains)

        val authorizationOnly = listOf(
            "otp", "one time password", "verification code", "do not share",
            "collect request", "requesting money", "payment request", "mandate request",
            "autopay reminder", "scheduled for", "will be debited", "may be debited",
            "transaction declined", "transaction failed", "payment failed", "payment pending",
            "txn declined", "txn failed", "payment unsuccessful", "transaction pending",
            "transaction processing", "payment processing", "refund initiated", "transfer initiated",
            "beneficiary added", "payee added",
        ).any(normalized::contains)

        val nonCompletedStatus = Regex(
            """\b(?:payment|transaction|txn|transfer|refund|reversal|mandate|auto[- ]?debit|collect\s+request)\b.{0,70}\b(?:failed|declined|pending|unsuccessful|processing|initiated|scheduled|requested|will\s+be|may\s+be)\b""",
        ).containsMatchIn(normalized) || Regex(
            """\b(?:failed|declined|pending|unsuccessful|processing|initiated|scheduled)\b.{0,70}\b(?:payment|transaction|txn|transfer|refund|reversal|mandate|auto[- ]?debit)\b""",
        ).containsMatchIn(normalized)

        return authorizationOnly || nonCompletedStatus || (cardContext && statementContext) || (cardContext && dueContext) ||
            (statementContext && dueContext)
    }

    /** Matches the truncated merchant produced by older parser versions for statement emails. */
    fun isStatementExtractionArtifact(text: String): Boolean {
        val normalized = text.lowercase().replace(Regex("""\s+"""), " ").trim()
        val containsEmail = Regex("""[a-z0-9._%+*\-]+@[a-z0-9.*\-]+\.[a-z]{2,}""")
            .containsMatchIn(normalized)
        return containsEmail && (normalized.contains("total of rs") || normalized.contains("total of inr"))
    }
}
