package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentPlayedListTest {

    @Test
    fun `new track goes to the front`() {
        assertEquals(listOf(3L, 1L, 2L), RecentPlayedList.updated(listOf(1L, 2L), 3L))
    }

    @Test
    fun `replaying a track moves it to the front without duplicating`() {
        assertEquals(listOf(2L, 1L, 3L), RecentPlayedList.updated(listOf(1L, 2L, 3L), 2L))
    }

    @Test
    fun `history is capped`() {
        val current = (1L..60L).toList()
        val updated = RecentPlayedList.updated(current, 99L, limit = 50)
        assertEquals(50, updated.size)
        assertEquals(99L, updated.first())
    }

    @Test
    fun `invalid ids are ignored`() {
        val current = listOf(1L, 2L)
        assertEquals(current, RecentPlayedList.updated(current, 0L))
        assertEquals(current, RecentPlayedList.updated(current, -5L))
    }

    @Test
    fun `decoding tolerates malformed storage`() {
        assertEquals(emptyList<Long>(), RecentPlayedList.decode(null))
        assertEquals(emptyList<Long>(), RecentPlayedList.decode(""))
        assertEquals(listOf(4L, 5L), RecentPlayedList.decode("4, 5"))
        assertEquals(listOf(1L), RecentPlayedList.decode("1,abc,,0"))
    }
}
