package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayCountRulesTest {

    @Test
    fun `long tracks need thirty seconds`() {
        assertEquals(30_000L, PlayCountRules.thresholdMs(240_000L))
        assertEquals(30_000L, PlayCountRules.thresholdMs(120_000L))
    }

    @Test
    fun `short tracks need half their duration`() {
        assertEquals(20_000L, PlayCountRules.thresholdMs(40_000L))
        // Half of 30s is smaller than the 30s rule, so a 30s clip needs 15s.
        assertEquals(15_000L, PlayCountRules.thresholdMs(30_000L))
        assertEquals(15_500L, PlayCountRules.thresholdMs(31_000L))
    }

    @Test
    fun `unknown duration falls back to the fixed threshold`() {
        assertEquals(30_000L, PlayCountRules.thresholdMs(0L))
        assertEquals(30_000L, PlayCountRules.thresholdMs(-1L))
    }

    @Test
    fun `position below the threshold does not count`() {
        assertFalse(PlayCountRules.qualifies(positionMs = 0L, durationMs = 240_000L))
        assertFalse(PlayCountRules.qualifies(positionMs = 29_999L, durationMs = 240_000L))
        assertFalse(PlayCountRules.qualifies(positionMs = 19_999L, durationMs = 40_000L))
    }

    @Test
    fun `position at the threshold counts`() {
        assertTrue(PlayCountRules.qualifies(positionMs = 30_000L, durationMs = 240_000L))
        assertTrue(PlayCountRules.qualifies(positionMs = 20_000L, durationMs = 40_000L))
    }

    @Test
    fun `unknown duration still requires thirty seconds`() {
        assertFalse(PlayCountRules.qualifies(positionMs = 1_000L, durationMs = 0L))
        assertTrue(PlayCountRules.qualifies(positionMs = 30_000L, durationMs = 0L))
    }
}
