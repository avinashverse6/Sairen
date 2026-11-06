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
 * index plus the persisted recently-played list.
 *
 * The scan is memoised in memory (never on disk), so a warm start is instant
 * and an explicit refresh is the only way to pick up new files.
 */
class MusicRepository(
    private val dataSource: MediaStoreDataSource,
    private val recentPlayedStore: RecentPlayedStore,
    private val scope: CoroutineScope,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MusicLibrary {

    private val _state = MutableStateFlow(LibraryState())
    override val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _recentlyPlayedIds = MutableStateFlow<List<Long>>(emptyList())
    override val recentlyPlayedIds: StateFlow<List<Long>> = _recentlyPlayedIds.asStateFlow()

    private val loadMutex = Mutex()

    @Volatile
    private var trackIndex: Map<Long, Track> = emptyMap()

    init {
        scope.launch {
            recentPlayedStore.trackIds
                .catch { emit(emptyList()) }
                .collect { ids -> _recentlyPlayedIds.value = ids }
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

    override suspend fun tracksByIds(ids: List<Long>): List<Track> {
        if (ids.isEmpty()) return emptyList()
        val index = trackIndex
        return ids.mapNotNull { index[it] }
    }
}
