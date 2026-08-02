package com.nudge.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureLearningTest {
    @Test
    fun companySuffixesAndDatesDoNotCreateNewMerchant() {
        assertEquals("unity software", CaptureLearning.canonicalMerchant("Unity Software Inc on 2026-07-26"))
        assertTrue(CaptureLearning.sameMerchant("Unity Software Inc", "UNITY SOFTWARE PVT LTD"))
    }

    @Test
    fun merchantVariantsMatchButUnrelatedMerchantsDoNot() {
        assertTrue(CaptureLearning.sameMerchant("Swiggy Bangalore", "SWIGGY"))
        assertFalse(CaptureLearning.sameMerchant("Swiggy", "Vodafone Idea"))
    }

    @Test
    fun tolerantPatternMatchesDecoratedBankText() {
        val pattern = CaptureLearning.tolerantPattern("Amazon Pay India Pvt Ltd")
        assertTrue(Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn("spent at AMAZON*PAY/INDIA ref 8891"))
    }

    @Test
    fun rejectionTemplateSurvivesChangingAmountsDatesAndReferences() {
        val pattern = CaptureLearning.rejectionPattern(
            sender = "AD-CLIQ",
            rawText = "INR 100 credited to your CLiQ Cash Wallet successfully. Ref 889122 on 01-08-2026",
            merchant = "Your CLiQ Cash Wallet Successfully",
        )
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        assertTrue(regex.containsMatchIn("AD-CLIQ INR 750 credited to your CLiQ Cash Wallet successfully. Ref 445566 on 02-09-2026"))
        assertFalse(regex.containsMatchIn("AX-OTHER INR 750 credited to a savings account successfully"))
    }

    @Test
    fun rejectedSuggestionMarkerIsExplicit() {
        assertTrue(CaptureLearning.isRejectedSuggestion(CaptureLearning.REJECTED_SUGGESTION))
        assertFalse(CaptureLearning.isRejectedSuggestion("food"))
    }
}
