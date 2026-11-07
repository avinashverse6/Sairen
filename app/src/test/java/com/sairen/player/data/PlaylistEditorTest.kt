package com.sairen.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistEditorTest {

    private fun playlists(): List<Playlist> = listOf(
        Playlist(id = "a", name = "Morning", trackIds = listOf(1L, 2L, 3L)),
        Playlist(id = "b", name = "Focus", trackIds = listOf(4L)),
    )

    @Test
    fun `create appends a trimmed playlist`() {
        val created = PlaylistEditor.create(playlists(), id = "c", name = "  Night  ")
        assertEquals(3, created.size)
        assertEquals("Night", created.last().name)
        assertTrue(created.last().trackIds.isEmpty())
    }

    @Test
    fun `create ignores blank names and duplicate ids`() {
        assertEquals(2, PlaylistEditor.create(playlists(), id = "c", name = "   ").size)
        assertEquals(2, PlaylistEditor.create(playlists(), id = "a", name = "Other").size)
    }

    @Test
    fun `create truncates very long names`() {
        val created = PlaylistEditor.create(emptyList(), id = "x", name = "n".repeat(200))
        assertEquals(PlaylistEditor.MAX_NAME_LENGTH, created.single().name.length)
    }

    @Test
    fun `rename changes only the target`() {
        val renamed = PlaylistEditor.rename(playlists(), "a", "Sunrise")
        assertEquals("Sunrise", PlaylistEditor.find(renamed, "a")!!.name)
        assertEquals("Focus", PlaylistEditor.find(renamed, "b")!!.name)
    }

    @Test
    fun `rename ignores a blank name`() {
        assertEquals("Morning", PlaylistEditor.find(PlaylistEditor.rename(playlists(), "a", "  "), "a")!!.name)
    }

    @Test
    fun `delete removes the playlist`() {
        val remaining = PlaylistEditor.delete(playlists(), "a")
        assertEquals(listOf("b"), remaining.map { it.id })
        assertNull(PlaylistEditor.find(remaining, "a"))
    }

    @Test
    fun `delete of an unknown id is a no-op`() {
        assertEquals(playlists(), PlaylistEditor.delete(playlists(), "zzz"))
    }

    @Test
    fun `addTrack appends once and deduplicates`() {
        val added = PlaylistEditor.addTrack(playlists(), "b", 9L)
        assertEquals(listOf(4L, 9L), PlaylistEditor.find(added, "b")!!.trackIds)

        val again = PlaylistEditor.addTrack(added, "b", 9L)
        assertEquals(listOf(4L, 9L), PlaylistEditor.find(again, "b")!!.trackIds)
    }

    @Test
    fun `addTrack ignores invalid ids and unknown playlists`() {
        assertEquals(playlists(), PlaylistEditor.addTrack(playlists(), "b", 0L))
        assertEquals(playlists(), PlaylistEditor.addTrack(playlists(), "missing", 5L))
    }

    @Test
    fun `removeAt drops the entry at the index`() {
        val removed = PlaylistEditor.removeAt(playlists(), "a", 1)
        assertEquals(listOf(1L, 3L), PlaylistEditor.find(removed, "a")!!.trackIds)
    }

    @Test
    fun `removeAt ignores out-of-range indices`() {
        assertEquals(playlists(), PlaylistEditor.removeAt(playlists(), "a", 3))
        assertEquals(playlists(), PlaylistEditor.removeAt(playlists(), "a", -1))
    }

    @Test
    fun `move up and down reorder the entries`() {
        val up = PlaylistEditor.move(playlists(), "a", 2, -1)
        assertEquals(listOf(1L, 3L, 2L), PlaylistEditor.find(up, "a")!!.trackIds)

        val down = PlaylistEditor.move(playlists(), "a", 0, 1)
        assertEquals(listOf(2L, 1L, 3L), PlaylistEditor.find(down, "a")!!.trackIds)
    }

    @Test
    fun `moves that leave the list are ignored`() {
        assertEquals(playlists(), PlaylistEditor.move(playlists(), "a", 0, -1))
        assertEquals(playlists(), PlaylistEditor.move(playlists(), "a", 2, 1))
        assertEquals(playlists(), PlaylistEditor.move(playlists(), "a", 9, 1))
    }

    @Test
    fun `move only touches the requested playlist`() {
        val moved = PlaylistEditor.move(playlists(), "a", 0, 1)
        assertEquals(listOf(4L), PlaylistEditor.find(moved, "b")!!.trackIds)
    }

    @Test
    fun `existingTrackIds keeps order and drops missing tracks`() {
        val playlist = Playlist(id = "a", name = "Mix", trackIds = listOf(3L, 99L, 1L))
        assertEquals(listOf(3L, 1L), PlaylistEditor.existingTrackIds(playlist, setOf(1L, 3L)))
        assertTrue(PlaylistEditor.existingTrackIds(playlist, emptySet()).isEmpty())
    }
}
