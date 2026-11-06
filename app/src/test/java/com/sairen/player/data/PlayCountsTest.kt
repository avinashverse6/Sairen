package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayCountsTest {

    @Test
    fun `encode sorts by id and skips empty entries`() {
        val encoded = PlayCounts.encode(mapOf(9L to 2, 3L to 7, 5L to 0))
        assertEquals("3:7,9:2", encoded)
    }

    @Test
    fun `round trip preserves counts`() {
        val counts = mapOf(1L to 1, 42L to 13, 7L to 4)
        assertEquals(counts, PlayCounts.decode(PlayCounts.encode(counts)))
    }

    @Test
    fun `decode tolerates junk`() {
        assertEquals(emptyMap<Long, Int>(), PlayCounts.decode(null))
        assertEquals(emptyMap<Long, Int>(), PlayCounts.decode(""))
        assertEquals(emptyMap<Long, Int>(), PlayCounts.decode(",,,:::"))
        assertEquals(mapOf(4L to 2), PlayCounts.decode("abc,4:2,-3:5,9:x,0:8"))
    }

    @Test
    fun `decode clamps absurd counts`() {
        val decoded = PlayCounts.decode("1:999999999")
        assertEquals(PlayCounts.MAX_COUNT, decoded[1L])
    }

    @Test
    fun `increment adds one play`() {
        assertEquals(mapOf(1L to 1), PlayCounts.increment(emptyMap(), 1L))
        assertEquals(mapOf(1L to 4), PlayCounts.increment(mapOf(1L to 3), 1L))
    }

    @Test
    fun `increment ignores invalid ids and keeps the map`() {
        val counts = mapOf(1L to 3)
        assertEquals(counts, PlayCounts.increment(counts, 0L))
        assertEquals(counts, PlayCounts.increment(counts, -5L))
    }

    @Test
    fun `increment does not overflow past the cap`() {
        val atCap = mapOf(1L to PlayCounts.MAX_COUNT)
        assertEquals(atCap, PlayCounts.increment(atCap, 1L))
    }

    @Test
    fun `countOf defaults to zero`() {
        assertEquals(0, PlayCounts.countOf(emptyMap(), 5L))
        assertEquals(0, PlayCounts.countOf(mapOf(5L to 2), 0L))
        assertEquals(2, PlayCounts.countOf(mapOf(5L to 2), 5L))
        assertTrue(PlayCounts.decode(PlayCounts.encode(mapOf(2L to 1))).isNotEmpty())
    }
}
