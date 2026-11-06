package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        assertEquals(emptyList<Long>(), RecentPlayedStore.decode(null))
        assertEquals(emptyList<Long>(), RecentPlayedStore.decode(""))
        assertEquals(listOf(4L, 5L), RecentPlayedStore.decode("4, 5"))
        assertEquals(listOf(1L), RecentPlayedStore.decode("1,abc,,0"))
    }
}

class SavedSessionCodecTest {

    @Test
    fun `round trips a session`() {
        val session = SavedSession(queueIds = listOf(11L, 12L, 13L), startIndex = 1, positionMs = 42_000L)
        val decoded = SavedSessionCodec.decode(SavedSessionCodec.encode(session))
        assertEquals(session, decoded)
    }

    @Test
    fun `rejects malformed payloads`() {
        assertNull(SavedSessionCodec.decode(null))
        assertNull(SavedSessionCodec.decode(""))
        assertNull(SavedSessionCodec.decode("v1;0;0"))
        assertNull(SavedSessionCodec.decode("v2;0;0;1,2"))
        assertNull(SavedSessionCodec.decode("v1;x;0;1,2"))
        assertNull(SavedSessionCodec.decode("v1;0;0;"))
    }

    @Test
    fun `clamps out of range indices and drops duplicates`() {
        val decoded = SavedSessionCodec.decode("v1;9;500;7,7,8")!!
        assertEquals(listOf(7L, 8L), decoded.queueIds)
        assertEquals(1, decoded.startIndex)
        assertEquals(500L, decoded.positionMs)
    }

    @Test
    fun `encoded payload is human readable`() {
        val encoded = SavedSessionCodec.encode(SavedSession(listOf(1L, 2L), 0, 0))
        assertEquals("v1;0;0;1,2", encoded)
        assertTrue(encoded.startsWith("v1;"))
    }
}

class ThemeModeTest {

    @Test
    fun `storage values round trip`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.storageValue))
        }
    }

    @Test
    fun `unknown storage values fall back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("sepia"))
    }
}
