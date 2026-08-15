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

    @Test
    fun creditCardRepaymentIsATransferNotIncome() {
        val result = assertNotNull(
            parser.parse(
                "Payment of Rs.4,000 received towards your HDFC Bank Credit Card ending 9529 on 07-Aug-2026.",
                "JD-HDFCBK-S",
            ),
        )

        assertEquals(400_000L, result.amount)
        assertEquals(TransactionType.TRANSFER, result.type)
        assertEquals("HDFC Credit Card Payment", result.merchantNormalized)
    }

    @Test
    fun cardSpendRemainsAnExpenseEvenThoughMessageSaysCreditCard() {
        val result = assertNotNull(
            parser.parse(
                "Rs.360 spent on your HDFC Bank Credit Card ending 9529 at EE on 08-Aug-2026.",
                "JD-HDFCBK-S",
            ),
        )

        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Unknown merchant", result.merchantNormalized)
    }

    @Test
    fun removesDistrictGatewayAndTimestampNoise() {
        assertEquals(
            "District Dining",
            parser.normalizeMerchant("District Dining CYBS on 2026-08-08:19:54:52.Not"),
        )
    }

    @Test
    fun districtGatewayMessageKeepsOnlyTheCustomerFacingMerchant() {
        val result = assertNotNull(
            parser.parse(
                "Rs.725.15 spent at District Dining CYBS on 2026-08-08:19:54:52.Notification from HDFC Bank.",
                "JD-HDFCBK-S",
            ),
        )
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("District Dining", result.merchantNormalized)
    }

    @Test
    fun classifiesIndianBankAndUpiDirectionCorpus() {
        val expenses = listOf(
            "Rs 499 debited from your A/c XX1234 for UPI transaction to ZOMATO",
            "Your a/c 1234 is debited by INR 1,250.00 on 13-Aug-26 at AMAZON",
            "You paid ₹360 to Rahul using Google Pay",
            "₹725 paid via UPI to District Dining",
            "Money sent: Rs.2,000 to Priya from account XX7788",
            "INR 899 spent on ICICI Bank Credit Card XX4008 at MYNTRA",
            "Your card ending 9529 was charged Rs 4,000 at APPLE STORE",
            "Cash withdrawal of INR 5,000 from ATM using card XX1234",
            "UPI/DR/609351234567/MOALAM Rs.350 debited from a/c XX1234",
            "Purchase of Rs 90 at VODAFONE IDEA was successful on your card",
        )
        val income = listOf(
            "Rs 10,000 credited to your a/c XX1234 from ACME PAYROLL",
            "INR 500 deposited into your account XX1234",
            "You received ₹1,000 from Shraddha via UPI",
            "Payment received from Jaskaran: Rs 100",
            "UPI/CR/120988551634/JASKARAN Rs.100 credited to your a/c",
            "Salary credited to your account: INR 45,000",
            "Rs 250 added to your Paytm wallet",
        )
        expenses.forEach { message ->
            assertEquals(TransactionType.DEBIT, assertNotNull(parser.parse(message, "HDFCBK"), message).type, message)
        }
        income.forEach { message ->
            assertEquals(TransactionType.CREDIT, assertNotNull(parser.parse(message, "HDFCBK"), message).type, message)
        }
    }

    @Test
    fun classifiesRefundsAndCardRepaymentsBeforeGenericCreditWords() {
        val refunds = listOf(
            "Refund of Rs 744 from SWIGGY has been credited to your account",
            "Transaction of INR 1,200 at AMAZON was reversed and credited to card XX4008",
            "Rs 99 refunded by UBER to your a/c XX1234",
        )
        refunds.forEach { message ->
            assertEquals(TransactionType.REFUND, assertNotNull(parser.parse(message, "HDFCBK"), message).type, message)
        }

        val repayments = listOf(
            "Payment of Rs.4,000 received towards your HDFC Bank Credit Card ending 9529",
            "Thank you. We received payment of INR 8,000 towards your ICICI credit card XX4008",
            "Credit card bill payment of Rs 2,500 completed for card ending 1234",
            "You paid your Axis Bank credit card bill of INR 6,000",
        )
        repayments.forEach { message ->
            assertEquals(TransactionType.TRANSFER, assertNotNull(parser.parse(message, "HDFCBK"), message).type, message)
        }
    }

    @Test
    fun rejectsAmbiguousIncompleteAndNonTransactionMoneyMessages() {
        val ignored = listOf(
            "Rs 5,000 available in your account",
            "Your balance is INR 12,300",
            "You can save Rs 500 on your next purchase",
            "Payment request of Rs 900 from Rahul",
            "Rs 1,200 will be debited tomorrow for your mandate",
            "Transaction of Rs 700 is pending",
            "Payment of INR 200 failed",
            "OTP 123456 for transaction of Rs 3,000. Do not share",
            "Your beneficiary Priya was added successfully",
            "Rs 300 transferred successfully",
            "Received your credit card statement. Amount due Rs 77,336",
            "Payment unsuccessful for Rs 500 at AMAZON",
            "Refund initiated for Rs 799 from MYNTRA",
            "Dr Rahul sent you a request for Rs 300",
        )
        ignored.forEach { message -> assertNull(parser.parse(message, "HDFCBK"), message) }
    }

    @Test
    fun understandsAdditionalCompletedMovementLanguageWithoutGuessing() {
        val debit = assertNotNull(parser.parse("Rs 650 cut from your account for electricity bill", "HDFCBK"))
        val autoDebit = assertNotNull(parser.parse("Auto-debit successful for INR 1,499 to NETFLIX", "HDFCBK"))
        val incomingTransfer = assertNotNull(parser.parse("Rs 2,500 transferred to your account from Priya", "HDFCBK"))

        assertEquals(TransactionType.DEBIT, debit.type)
        assertEquals(TransactionType.DEBIT, autoDebit.type)
        assertEquals(TransactionType.CREDIT, incomingTransfer.type)
    }

    @Test
    fun fallbackChoosesMovementAmountInsteadOfBalanceOrLimit() {
        val debit = assertNotNull(
            parser.parse("INR 360 deducted from your account for EE. Available balance INR 15,240.80", "NEWBANK"),
        )
        val credit = assertNotNull(
            parser.parse("You received Rs.1,250 from Shraddha via UPI. Balance Rs.42,890.50", "UNLISTED-UPI"),
        )

        assertEquals(36_000L, debit.amount)
        assertEquals(TransactionType.DEBIT, debit.type)
        assertEquals(125_000L, credit.amount)
        assertEquals(TransactionType.CREDIT, credit.type)
    }

    @Test
    fun lightweightCandidateAcceptsUnknownBanksAndRejectsNoise() {
        assertTrue(FinancialMessageCandidate.looksLikeCompletedMovement("A/c XX7788 debited by 499.00 at ZOMATO"))
        assertTrue(FinancialMessageCandidate.looksLikeCompletedMovement("₹726 paid to District Dining"))
        assertTrue(FinancialMessageCandidate.looksLikeCompletedMovement("Rs 1,000 credited to your account"))
        assertEquals(false, FinancialMessageCandidate.looksLikeCompletedMovement("Your balance is Rs 40,000"))
        assertEquals(false, FinancialMessageCandidate.looksLikeCompletedMovement("OTP 123456 for payment of Rs 500"))
        assertEquals(false, FinancialMessageCandidate.looksLikeCompletedMovement("Payment of Rs 900 failed"))
    }
}
