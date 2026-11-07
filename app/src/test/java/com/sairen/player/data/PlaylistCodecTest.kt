package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistCodecTest {

    @Test
    fun `round trip preserves names and tracks`() {
        val playlists = listOf(
            Playlist(id = "one", name = "Road trip", trackIds = listOf(5L, 6L, 7L)),
            Playlist(id = "two", name = "Quiet", trackIds = emptyList()),
        )
        assertEquals(playlists, PlaylistCodec.decode(PlaylistCodec.encode(playlists)))
    }

    @Test
    fun `names with separators and newlines survive`() {
        val playlists = listOf(
            Playlist(id = "a", name = "pipes | and \n newlines |", trackIds = listOf(1L)),
            Playlist(id = "b", name = "emoji 🎧 ünïcode", trackIds = emptyList()),
        )
        assertEquals(playlists, PlaylistCodec.decode(PlaylistCodec.encode(playlists)))
    }

    @Test
    fun `decode tolerates empty and malformed payloads`() {
        assertTrue(PlaylistCodec.decode(null).isEmpty())
        assertTrue(PlaylistCodec.decode("").isEmpty())
        assertTrue(PlaylistCodec.decode("garbage-without-fields").isEmpty())
        assertTrue(PlaylistCodec.decode("|nameonly").isEmpty())
    }

    @Test
    fun `decode drops duplicate ids and non-positive track ids`() {
        val payload = PlaylistCodec.encode(listOf(Playlist("a", "One", listOf(1L, 2L)))) +
            "\n" + PlaylistCodec.encode(listOf(Playlist("a", "Duplicate", listOf(3L))))
        val decoded = PlaylistCodec.decode(payload)
        assertEquals(1, decoded.size)
        assertEquals("One", decoded.single().name)
    }

    @Test
    fun `decode normalises junk track ids`() {
        val raw = "p1|${java.util.Base64.getEncoder().encodeToString("Mix".toByteArray())}|1, x, 0, -3, 2"
        assertEquals(listOf(1L, 2L), PlaylistCodec.decode(raw).single().trackIds)
    }

    @Test
    fun `blank stored name falls back to a placeholder`() {
        val raw = "p1|${java.util.Base64.getEncoder().encodeToString("   ".toByteArray())}|"
        assertEquals("Playlist", PlaylistCodec.decode(raw).single().name)
    }

    @Test
    fun `playlists without ids are not encoded`() {
        assertTrue(PlaylistCodec.encode(listOf(Playlist("", "Nameless"))).isEmpty())
    }
}
