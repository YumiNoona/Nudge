package com.nudge.engine

import com.nudge.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
}
