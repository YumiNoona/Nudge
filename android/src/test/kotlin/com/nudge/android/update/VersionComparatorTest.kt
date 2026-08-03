package com.nudge.android.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test fun detectsNewerSemanticVersions() {
        assertTrue(VersionComparator.isNewer("v1.0.1", "1.0.0"))
        assertTrue(VersionComparator.isNewer("2.0.0", "1.9.9"))
        assertTrue(VersionComparator.isNewer("1.1", "1.0.9"))
    }

    @Test fun rejectsCurrentAndOlderVersions() {
        assertFalse(VersionComparator.isNewer("v1.0.0", "1.0.0"))
        assertFalse(VersionComparator.isNewer("0.9.9", "1.0.0"))
    }
}
