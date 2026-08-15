package com.nudge.android.importer

import com.nudge.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class FinancialDocumentImporterTest {
    @Test
    fun parsesDebitAndCreditColumnsFromBankCsv() {
        val drafts = FinancialDocumentImporter.parseStatement(
            """Transaction Date,Narration,Withdrawal Amount,Deposit Amount,Balance
08/08/2026,DISTRICT DINING CYBS on 2026-08-08:19:54:52,725.15,,10000.00
09/08/2026,SALARY ACME,,40000.00,50000.00""",
        )

        assertEquals(2, drafts.size)
        assertEquals(TransactionType.DEBIT, drafts[0].type)
        assertEquals(72_515L, drafts[0].amountCents)
        assertEquals("District Dining", drafts[0].merchant)
        assertEquals(TransactionType.CREDIT, drafts[1].type)
        assertEquals(4_000_000L, drafts[1].amountCents)
    }

    @Test
    fun receiptPrefersGrandTotalOverLineItems() {
        val result = requireNotNull(
            FinancialDocumentImporter.parseReceipt(
                """DISTRICT DINING
Tax Invoice
Burger 450.00
Taxes 75.15
Grand Total Rs. 525.15""",
            ),
        )

        assertEquals(52_515L, result.amountCents)
        assertEquals("District Dining", result.merchant)
    }

    @Test
    fun creditCardStatementPaymentIsTransferNotIncome() {
        val draft = FinancialDocumentImporter.parseStatement(
            """Date,Narration,Debit,Credit
08/08/2026,PAYMENT RECEIVED - THANK YOU,,4000.00""",
        ).single()

        assertEquals(TransactionType.TRANSFER, draft.type)
        assertEquals("Credit Card Payment", draft.merchant)
    }

    @Test
    fun loosePdfRowUsesMovementBeforeRunningBalance() {
        val draft = FinancialDocumentImporter.parseStatement(
            "08-Aug-2026 DISTRICT DINING 725.15 10,000.00 DR",
        ).single()

        assertEquals(72_515L, draft.amountCents)
        assertEquals(TransactionType.DEBIT, draft.type)
    }

    @Test
    fun sbiOcrRowIgnoresRepeatedDateReferenceAndBalance() {
        val draft = FinancialDocumentImporter.parseStatement(
            "01/04/2026 01/04/2026 UPI/DR/645748193502/SANTOSH - 40.00 - 4,272.35",
        ).single()

        assertEquals(4_000L, draft.amountCents)
        assertEquals(TransactionType.DEBIT, draft.type)
        assertEquals("Santosh", draft.merchant)
        assertDate(draft.timestampEpoch, 2026, Calendar.APRIL, 1)
    }

    @Test
    fun sbiCreditMarkerCarriesIntoFollowingTransactionRow() {
        val draft = FinancialDocumentImporter.parseStatement(
            """DEP TFR
01 04 2026 01 04 2026 NEFT*UTIB0000104*AXISP00785771417*VK DESIGN AND P - - 26,413.00 41,371.58""",
        ).single()

        assertEquals(2_641_300L, draft.amountCents)
        assertEquals(TransactionType.CREDIT, draft.type)
        assertEquals("VK Design And P", draft.merchant)
        assertDate(draft.timestampEpoch, 2026, Calendar.APRIL, 1)
    }

    @Test
    fun statementRowsKeepTheirOwnMonths() {
        val drafts = FinancialDocumentImporter.parseStatement(
            """WDL TFR
01/04/2026 01/04/2026 UPI/DR/100000000001/APRIL SHOP - 100.00 - 9,900.00
WDL TFR
08/08/2026 08/08/2026 UPI/DR/100000000002/AUGUST SHOP - 200.00 - 9,700.00""",
        )

        assertEquals(2, drafts.size)
        assertDate(drafts[0].timestampEpoch, 2026, Calendar.APRIL, 1)
        assertDate(drafts[1].timestampEpoch, 2026, Calendar.AUGUST, 8)
    }

    @Test
    fun flexibleUpiCreditKeepsOnlyPersonName() {
        val draft = FinancialDocumentImporter.parseStatement(
            "01/04/2026 01/04/2026 DEP TFR UPVCR 116795757808 SHRADDHA 1,000.00 14,000.00",
        ).single()

        assertEquals(TransactionType.CREDIT, draft.type)
        assertEquals("Shraddha", draft.merchant)
    }

    @Test
    fun spacedUpiDirectionAndReferenceAreNotMerchantText() {
        val drafts = FinancialDocumentImporter.parseStatement(
            """DEP TFR
03/04/2026 03/04/2026 UP CR 120988551634 JASKARAN 100.00 10,000.00
WDL TFR
03/04/2026 03/04/2026 UP DR 609351234567 MOALAM 350.00 9,650.00""",
        )

        assertEquals("Jaskaran", drafts[0].merchant)
        assertEquals(TransactionType.CREDIT, drafts[0].type)
        assertEquals("Moalam", drafts[1].merchant)
        assertEquals(TransactionType.DEBIT, drafts[1].type)
    }

    @Test
    fun genericReferenceAndBankColumnsDoNotLeakIntoName() {
        val drafts = FinancialDocumentImporter.parseStatement(
            """DEP TFR
01/04/2026 01/04/2026 45514343031 7 MISS 13,000.00 17,272.35
WDL TFR
01/04/2026 01/04/2026 UPI/DR/645748193502/SANTOSH 40.00 4,272.35 YESB""",
        )

        assertEquals("Miss", drafts[0].merchant)
        assertEquals("Santosh", drafts[1].merchant)
    }

    @Test
    fun signedSingleAmountColumnKeepsItsDirection() {
        val drafts = FinancialDocumentImporter.parseStatement(
            """Date,Narration,Amount
01/08/2026,AMAZON,-499.00
02/08/2026,ACME SALARY,+45000.00""",
        )

        assertEquals(2, drafts.size)
        assertEquals(TransactionType.DEBIT, drafts[0].type)
        assertEquals(TransactionType.CREDIT, drafts[1].type)
    }

    @Test
    fun unsignedAmountWithoutDirectionIsNotGuessedAsExpense() {
        val drafts = FinancialDocumentImporter.parseStatement(
            """Date,Narration,Amount
01/08/2026,REFERENCE 123456,499.00""",
        )

        assertEquals(0, drafts.size)
    }

    private fun assertDate(epoch: Long, year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance().apply { timeInMillis = epoch }
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
    }
}
