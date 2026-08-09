package com.nudge.android.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryIconsTest {
    @Test
    fun libraryContainsExactly200UniqueSearchableIcons() {
        assertEquals(200, CategoryIcons.all.size)
        assertEquals(200, CategoryIcons.all.map { it.key }.distinct().size)
        assertEquals(200, CategoryIcons.all.map { it.label }.distinct().size)
    }
}
