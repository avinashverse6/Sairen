package com.sairen.player.ui.util

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
