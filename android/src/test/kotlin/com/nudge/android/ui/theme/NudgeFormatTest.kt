package com.nudge.android.ui.theme

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NudgeFormatTest {
    @Test
    fun `compact amounts preserve useful decimal precision`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("24.5K", formatCompactCentsPlain(2_450_000))
            assertEquals("24K", formatCompactCentsPlain(2_400_000))
            assertEquals("24.5L", formatCompactCentsPlain(245_000_000))
        } finally {
            Locale.setDefault(original)
        }
    }
}
