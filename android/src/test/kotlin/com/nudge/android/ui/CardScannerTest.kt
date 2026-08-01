package com.nudge.android.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardScannerTest {
    @Test fun acceptsKnownValidCardNumbers() {
        assertTrue(passesLuhn("4111111111111111"))
        assertTrue(passesLuhn("5555555555554444"))
    }

    @Test fun rejectsInvalidOrUnsafeCandidates() {
        assertFalse(passesLuhn("4111111111111112"))
        assertFalse(passesLuhn("1234"))
    }
}
