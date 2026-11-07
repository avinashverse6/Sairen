package com.sairen.player.data

import com.sairen.player.testing.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongCollectionsTest {

    private val alpha = track(1, "Alpha").copy(dateAddedMs = 1_000L)
    private val bravo = track(2, "Bravo").copy(dateAddedMs = 3_000L)
    private val charlie = track(3, "Charlie").copy(dateAddedMs = 2_000L)
    private val delta = track(4, "Delta").copy(dateAddedMs = 4_000L)

    private val library = listOf(alpha, bravo, charlie, delta)

    private val counts = mapOf(1L to 5, 2L to 2, 4L to 2)

    @Test
    fun `all keeps library order`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), SongCollections.tracks(SongCollection.ALL, library).map { it.id })
    }

    @Test
    fun `liked keeps only liked ids in library order`() {
        val liked = SongCollections.tracks(
            SongCollection.LIKED,
            library,
            likedIds = setOf(4L, 1L),
        )
        assertEquals(listOf(1L, 4L), liked.map { it.id })
    }

    @Test
    fun `liked with no likes is empty`() {
        assertTrue(SongCollections.tracks(SongCollection.LIKED, library).isEmpty())
    }

    @Test
    fun `recently added is newest first`() {
        val recent = SongCollections.tracks(SongCollection.RECENTLY_ADDED, library)
        assertEquals(listOf(4L, 2L, 3L, 1L), recent.map { it.id })
    }

    @Test
    fun `recently added is capped`() {
        val capped = SongCollections.tracks(SongCollection.RECENTLY_ADDED, library, recentlyAddedLimit = 2)
        assertEquals(listOf(4L, 2L), capped.map { it.id })
    }

    @Test
    fun `most played hides unplayed tracks and sorts by count`() {
        val mostPlayed = SongCollections.tracks(SongCollection.MOST_PLAYED, library, playCounts = counts)
        // 1 has five plays; 2 and 4 tie on two plays and break ties by title.
        assertEquals(listOf(1L, 2L, 4L), mostPlayed.map { it.id })
    }

    @Test
    fun `most played with no counts is empty`() {
        assertTrue(SongCollections.tracks(SongCollection.MOST_PLAYED, library).isEmpty())
    }

    @Test
    fun `recently played follows history order and skips unknown ids`() {
        val recent = SongCollections.tracks(
            SongCollection.RECENTLY_PLAYED,
            library,
            recentIds = listOf(3L, 999L, 1L, 3L),
        )
        assertEquals(listOf(3L, 1L), recent.map { it.id })
    }

    @Test
    fun `not played keeps zero-count tracks`() {
        val notPlayed = SongCollections.tracks(SongCollection.NOT_PLAYED, library, playCounts = counts)
        assertEquals(listOf(3L), notPlayed.map { it.id })
    }

    @Test
    fun `not played is the whole library when nothing has been played`() {
        assertEquals(library.map { it.id }, SongCollections.tracks(SongCollection.NOT_PLAYED, library).map { it.id })
    }

    @Test
    fun `every collection handles an empty library`() {
        for (collection in SongCollection.entries) {
            assertTrue(
                "collection $collection should be empty",
                SongCollections.tracks(collection, emptyList(), setOf(1L), mapOf(1L to 3), listOf(1L)).isEmpty(),
            )
        }
    }

    @Test
    fun `display order exposes all six collections once`() {
        assertEquals(SongCollection.entries.toSet(), SongCollections.displayOrder.toSet())
        assertEquals(SongCollection.ALL, SongCollections.displayOrder.first())
        assertEquals(SongCollection.NOT_PLAYED, SongCollections.displayOrder.last())
    }
}
