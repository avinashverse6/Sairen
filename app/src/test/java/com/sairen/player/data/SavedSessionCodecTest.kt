package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
