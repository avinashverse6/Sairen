package com.sairen.player.data

import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Default [MusicLibrary] implementation: a lazily scanned, in-memory MediaStore
 * index plus the persisted user data (history, likes, play counts, playlists).
 *
 * The scan is memoised in memory (never on disk), so a warm start is instant
 * and an explicit refresh is the only way to pick up new files. The scan is
 * intentionally silent: it publishes [state] when it is done, and nothing in the
 * UI blocks on it.
 */
class MusicRepository(
    private val dataSource: MediaStoreDataSource,
    private val recentPlayedStore: RecentPlayedStore,
    private val likedStore: LikedTracksStore,
    private val playCountStore: PlayCountsStore,
    private val playlistStore: PlaylistsStore,
    private val scope: CoroutineScope,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MusicLibrary {

    private val _state = MutableStateFlow(LibraryState())
    override val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _recentlyPlayedIds = MutableStateFlow<List<Long>>(emptyList())
    override val recentlyPlayedIds: StateFlow<List<Long>> = _recentlyPlayedIds.asStateFlow()

    private val _likedIds = MutableStateFlow<Set<Long>>(emptySet())
    override val likedIds: StateFlow<Set<Long>> = _likedIds.asStateFlow()

    private val _playCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    override val playCounts: StateFlow<Map<Long, Int>> = _playCounts.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    override val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val loadMutex = Mutex()

    @Volatile
    private var trackIndex: Map<Long, Track> = emptyMap()

    init {
        scope.launch {
            recentPlayedStore.trackIds
                .catch { emit(emptyList()) }
                .collect { ids -> _recentlyPlayedIds.value = ids }
        }
        scope.launch {
            likedStore.likedIds
                .catch { emit(emptySet()) }
                .collect { ids -> _likedIds.value = ids }
        }
        scope.launch {
            playCountStore.playCounts
                .catch { emit(emptyMap()) }
                .collect { counts -> _playCounts.value = counts }
        }
        scope.launch {
            playlistStore.playlists
                .catch { emit(emptyList()) }
                .collect { playlists -> _playlists.value = playlists }
        }
    }

    override suspend fun refresh() {
        loadMutex.withLock {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { dataSource.loadTracks() }
                .onSuccess { tracks ->
                    val snapshot = withContext(computeDispatcher) { LibraryGrouping.snapshot(tracks) }
                    trackIndex = tracks.associateBy { it.id }
                    _state.value = LibraryState(
                        isLoading = false,
                        hasLoadedOnce = true,
                        library = snapshot,
                        errorMessage = null,
                    )
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasLoadedOnce = true,
                            errorMessage = error.message ?: "MediaStore query failed",
                        )
                    }
                }
        }
    }

    override suspend fun recordPlayed(trackId: Long) {
        if (trackId <= 0L) return
        runCatching { recentPlayedStore.record(trackId) }
    }

    override suspend fun recordCompletedPlay(trackId: Long) {
        if (trackId <= 0L) return
        runCatching { playCountStore.incrementPlayCount(trackId) }
    }

    override suspend fun tracksByIds(ids: List<Long>): List<Track> {
        if (ids.isEmpty()) return emptyList()
        val index = trackIndex
        return ids.mapNotNull { index[it] }
    }

    // ------------------------------------------------------------------ likes

    override suspend fun toggleLike(trackId: Long) {
        if (trackId <= 0L) return
        runCatching { likedStore.toggleLike(trackId) }
    }

    // -------------------------------------------------------------- playlists

    override suspend fun createPlaylist(name: String): String? =
        runCatching { playlistStore.createPlaylist(name) }.getOrNull()

    override suspend fun renamePlaylist(playlistId: String, name: String) {
        if (playlistId.isBlank()) return
        runCatching { playlistStore.renamePlaylist(playlistId, name) }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        runCatching { playlistStore.deletePlaylist(playlistId) }
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        if (playlistId.isBlank() || trackId <= 0L) return
        runCatching { playlistStore.addTrackToPlaylist(playlistId, trackId) }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, index: Int) {
        if (playlistId.isBlank() || index < 0) return
        runCatching { playlistStore.removeTrackFromPlaylist(playlistId, index) }
    }

    override suspend fun movePlaylistTrack(playlistId: String, index: Int, offset: Int) {
        if (playlistId.isBlank() || index < 0 || offset == 0) return
        runCatching { playlistStore.movePlaylistTrack(playlistId, index, offset) }
    }
}
