package com.sairen.player.ui.library

import com.sairen.player.data.LibraryGrouping
import com.sairen.player.testing.FakeMusicLibrary
import com.sairen.player.testing.MainDispatcherRule
import com.sairen.player.testing.track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tracks = listOf(
        track(1, "First", artist = "Artist A", album = "Album A", albumId = 100, trackNumber = 2),
        track(2, "Second", artist = "Artist A", album = "Album A", albumId = 100, trackNumber = 1),
        track(3, "Third", artist = "Artist B", album = "Album B", albumId = 200, trackNumber = 1),
    )

    @Test
    fun `initial load triggers a library refresh`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks)
        val viewModel = LibraryViewModel(library, mainDispatcherRule.dispatcher)

        advanceUntilIdle()

        assertEquals(1, library.refreshCount)
        assertTrue(viewModel.state.value.hasLoadedOnce)
    }

    @Test
    fun `tracks are grouped by album in track order and by artist`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks)
        val viewModel = LibraryViewModel(library, mainDispatcherRule.dispatcher)

        advanceUntilIdle()

        val albumA = viewModel.tracksByAlbum.value.getValue(100L)
        assertEquals(listOf(2L, 1L), albumA.map { it.id })
        assertEquals(2, viewModel.tracksByArtist.value.getValue("Artist A").size)
        assertEquals(1, viewModel.tracksByArtist.value.getValue("Artist B").size)
    }

    @Test
    fun `recently played ids are resolved to tracks newest first`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks, initialRecentIds = listOf(3L, 1L))
        val viewModel = LibraryViewModel(library, mainDispatcherRule.dispatcher)

        advanceUntilIdle()

        assertEquals(listOf("Third", "First"), viewModel.recentlyPlayed.value.map { it.title })
    }

    @Test
    fun `unknown recent ids are skipped instead of crashing`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks, initialRecentIds = listOf(999L))
        val viewModel = LibraryViewModel(library, mainDispatcherRule.dispatcher)

        advanceUntilIdle()

        assertTrue(viewModel.recentlyPlayed.value.isEmpty())
    }

    @Test
    fun `refresh picks up newly scanned tracks`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = emptyList())
        val viewModel = LibraryViewModel(library, mainDispatcherRule.dispatcher)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.library.isEmpty)

        val scanned = listOf(track(7, "Late arrival"))
        library.tracksOnRefresh = scanned
        library.setLibrary(scanned)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(7L), viewModel.state.value.library.tracks.map { it.id })
    }

    @Test
    fun `grouping produces albums and artists for the snapshot`() {
        val snapshot = LibraryGrouping.snapshot(tracks)
        assertEquals(2, snapshot.albums.size)
        assertEquals(2, snapshot.artists.size)
        assertEquals(3, snapshot.tracks.size)
    }
}
