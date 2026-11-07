package com.sairen.player.ui.library

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

/**
 * ViewModel-level coverage for the three new user-data features: likes, play
 * counts and playlists (including the reorder/remove edge cases).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryActionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tracks = listOf(
        track(1, "First"),
        track(2, "Second"),
        track(3, "Third"),
    )

    private fun viewModel(library: FakeMusicLibrary) =
        LibraryViewModel(library, mainDispatcherRule.dispatcher)

    @Test
    fun `liking a track exposes it and unliking removes it`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val library = FakeMusicLibrary(initialTracks = tracks)
            val viewModel = viewModel(library)
            advanceUntilIdle()

            viewModel.toggleLike(2L)
            advanceUntilIdle()
            assertEquals(setOf(2L), viewModel.likedIds.value)

            viewModel.toggleLike(2L)
            advanceUntilIdle()
            assertTrue(viewModel.likedIds.value.isEmpty())
        }

    @Test
    fun `likes survive an unrelated library refresh`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val library = FakeMusicLibrary(initialTracks = tracks, initialLikedIds = setOf(1L, 3L))
            val viewModel = viewModel(library)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(setOf(1L, 3L), viewModel.likedIds.value)
        }

    @Test
    fun `completed plays are counted once per call`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val library = FakeMusicLibrary(initialTracks = tracks)
            val viewModel = viewModel(library)
            advanceUntilIdle()

            library.incrementPlayCount(1L)
            library.incrementPlayCount(1L)
            library.incrementPlayCount(3L)
            advanceUntilIdle()

            assertEquals(mapOf(1L to 2, 3L to 1), viewModel.playCounts.value)
        }

    @Test
    fun `creating a playlist appends it`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks)
        val viewModel = viewModel(library)
        advanceUntilIdle()

        viewModel.createPlaylist("  Morning run  ")
        advanceUntilIdle()

        assertEquals(1, viewModel.playlists.value.size)
        assertEquals("Morning run", viewModel.playlists.value.single().name)
    }

    @Test
    fun `blank playlist names are rejected`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks)
        val viewModel = viewModel(library)
        advanceUntilIdle()

        viewModel.createPlaylist("   ")
        advanceUntilIdle()

        assertTrue(viewModel.playlists.value.isEmpty())
    }

    @Test
    fun `renaming and deleting a playlist`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks)
        val viewModel = viewModel(library)
        advanceUntilIdle()
        viewModel.createPlaylist("First name")
        advanceUntilIdle()
        val id = viewModel.playlists.value.single().id

        viewModel.renamePlaylist(id, "Second name")
        advanceUntilIdle()
        assertEquals("Second name", viewModel.playlists.value.single().name)

        viewModel.deletePlaylist(id)
        advanceUntilIdle()
        assertTrue(viewModel.playlists.value.isEmpty())
    }

    @Test
    fun `adding, reordering and removing playlist tracks`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val library = FakeMusicLibrary(initialTracks = tracks)
            val viewModel = viewModel(library)
            advanceUntilIdle()
            viewModel.createPlaylist("Mix")
            advanceUntilIdle()
            val id = viewModel.playlists.value.single().id

            viewModel.addToPlaylist(id, 1L)
            viewModel.addToPlaylist(id, 2L)
            viewModel.addToPlaylist(id, 3L)
            viewModel.addToPlaylist(id, 2L) // duplicate: ignored
            advanceUntilIdle()
            assertEquals(listOf(1L, 2L, 3L), viewModel.playlists.value.single().trackIds)

            viewModel.movePlaylistTrack(id, 2, -1)
            advanceUntilIdle()
            assertEquals(listOf(1L, 3L, 2L), viewModel.playlists.value.single().trackIds)

            viewModel.removeFromPlaylist(id, 0)
            advanceUntilIdle()
            assertEquals(listOf(3L, 2L), viewModel.playlists.value.single().trackIds)
        }

    @Test
    fun `out-of-range playlist edits are ignored`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val library = FakeMusicLibrary(initialTracks = tracks)
        val viewModel = viewModel(library)
        advanceUntilIdle()
        viewModel.createPlaylist("Mix")
        advanceUntilIdle()
        val id = viewModel.playlists.value.single().id

        viewModel.addToPlaylist(id, 1L)
        advanceUntilIdle()

        viewModel.movePlaylistTrack(id, 0, -1) // already at the top
        viewModel.removeFromPlaylist(id, 5) // out of range
        advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.playlists.value.single().trackIds)
    }

    @Test
    fun `playlist tracks keep playlist order, not library order`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val library = FakeMusicLibrary(initialTracks = tracks)
            val viewModel = viewModel(library)
            advanceUntilIdle()
            viewModel.createPlaylist("Reverse")
            advanceUntilIdle()
            val id = viewModel.playlists.value.single().id

            viewModel.addToPlaylist(id, 3L)
            viewModel.addToPlaylist(id, 1L)
            advanceUntilIdle()

            val playlist = viewModel.playlists.value.single()
            val resolved = playlist.trackIds.mapNotNull { viewModel.tracks.value[it] }
            assertEquals(listOf("Third", "First"), resolved.map { it.title })
        }
}
