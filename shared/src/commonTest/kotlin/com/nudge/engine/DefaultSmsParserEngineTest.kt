package com.nudge.engine

import com.nudge.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultSmsParserEngineTest {
    private val parser = DefaultSmsParserEngine()

    @Test
    fun parsesUpiExpense() {
        val result = assertNotNull(parser.parse("₹200.00 paid to SWIGGY via UPI", "GPAY"))
        assertEquals(20_000L, result.amount)
        assertEquals(TransactionType.DEBIT, result.type)
    }

    @Test
    fun refundWinsOverCreditedKeyword() {
        val result = assertNotNull(parser.parse("Refund of ₹200.00 credited to your account from SWIGGY", "HDFCBK"))
        assertEquals(20_000L, result.amount)
        assertEquals(TransactionType.REFUND, result.type)
    }

    @Test
    fun salaryIsIncome() {
        val result = assertNotNull(parser.parse("INR 45,000 credited to your account as salary", "HDFCBK"))
        assertEquals(TransactionType.CREDIT, result.type)
    }

    @Test
    fun removesDateAndSequenceFromUnityMerchant() {
        val result = parser.normalizeMerchant("UNITY SOFTWARE INC ON 2026-07-26:1")
        assertEquals("Unity Software Inc", result)
    }

    @Test
    fun removesReferenceMetadataFromMerchant() {
        val result = parser.normalizeMerchant("SWIGGY via UPI Ref 87ABC9921")
        assertEquals("Swiggy", result)
    }

    @Test
    fun bankAccountFragmentIsNotAMerchant() {
        val result = parser.normalizeMerchant("Using ICICI Bank Card XX4008 on 25-Jul-26")
        assertEquals("Unknown merchant", result)
    }

    @Test
    fun extractsMerchantAfterIciciCardDate() {
        val result = assertNotNull(
            parser.parse(
                "INR 1,003.99 spent using ICICI Bank Card XX4008 on 25-Jul-2026 on ANOMALY. Avl Limit: INR 20,000",
                "ICICIB",
            ),
        )

        assertEquals("Anomaly", result.merchantNormalized)
    }

    @Test
    fun ignoresIciciCreditCardStatementDueNotice() {
        val result = parser.parse(
            "ICICI Bank Credit Card XX4008 Statement is sent to ru**************01@gmail.com. " +
                "Total of Rs 77,336.90 or minimum of Rs 22,760.00 is due by 12-AUG-26.",
            "AX-ICICIT-S",
        )

        assertNull(result)
        assertTrue(parser.isWhitelisted("AX-ICICIT-S"))
    }

    @Test
    fun ignoresCreditCardStatementAndMinimumDueVariants() {
        assertNull(parser.parse("Your credit card statement is generated. Total amount due INR 18,200 by 05 Aug.", "HDFCBK"))
        assertNull(parser.parse("Credit card ending 4008 has minimum amount due Rs 2,000. Payment due by 12-AUG-26.", "ICICIT"))
    }

    @Test
    fun stillParsesRealIciciCardSpendAndCredit() {
        val spend = assertNotNull(
            parser.parse("INR 1,003.99 spent using ICICI Bank Card XX4008 on 25-Jul-2026 on SWIGGY.", "AX-ICICIT-S"),
        )
        val credit = assertNotNull(parser.parse("Rs 5,000 credited to your a/c XX4008", "AX-ICICIT-S"))

        assertEquals(TransactionType.DEBIT, spend.type)
        assertEquals("Swiggy", spend.merchantNormalized)
        assertEquals(TransactionType.CREDIT, credit.type)
    }
}
