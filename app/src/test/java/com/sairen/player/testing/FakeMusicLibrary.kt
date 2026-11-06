package com.sairen.player.testing

import com.sairen.player.data.LibraryGrouping
import com.sairen.player.data.LikedTracks
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.PlayCounts
import com.sairen.player.data.Playlist
import com.sairen.player.data.PlaylistEditor
import com.sairen.player.data.RecentPlayedList
import com.sairen.player.data.model.LibrarySnapshot
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [MusicLibrary] for JVM tests: no MediaStore, no DataStore, no Android.
 * Mutations go through the same pure helpers the real stores use, so the ViewModel
 * sees exactly what production would produce.
 */
class FakeMusicLibrary(
    initialTracks: List<Track> = emptyList(),
    initialRecentIds: List<Long> = emptyList(),
    loading: Boolean = false,
    initialLikedIds: Set<Long> = emptySet(),
    initialPlayCounts: Map<Long, Int> = emptyMap(),
    initialPlaylists: List<Playlist> = emptyList(),
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

    private val likedFlow = MutableStateFlow(initialLikedIds)
    override val likedIds: StateFlow<Set<Long>> = likedFlow.asStateFlow()

    private val countsFlow = MutableStateFlow(initialPlayCounts)
    override val playCounts: StateFlow<Map<Long, Int>> = countsFlow.asStateFlow()

    private val playlistsFlow = MutableStateFlow(initialPlaylists)
    override val playlists: StateFlow<List<Playlist>> = playlistsFlow.asStateFlow()

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

    override suspend fun incrementPlayCount(trackId: Long) {
        countsFlow.value = PlayCounts.increment(countsFlow.value, trackId)
    }

    override suspend fun tracksByIds(ids: List<Long>): List<Track> {
        val index = stateFlow.value.library.tracks.associateBy { it.id }
        return ids.mapNotNull { index[it] }
    }

    override suspend fun toggleLike(trackId: Long) {
        likedFlow.value = LikedTracks.toggle(likedFlow.value, trackId)
    }

    override suspend fun createPlaylist(name: String): String? {
        if (name.isBlank()) return null
        val id = "playlist-${playlistsFlow.value.size + 1}"
        playlistsFlow.value = PlaylistEditor.create(playlistsFlow.value, id = id, name = name)
        return id
    }

    override suspend fun renamePlaylist(playlistId: String, name: String) {
        playlistsFlow.value = PlaylistEditor.rename(playlistsFlow.value, playlistId, name)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistsFlow.value = PlaylistEditor.delete(playlistsFlow.value, playlistId)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        playlistsFlow.value = PlaylistEditor.addTrack(playlistsFlow.value, playlistId, trackId)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, index: Int) {
        playlistsFlow.value = PlaylistEditor.removeAt(playlistsFlow.value, playlistId, index)
    }

    override suspend fun movePlaylistTrack(playlistId: String, index: Int, offset: Int) {
        playlistsFlow.value = PlaylistEditor.move(playlistsFlow.value, playlistId, index, offset)
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
