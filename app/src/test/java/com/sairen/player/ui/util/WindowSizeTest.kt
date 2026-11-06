package com.sairen.player.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowSizeTest {

    @Test
    fun `breakpoints follow the material 3 classes`() {
        assertEquals(SairenWindowSize.COMPACT, SairenWindowSize.fromWidthDp(320f))
        assertEquals(SairenWindowSize.COMPACT, SairenWindowSize.fromWidthDp(599f))
        assertEquals(SairenWindowSize.MEDIUM, SairenWindowSize.fromWidthDp(600f))
        assertEquals(SairenWindowSize.MEDIUM, SairenWindowSize.fromWidthDp(839f))
        assertEquals(SairenWindowSize.EXPANDED, SairenWindowSize.fromWidthDp(840f))
        assertEquals(SairenWindowSize.EXPANDED, SairenWindowSize.fromWidthDp(1280f))
    }

    @Test
    fun `flags match the bucket`() {
        assertEquals(true, SairenWindowSize.COMPACT.isCompact)
        assertEquals(false, SairenWindowSize.MEDIUM.isCompact)
        assertEquals(true, SairenWindowSize.EXPANDED.isExpanded)
    }
}
