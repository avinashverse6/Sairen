package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LikedTracksTest {

    @Test
    fun `toggle adds then removes`() {
        val empty = emptySet<Long>()
        val liked = LikedTracks.toggle(empty, 7L)
        assertEquals(setOf(7L), liked)
        assertEquals(emptySet<Long>(), LikedTracks.toggle(liked, 7L))
    }

    @Test
    fun `toggle ignores invalid ids`() {
        val liked = setOf(1L)
        assertEquals(liked, LikedTracks.toggle(liked, 0L))
        assertEquals(liked, LikedTracks.toggle(liked, -2L))
    }

    @Test
    fun `encode is sorted and decodes back`() {
        val encoded = LikedTracks.encode(setOf(9L, 3L, 30L))
        assertEquals("3,9,30", encoded)
        assertEquals(setOf(3L, 9L, 30L), LikedTracks.decode(encoded))
    }

    @Test
    fun `decode tolerates junk and drops non-positive ids`() {
        assertTrue(LikedTracks.decode(null).isEmpty())
        assertTrue(LikedTracks.decode("  ").isEmpty())
        assertEquals(setOf(4L, 6L), LikedTracks.decode("4, x, 6, 0, -1,"))
    }

    @Test
    fun `empty set round trips`() {
        assertEquals(emptySet<Long>(), LikedTracks.decode(LikedTracks.encode(emptySet())))
    }
}
