package com.sairen.player.ui.search

import com.sairen.player.testing.FakeMusicLibrary
import com.sairen.player.testing.MainDispatcherRule
import com.sairen.player.testing.track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val library = FakeMusicLibrary(
        initialTracks = listOf(
            track(1, "Kind of Blue", artist = "Miles Davis"),
            track(2, "Take Five", artist = "Dave Brubeck"),
        ),
    )

    @Test
    fun `typing is debounced before scanning`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = SearchViewModel(
            musicLibrary = library,
            debounceMs = 300L,
            computeDispatcher = mainDispatcherRule.dispatcher,
        )

        viewModel.onQueryChange("kind")
        val query = viewModel.query
        assertEquals("kind", query.value)

        advanceTimeBy(200L)
        runCurrent()
        // Still inside the debounce window: no scan yet.
        assertTrue(viewModel.results.value.tracks.isEmpty())

        advanceTimeBy(150L)
        runCurrent()
        assertEquals(listOf(1L), viewModel.results.value.tracks.map { it.id })
        assertFalse(viewModel.isSearching.value)
    }

    @Test
    fun `rapid typing only scans the final text`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = SearchViewModel(
            musicLibrary = library,
            debounceMs = 300L,
            computeDispatcher = mainDispatcherRule.dispatcher,
        )

        viewModel.onQueryChange("k")
        advanceTimeBy(100L)
        viewModel.onQueryChange("ki")
        advanceTimeBy(100L)
        viewModel.onQueryChange("kind")
        advanceTimeBy(400L)
        runCurrent()

        assertEquals(listOf(1L), viewModel.results.value.tracks.map { it.id })
    }

    @Test
    fun `clearing the query resets results immediately`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = SearchViewModel(
            musicLibrary = library,
            debounceMs = 300L,
            computeDispatcher = mainDispatcherRule.dispatcher,
        )
        viewModel.onQueryChange("blue")
        advanceTimeBy(400L)
        runCurrent()
        assertTrue(viewModel.results.value.tracks.isNotEmpty())

        viewModel.clear()
        runCurrent()
        assertTrue(viewModel.results.value.tracks.isEmpty())
    }

    @Test
    fun `library updates are searchable after being emitted`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = SearchViewModel(
            musicLibrary = library,
            debounceMs = 0L,
            computeDispatcher = mainDispatcherRule.dispatcher,
        )
        viewModel.onQueryChange("take")
        advanceUntilIdle()
        assertEquals(listOf(2L), viewModel.results.value.tracks.map { it.id })
    }
}
