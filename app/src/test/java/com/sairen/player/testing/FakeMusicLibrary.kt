package com.sairen.player.testing

import com.sairen.player.data.LibraryGrouping
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.RecentPlayedList
import com.sairen.player.data.model.LibrarySnapshot
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMusicLibrary(
    initialTracks: List<Track> = emptyList(),
    initialRecentIds: List<Long> = emptyList(),
    loading: Boolean = false,
) : MusicLibrary {

    private val stateFlow = MutableStateFlow(
        LibraryState(
            isLoading = loading,
            hasLoadedOnce = !loading,
            library = LibrarySnapshot(tracks = initialTracks),
        ),
    )
    override val state: StateFlow<LibraryState> = stateFlow.asStateFlow()

    private val recentFlow = MutableStateFlow(initialRecentIds)
    override val recentlyPlayedIds: StateFlow<List<Long>> = recentFlow.asStateFlow()

    var refreshCount: Int = 0
        private set

    /** When set, the next refresh publishes this snapshot (simulating a MediaStore scan). */
    var tracksOnRefresh: List<Track>? = null

    override suspend fun refresh() {
        refreshCount++
        tracksOnRefresh?.let { scanned ->
            stateFlow.value = LibraryState(
                isLoading = false,
                hasLoadedOnce = true,
                library = LibraryGrouping.snapshot(scanned),
            )
        }
    }

    override suspend fun recordPlayed(trackId: Long) {
        recentFlow.value = RecentPlayedList.updated(recentFlow.value, trackId)
    }

    override suspend fun tracksByIds(ids: List<Long>): List<Track> {
        val index = stateFlow.value.library.tracks.associateBy { it.id }
        return ids.mapNotNull { index[it] }
    }

    fun setLibrary(tracks: List<Track>) {
        stateFlow.value = LibraryState(
            isLoading = false,
            hasLoadedOnce = true,
            library = LibraryGrouping.snapshot(tracks),
        )
    }

    fun setRecentIds(ids: List<Long>) {
        recentFlow.value = ids
    }
}
