package com.sairen.player.ui.util

import com.sairen.player.ui.theme.shouldUseDarkTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:07", formatDuration(7_400))
        assertEquals("3:05", formatDuration(185_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test
    fun `formats hours once a track passes an hour`() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:03:04", formatDuration(7_384_000))
    }

    @Test
    fun `negative and unknown durations clamp to zero`() {
        assertEquals("0:00", formatDuration(-1))
        assertEquals("0:00", formatDuration(Long.MIN_VALUE))
    }

    @Test
    fun `total duration is summarised in minutes and hours`() {
        assertEquals("0 min", formatTotalDuration(0))
        assertEquals("48 min", formatTotalDuration(48 * 60_000L))
        assertEquals("1 h 12 min", formatTotalDuration(72 * 60_000L))
    }
}

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

class ThemeResolutionTest {

    @Test
    fun `system mode follows the system setting`() {
        assertEquals(true, shouldUseDarkTheme(com.sairen.player.data.ThemeMode.SYSTEM, true))
        assertEquals(false, shouldUseDarkTheme(com.sairen.player.data.ThemeMode.SYSTEM, false))
    }

    @Test
    fun `explicit modes override the system setting`() {
        assertEquals(true, shouldUseDarkTheme(com.sairen.player.data.ThemeMode.DARK, false))
        assertEquals(false, shouldUseDarkTheme(com.sairen.player.data.ThemeMode.LIGHT, true))
    }
}
