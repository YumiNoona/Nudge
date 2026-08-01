package com.nudge.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceMessagePolicyTest {
    @Test fun savesOnlyAutomaticSourcesWhenEnabled() {
        assertTrue(SourceMessagePolicy.shouldSaveBody(true, "sms"))
        assertTrue(SourceMessagePolicy.shouldSaveBody(true, "notification"))
        assertFalse(SourceMessagePolicy.shouldSaveBody(false, "sms"))
        assertFalse(SourceMessagePolicy.shouldSaveBody(true, "manual"))
    }

    @Test fun computesRetentionCutoffSafely() {
        val now = 1_000_000_000L
        assertEquals(now - 30L * 24L * 60L * 60L * 1000L, SourceMessagePolicy.retentionCutoff(now, 30))
        assertNull(SourceMessagePolicy.retentionCutoff(now, null))
        assertNull(SourceMessagePolicy.retentionCutoff(now, 0))
    }
}
