package com.sairen.player.data

import com.sairen.player.testing.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupingTest {

    @Test
    fun `albums aggregate track count and total duration`() {
        val albums = LibraryGrouping.albumsOf(
            listOf(
                track(1, "A", album = "Record", albumId = 5, durationMs = 60_000),
                track(2, "B", album = "Record", albumId = 5, durationMs = 90_000),
                track(3, "C", album = "Other", albumId = 6),
            ),
        )

        val record = albums.first { it.id == 5L }
        assertEquals(2, record.trackCount)
        assertEquals(150_000L, record.durationMs)
        assertEquals("Record", record.title)
        assertEquals(2, albums.size)
    }

    @Test
    fun `albums and artists are sorted case-insensitively`() {
        val snapshot = LibraryGrouping.snapshot(
            listOf(
                track(1, "x", artist = "zebra", album = "beta", albumId = 1),
                track(2, "y", artist = "Alpha", album = "alpha", albumId = 2),
            ),
        )
        assertEquals(listOf("alpha", "beta"), snapshot.albums.map { it.title })
        assertEquals(listOf("Alpha", "zebra"), snapshot.artists.map { it.name })
    }

    @Test
    fun `artists count distinct albums`() {
        val artists = LibraryGrouping.artistsOf(
            listOf(
                track(1, "a", artist = "Shared", album = "One", albumId = 1),
                track(2, "b", artist = "Shared", album = "Two", albumId = 2),
                track(3, "c", artist = "SHARED", album = "Two", albumId = 2),
            ),
        )
        val shared = artists.single()
        assertEquals(3, shared.trackCount)
        assertEquals(2, shared.albumCount)
    }

    @Test
    fun `artwork is inherited from the first track that has some`() {
        val withoutArt = track(1, "a", album = "One", albumId = 1)
        val withArt = withoutArt.copy(id = 2, artworkUri = "content://art/1", albumId = 1)
        val album = LibraryGrouping.albumsOf(listOf(withoutArt, withArt)).single()
        assertEquals("content://art/1", album.artworkUri)
    }

    @Test
    fun `empty library produces empty groups`() {
        val snapshot = LibraryGrouping.snapshot(emptyList())
        assertTrue(snapshot.isEmpty)
        assertTrue(snapshot.albums.isEmpty())
        assertTrue(snapshot.artists.isEmpty())
    }
}
