package com.nudge.android.data

import org.junit.Assert.*
import org.junit.Test

class CaptureMatchingTest {
    private fun account(id: String, last4: String, bank: String = "HDFC", active: Boolean = true) =
        AccountEntity(id, id, bank, "upi", last4, null, isActive = active, isDefault = id == "primary")
    private val accounts = listOf(account("primary", "1234"), account("secondary", "5678"))
    private fun evidence(source: String = "sms", account: String = "primary", at: Long = 100000,
        merchant: String = "Corner Cafe", reference: String? = null, fingerprint: String? = null, confidence: Float = .95f) =
        CaptureDuplicateMatcher.Evidence(account, confidence, merchant, source, at, fingerprint, reference)

    @Test fun repeatPaymentsAreNotSilentlyRemoved() {
        assertEquals(CaptureDuplicateMatcher.Decision.POSSIBLE, CaptureDuplicateMatcher.compare(evidence(at = 101000), evidence()))
    }
    @Test fun differentMerchantsRemainDistinct() {
        assertEquals(CaptureDuplicateMatcher.Decision.DISTINCT, CaptureDuplicateMatcher.compare(evidence(merchant = "Metro Rail"), evidence()))
    }
    @Test fun differentAccountsRemainDistinct() {
        assertEquals(CaptureDuplicateMatcher.Decision.DISTINCT, CaptureDuplicateMatcher.compare(evidence(account = "secondary"), evidence()))
    }
    @Test fun differentReferencesOverrideTimeAndMerchantSimilarity() {
        assertEquals(CaptureDuplicateMatcher.Decision.DISTINCT, CaptureDuplicateMatcher.compare(evidence(reference = "one"), evidence(reference = "two")))
    }
    @Test fun matchingReferenceCombinesKnownAccountSources() {
        assertEquals(CaptureDuplicateMatcher.Decision.DUPLICATE, CaptureDuplicateMatcher.compare(evidence("notification", reference = "one"), evidence(reference = "one")))
    }
    @Test fun matchingReferenceDoesNotHideUncertainAccount() {
        assertEquals(CaptureDuplicateMatcher.Decision.POSSIBLE, CaptureDuplicateMatcher.compare(evidence(reference = "one", confidence = .25f), evidence(reference = "one")))
    }
    @Test fun exactRetryIsIdempotent() {
        assertEquals(CaptureDuplicateMatcher.Decision.DUPLICATE, CaptureDuplicateMatcher.compare(evidence(fingerprint = "abc"), evidence(fingerprint = "abc")))
    }
    @Test fun crossSourceWithoutReferenceNeedsStrongAccountAndMerchant() {
        assertEquals(CaptureDuplicateMatcher.Decision.POSSIBLE, CaptureDuplicateMatcher.compare(evidence("notification"), evidence()))
        assertEquals(CaptureDuplicateMatcher.Decision.POSSIBLE, CaptureDuplicateMatcher.compare(evidence("notification", confidence = .25f), evidence()))
    }
    @Test fun delayedNoReferencePaymentIsNotMerged() {
        assertEquals(CaptureDuplicateMatcher.Decision.DISTINCT, CaptureDuplicateMatcher.compare(evidence("notification", at = 300000), evidence()))
    }
    @Test fun unknownMerchantsNeverMergeByName() {
        assertEquals(CaptureDuplicateMatcher.Decision.DISTINCT, CaptureDuplicateMatcher.compare(evidence("notification", merchant = "Unknown merchant"), evidence(merchant = "Unknown merchant")))
    }
    @Test fun matchesMaskedAccountInsteadOfDefault() {
        val match = CaptureAccountMatcher.resolve("INR 100 debited from HDFC a/c XX5678", accounts)
        assertEquals("secondary", match.account?.id)
        assertTrue(match.confidence >= .95f)
    }
    @Test fun ambiguousBankDoesNotChooseAnAccountConfidently() {
        assertTrue(CaptureAccountMatcher.resolve("HDFC payment completed", accounts).confidence < .85f)
    }
    @Test fun handlesMultipleMaskStylesAndPlainSuffixes() {
        listOf("****5678", "••••5678", "ending in 5678", "a/c 5678", "Card no. 5678").forEach {
            assertEquals(it, "secondary", CaptureAccountMatcher.resolve(it, accounts).account?.id)
        }
    }
    @Test fun sameSuffixAtDifferentBanksNeedsBankEvidence() {
        val both = accounts + account("other", "5678", "ICICI")
        assertEquals("other", CaptureAccountMatcher.resolve("ICICI XX5678", both).account?.id)
        assertTrue(CaptureAccountMatcher.resolve("XX5678", both).confidence < .85f)
    }
    @Test fun conflictingBankAndDigitsStayUncertain() {
        assertTrue(CaptureAccountMatcher.resolve("ICICI XX1234", accounts + account("other", "5678", "ICICI")).confidence < .85f)
    }
    @Test fun inactiveAccountsAreNeverSelected() {
        assertNull(CaptureAccountMatcher.resolve("XX1234", listOf(account("old", "1234", active = false))).account)
    }
    @Test fun twoAccountSuffixesRequireReview() {
        assertTrue(CaptureAccountMatcher.resolve("HDFC transferred from XX1234 to XX5678", accounts).confidence < .85f)
    }
    @Test fun directionCannotMaskLowAmountConfidence() {
        val assessment = CaptureAssessment(.4f, .99f, .98f, .99f, .98f)
        assertTrue(assessment.needsReview)
        assertEquals(.4f, assessment.overall)
        assertTrue("Check the amount" in assessment.reasons)
    }
    @Test fun learnedCategoryCannotMaskAnUncertainAccount() {
        assertTrue(CaptureAssessment(.95f, .99f, .98f, .25f, .98f).needsReview)
    }
    @Test fun completeEvidenceCanBeAutoAccepted() {
        assertFalse(CaptureAssessment(.95f, .99f, .98f, .99f, .98f).needsReview)
    }
    @Test fun referenceHashNormalizesLabelsAndDoesNotStoreReference() {
        val first = CaptureIdentity.referenceHash("UTR: 123456789012")
        assertNotNull(first)
        assertEquals(first, CaptureIdentity.referenceHash("UPI Ref No. 123456789012"))
        assertFalse(first!!.contains("123456789012"))
    }
    @Test fun conflictingReferencesAndMissingReferencesAreNotInvented() {
        assertNull(CaptureIdentity.referenceHash("Ref 12345678 UTR 87654321"))
        assertNull(CaptureIdentity.referenceHash("Paid INR 100 to Cafe"))
    }
    @Test fun repeatedIdenticalMessagesAtDifferentTimesGetDifferentIdentities() {
        assertNotEquals(CaptureIdentity.fingerprint("sms", "BANK", "Paid INR 100", 1), CaptureIdentity.fingerprint("sms", "BANK", "Paid INR 100", 2))
    }
    @Test fun smsLookupUsesNearestMessageInsteadOfTheNewestIdenticalBody() {
        assertEquals("first", CaptureIdentity.closestMessage(listOf("first" to 100000L, "second" to 102000L), 100000L))
        assertNull(CaptureIdentity.closestMessage(listOf("first" to 100000L, "second" to 100000L), 100000L))
        assertNull(CaptureIdentity.closestMessage(listOf("old" to 100000L), 500000L))
    }
}
