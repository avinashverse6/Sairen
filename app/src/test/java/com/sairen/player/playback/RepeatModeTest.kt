package com.sairen.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeTest {

    @Test
    fun `cycling goes off to all to one and back`() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }

    @Test
    fun `player values match media3 constants`() {
        assertEquals(0, RepeatMode.OFF.playerValue)
        assertEquals(1, RepeatMode.ALL.playerValue)
        assertEquals(2, RepeatMode.ONE.playerValue)
    }

    @Test
    fun `unknown player modes fall back to off`() {
        assertEquals(RepeatMode.OFF, RepeatMode.fromPlayerMode(-1))
        assertEquals(RepeatMode.ALL, RepeatMode.fromPlayerMode(1))
    }

    @Test
    fun `progress is clamped and safe for unknown durations`() {
        val state = PlayerUiState(positionMs = 30_000, durationMs = 120_000)
        assertEquals(0.25f, state.progress, 0.0001f)
        assertEquals(0f, PlayerUiState(positionMs = 10, durationMs = 0).progress, 0.0001f)
    }

    @Test
    fun `hasTrack reflects the current media id`() {
        assertEquals(false, PlayerUiState().hasTrack)
        assertEquals(true, PlayerUiState(currentMediaId = 42L).hasTrack)
    }
}
