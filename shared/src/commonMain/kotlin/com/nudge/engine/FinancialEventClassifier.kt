package com.nudge.engine

import com.nudge.model.TransactionType

/**
 * Determines money direction from the sentence meaning, independently of bank templates.
 * Templates may locate an amount or merchant, but they must never override explicit direction.
 */
object FinancialEventClassifier {
    data class Result(
        val type: TransactionType,
        val confidence: Float,
        val evidence: String,
    )

    fun classify(text: String): Result? {
        val value = text.lowercase().replace(Regex("""\s+"""), " ").trim()
        if (value.isBlank() || TransactionMessageGuard.isNonTransaction(value)) return null

        // A card repayment moves money between the user's accounts. "Received" here is not income.
        if (isCreditCardRepayment(value)) return Result(TransactionType.TRANSFER, .99f, "credit-card repayment")

        // Reversals/refunds win over the word credited because they reverse an earlier expense.
        if (REFUND.containsMatchIn(value) && COMPLETED_REFUND.containsMatchIn(value)) {
            return Result(TransactionType.REFUND, .98f, "completed refund/reversal")
        }

        // Explicit bank and UPI direction is the strongest source of truth.
        if (DEBIT_EXPLICIT.containsMatchIn(value)) return Result(TransactionType.DEBIT, .99f, "explicit debit/spend")
        if (CREDIT_EXPLICIT.containsMatchIn(value)) return Result(TransactionType.CREDIT, .99f, "explicit credit/receipt")

        if (OUTGOING_PAYMENT.containsMatchIn(value)) return Result(TransactionType.DEBIT, .96f, "outgoing payment")
        if (INCOMING_PAYMENT.containsMatchIn(value)) return Result(TransactionType.CREDIT, .96f, "incoming payment")

        // Card purchase notifications often omit "debited" but still describe a completed charge.
        if (CARD_PURCHASE.containsMatchIn(value)) return Result(TransactionType.DEBIT, .94f, "card purchase/charge")
        if (CASH_WITHDRAWAL.containsMatchIn(value)) return Result(TransactionType.DEBIT, .98f, "cash withdrawal")

        // Transfers require a direction. A bare "transferred" is intentionally not guessed.
        if (TRANSFER_IN.containsMatchIn(value)) return Result(TransactionType.CREDIT, .94f, "incoming transfer")
        if (TRANSFER_OUT.containsMatchIn(value)) return Result(TransactionType.DEBIT, .94f, "outgoing transfer")

        // Wallet loads and cash additions are income only when the destination is the user's balance.
        if (WALLET_LOAD.containsMatchIn(value)) return Result(TransactionType.CREDIT, .91f, "wallet/account load")

        return null
    }

    fun isCreditCardRepayment(text: String): Boolean {
        val value = text.lowercase()
        val card = Regex("""\b(?:credit\s*card|card\s+(?:ending|xx|x{2,})|cc\s+(?:bill|payment)|card\s+bill)\b""")
            .containsMatchIn(value)
        val settlement = Regex("""\b(?:payment\s+(?:received|made)|received\s+(?:payment|towards)|paid\s+(?:towards|your)|bill\s+payment|payment\s+towards|autopay(?:ment)?\s+(?:successful|completed))\b""")
            .containsMatchIn(value)
        return card && settlement
    }

    private val REFUND = Regex("""\b(?:refund(?:ed)?|reversal|reversed|revoked transaction)\b""")
    private val COMPLETED_REFUND = Regex("""\b(?:credited|received|processed|completed|successful|reversed|refunded|has been|was)\b""")
    private val DEBIT_EXPLICIT = Regex(
        """\b(?:debited|debit(?:ed)?\s+from|spent|deducted|withdrawn|cut\s+from\s+(?:your|ur)\s+(?:a/?c|account|balance)|upi\s*[/|:-]?\s*dr\b|money\s+out|auto[- ]?debit(?:ed)?\s+(?:successful|completed))\b""",
    )
    private val CREDIT_EXPLICIT = Regex(
        """\b(?:credited\s+(?:to|in|into)\s+(?:your|ur|the)?\s*(?:a/?c|account|wallet|balance)|(?:a/?c|account)\s+.{0,20}credited\s+(?:with|by)|has\s+been\s+credited|deposited\s+(?:in|into|to)|upi\s*[/|:-]?\s*cr\b|money\s+in|salary\s+(?:is\s+)?credited)\b""",
    )
    private val OUTGOING_PAYMENT = Regex(
        """\b(?:you\s+(?:have\s+)?paid|paid\s+(?:to|at|via|using|from\s+your)|payment\s+(?:to|at)\b|sent\s+(?:to|via)|money\s+sent|purchase\s+(?:of|at|for)|payment\s+successful\s+(?:to|at))\b""",
    )
    private val INCOMING_PAYMENT = Regex(
        """\b(?:you\s+(?:have\s+)?received|received\s+(?:from|in|into|via)|money\s+received|payment\s+received\s+from|received\s+payment|cashback\s+(?:received|credited)|reward\s+(?:received|credited))\b""",
    )
    private val CARD_PURCHASE = Regex(
        """\b(?:charged(?:\s+(?:to|on))?|transaction\s+(?:of|for).{0,40}(?:card|at)|card.{0,30}(?:used|purchase|charged)|purchase.{0,30}(?:card|successful))\b""",
    )
    private val CASH_WITHDRAWAL = Regex("""\b(?:cash\s+withdrawal|atm\s+withdrawal|withdrawal\s+(?:of|from))\b""")
    private val TRANSFER_OUT = Regex("""\b(?:transferred|transfer)\s+(?:to|via)(?!\s+(?:your|ur)\s+(?:a/?c|account|wallet))|sent\s+to\b""")
    private val TRANSFER_IN = Regex("""\b(?:(?:transferred|transfer)\s+from|(?:transferred|transfer)\s+to\s+(?:your|ur)\s+(?:a/?c|account|wallet)|received\s+from)\b""")
    private val WALLET_LOAD = Regex(
        """\b(?:added|loaded|topped\s*up)\s+(?:to|into)\s+(?:your|ur)?\s*(?:[a-z][a-z0-9]*\s+)?(?:wallet|account|a/?c|balance)\b""",
    )
}
