package com.nudge.android.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptIntelligenceTest {
    @Test
    fun stitchesLongReceiptPagesWithoutDuplicatingOverlap() {
        val stitched = ReceiptIntelligence.stitchPages(
            listOf(
                """DMART
LAYS CLASSIC 2 x 20.00 40.00
KURKURE MASALA 1 x 20.00 20.00
MILK 1L 2 x 62.00 124.00""",
                """KURKURE MASALA 1 x 20.00 20.00
MILK 1L 2 x 62.00 124.00
RICE 5KG 1 x 320.00 320.00
SUB TOTAL 504.00
CGST 4.00
SGST 4.00
GRAND TOTAL 512.00""",
            ),
        )

        assertEquals(1, Regex("KURKURE MASALA").findAll(stitched).count())
        assertEquals(1, Regex("MILK 1L").findAll(stitched).count())
        assertTrue(stitched.contains("GRAND TOTAL 512.00"))
    }

    @Test
    fun parsesDmartItemsQuantitiesGstAndTotal() {
        val receipt = requireNotNull(
            ReceiptIntelligence.parse(
                listOf(
                    """DMART READY
GSTIN 27AAAAA0000A1Z5
13/08/2026
LAYS CLASSIC 2 x 20.00 40.00
KURKURE MASALA 1 x 20.00 20.00
MILK 1L 2 x 62.00 124.00
SUB TOTAL 184.00
DISCOUNT 10.00
CGST 4.00
SGST 4.00
GRAND TOTAL 182.00""",
                ),
            ),
        )

        assertEquals("Dmart Ready", receipt.merchant)
        assertEquals(18_200L, receipt.printedTotalCents)
        assertEquals(18_400L, receipt.subtotalCents)
        assertEquals(1_000L, receipt.discountCents)
        assertEquals(800L, receipt.taxCents)
        assertEquals(3, receipt.items.size)
        assertEquals(2.0, receipt.items.first().quantity, .001)
        assertEquals(4_000L, receipt.items.first().lineTotalCents)
        assertEquals(0L, receipt.mismatchCents)
    }

    @Test
    fun allocatesPrintedTotalAcrossItemsWithoutLosingRounding() {
        val items = listOf(
            ReceiptLineDraft("A", lineTotalCents = 3_333),
            ReceiptLineDraft("B", lineTotalCents = 3_333),
            ReceiptLineDraft("C", lineTotalCents = 3_334),
        )
        val allocated = ReceiptIntelligence.allocateTotal(items, 10_007)

        assertEquals(10_007L, allocated.sum())
        assertEquals(3, allocated.size)
    }

    @Test
    fun missingPrintedTotalDoesNotInventReceipt() {
        assertEquals(null, ReceiptIntelligence.parse(listOf("DMART\nLAYS CLASSIC\nThank you")))
    }

    @Test
    fun ignoresIndianInvoiceMetadataTaxRatesAndSavingsRows() {
        val receipt = requireNotNull(
            ReceiptIntelligence.parse(
                listOf(
                    """D'MART
TAX INVOICE
Fssay N 4.00
Phane OT 1001.00
HORLICKS MILK PLUS BR 1 30.00 30.00
PARLE KRACHJ 2 x 17.00 34.00
2) Cest 2.50%, Sst 2.50%
ITEM TOTAL 1330.00
** Sayed 350.00
CGST 2.50% 6.50
SGST 2.50% 6.50
GRAND TOTAL 993.00""",
                ),
            ),
        )

        assertEquals(99_300L, receipt.printedTotalCents)
        assertEquals(133_000L, receipt.subtotalCents)
        assertEquals(0L, receipt.discountCents)
        assertEquals(35_000L, receipt.savingsCents)
        assertEquals(1_300L, receipt.taxCents)
        assertEquals(2, receipt.items.size)
        assertTrue(receipt.items.none { it.name.contains("GST", true) || it.name.contains("Phone", true) || it.name.contains("FSSAI", true) })
        assertEquals(-35_000L, receipt.mismatchCents)
    }

    @Test
    fun dmartGstReceiptTreatsValueAndPrintedTotalAsAuthoritative() {
        val receipt = requireNotNull(
            ReceiptIntelligence.parse(
                listOf(
                    """D'MART
AVENUE SUPERMARTS LTD
GSTIN : 36AACCA8432H1ZR
FSSAI No : 13620014000372
TAX INVOICE
HSN Particulars Qty/Kg N/Rate Value
1) CGST @ 0.00%, SGST @ 0.00%
190590 MODERN MILK PLUS BR 1 30.00 30.00
2) CGST @ 2.50%, SGST @ 2.50%
040140 MILKY MIST UHT -1lt 1 72.00 72.00
040320 JERSEY CURD PP-425g 1 38.00 38.00
090230 WAGHBAKRI STRO-250g 1 142.00 142.00
151219 GOLD DROP SUNFL-1lt 2 142.00 284.00
190240 BAMBINO ROASTE-400g 1 55.00 55.00
190540 PARLE REAL ELA-400g 1 85.00 85.00
210690 ASAL IDLY & DOS-1kg 1 47.50 47.50
3) CGST @ 6.00%, SGST @ 6.00%
040510 MILKY MIST TBL -10g 1 68.00 68.00
200410 MCCAIN MASALA -375g 1 117.25 117.25
220299 EPIGAMIA CHOC-180ml 2 20.00 40.00
4) CGST @ 9.00%, SGST @ 9.00%
190590 PARLE KRACKJ-176.4g 1 36.00 36.00
SALE ROUND OFF ACCOUNT (+) 0.25
Items: 12 Qty: 14 993.00
GST Breakup Details (Amount INR)
GST IND Taxable CGST SGST CESS Total Amount
1 30.25 0.00 0.00 30.25
2 670.00 16.75 16.75 703.50
3 199.33 11.96 11.96 223.25
4 30.50 2.75 2.75 36.00
T: 930.00 31.46 31.46 993.00
Amount Received From Customer
Cash 1003.00
Balance Paid In Cash 10.00
Saved Rs. 350.00 On MRP""",
                ),
            ),
        )

        assertEquals(99_300L, receipt.printedTotalCents)
        assertEquals(93_000L, receipt.subtotalCents)
        assertEquals(6_292L, receipt.taxCents)
        assertEquals(35_000L, receipt.savingsCents)
        assertEquals(0L, receipt.discountCents)
        assertEquals(12, receipt.items.size)
        assertEquals("Modern Milk Plus Br", receipt.items.first().name)
        assertEquals(1.0, receipt.items.first().quantity, .001)
        assertEquals(3_000L, receipt.items.first().unitPriceCents)
        assertEquals(3_000L, receipt.items.first().lineTotalCents)
        assertEquals("Gold Drop Sunfl-1lt", receipt.items[4].name)
        assertEquals(2.0, receipt.items[4].quantity, .001)
        assertEquals(14_200L, receipt.items[4].unitPriceCents)
        assertEquals(28_400L, receipt.items[4].lineTotalCents)
        assertTrue(receipt.items.none { item ->
            listOf("GST", "HSN", "FSSAI", "Cash", "Balance", "Saved").any { item.name.contains(it, true) }
        })
    }

    @Test
    fun itemSummaryWithoutAmountDoesNotTurnQuantityIntoReceiptTotal() {
        val receipt = requireNotNull(
            ReceiptIntelligence.parse(
                listOf(
                    """MEDPLUS PHARMACY
Description Qty Rate Amount
PARACETAMOL 2 25.00 50.00
Items: 1 Qty: 2
110.00
GST Breakup Details
T: 100.00 5.00 5.00 110.00
Cash 200.00
Change 90.00""",
                ),
            ),
        )

        assertEquals(11_000L, receipt.printedTotalCents)
        assertEquals(10_000L, receipt.subtotalCents)
        assertEquals(1_000L, receipt.taxCents)
        assertEquals(1, receipt.items.size)
    }

    @Test
    fun parsesDeliveryReceiptWithoutPromotingFeesOrOrderIdsToProducts() {
        val receipt = requireNotNull(
            ReceiptIntelligence.parse(
                listOf(
                    """SWIGGY
Order #18473920
Description Qty Rate Amount
Paneer Roll 2 180.00 360.00
Delivery fee 25.00
GST 18.00
Grand Total 403.00""",
                ),
            ),
        )

        assertEquals(40_300L, receipt.printedTotalCents)
        assertEquals(1, receipt.items.size)
        assertEquals("Paneer Roll", receipt.items.single().name)
        assertEquals(2.0, receipt.items.single().quantity, .001)
        assertEquals(36_000L, receipt.items.single().lineTotalCents)
    }

    @Test
    fun understandsIndianQtyRateAmountColumnsWithoutAnXMarker() {
        val receipt = requireNotNull(
            ReceiptIntelligence.parse(
                listOf(
                    """RELIANCE SMART
KURKURE MASALA 3 PCS 20.00 60.00
AMUL MILK 2 NOS 31.00 62.00
ITEM TOTAL 122.00
GRAND TOTAL 122.00""",
                ),
            ),
        )

        assertEquals(2, receipt.items.size)
        assertEquals(3.0, receipt.items[0].quantity, .001)
        assertEquals(2_000L, receipt.items[0].unitPriceCents)
        assertEquals(6_000L, receipt.items[0].lineTotalCents)
    }
}
