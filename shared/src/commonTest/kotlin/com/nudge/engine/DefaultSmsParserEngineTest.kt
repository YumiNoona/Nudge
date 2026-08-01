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
}
