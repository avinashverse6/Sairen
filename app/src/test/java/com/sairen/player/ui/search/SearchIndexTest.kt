package com.sairen.player.ui.search

import com.sairen.player.data.LibraryGrouping
import com.sairen.player.testing.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexTest {

    private val library = LibraryGrouping.snapshot(
        listOf(
            track(1, "Kind of Blue", artist = "Miles Davis", album = "Kind of Blue", albumId = 10),
            track(2, "So What", artist = "Miles Davis", album = "Kind of Blue", albumId = 10),
            track(3, "Blue in Green", artist = "Bill Evans", album = "Kind of Blue", albumId = 10),
            track(4, "Time Out", artist = "The Dave Brubeck Quartet", album = "Time Out", albumId = 11),
            track(5, "Take Five", artist = "The Dave Brubeck Quartet", album = "Time Out", albumId = 11),
        ),
    )

    private val index = SearchIndex.of(library)

    @Test
    fun `blank query returns nothing`() {
        assertTrue(index.search("").isEmpty)
        assertTrue(index.search("   ").isEmpty)
    }

    @Test
    fun `search is case insensitive`() {
        val lower = index.search("miles")
        val upper = index.search("MILES")
        assertEquals(lower.tracks.map { it.id }, upper.tracks.map { it.id })
        assertEquals(2, lower.tracks.size)
    }

    @Test
    fun `title matches rank above album only matches`() {
        val results = index.search("blue")
        // 3: title starts with the token, 1: title contains it, 2: only the album matches.
        assertEquals(listOf(3L, 1L, 2L), results.tracks.map { it.id })
    }

    @Test
    fun `all tokens must match the combined haystack`() {
        val results = index.search("miles kind")
        assertEquals(listOf(1L, 2L), results.tracks.map { it.id })
    }

    @Test
    fun `album matches are grouped separately`() {
        val results = index.search("time out")
        assertEquals(listOf(11L), results.albums.map { it.id })
        assertEquals(0, results.artists.size)
    }

    @Test
    fun `artist matches are returned`() {
        val results = index.search("brubeck")
        assertEquals(1, results.artists.size)
        assertEquals(2, results.tracks.size)
    }

    @Test
    fun `limit caps the number of results`() {
        val results = index.search("e", limit = 2)
        assertEquals(2, results.tracks.size)
    }

    @Test
    fun `no match returns empty results`() {
        assertTrue(index.search("zzzz").isEmpty)
    }

    @Test
    fun `tokenizer lowercases and splits on whitespace`() {
        assertEquals(listOf("kind", "of", "blue"), SearchIndex.tokenize("  Kind   OF blue "))
    }
}
