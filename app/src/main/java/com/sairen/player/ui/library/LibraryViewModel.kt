package com.sairen.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the library scan state and the recently-played projection.
 *
 * The scan itself runs in the repository on IO/Default dispatchers; this class
 * only triggers it and maps ids back to tracks, so it stays unit testable with a
 * fake [MusicLibrary].
 */
class LibraryViewModel(
    private val musicLibrary: MusicLibrary,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    val state: StateFlow<LibraryState> = musicLibrary.state

    private val recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = recentTracks.asStateFlow()

    /** Song lists for detail screens, resolved from the in-memory snapshot. */
    val tracksByAlbum = MutableStateFlow<Map<Long, List<Track>>>(emptyMap())
    val tracksByArtist = MutableStateFlow<Map<String, List<Track>>>(emptyMap())

    init {
        viewModelScope.launch {
            combine(musicLibrary.state, musicLibrary.recentlyPlayedIds) { libraryState, ids ->
                libraryState to ids
            }.collect { (libraryState, ids) ->
                val tracks = libraryState.library.tracks
                recentTracks.value = withContext(computeDispatcher) {
                    if (ids.isEmpty()) {
                        emptyList()
                    } else {
                        val byId = tracks.associateBy { it.id }
                        ids.mapNotNull { byId[it] }
                    }
                }
                tracksByAlbum.value = withContext(computeDispatcher) {
                    tracks.groupBy { it.albumId }
                        .mapValues { (_, albumTracks) ->
                            albumTracks.sortedBy { it.trackNumber }
                        }
                }
                tracksByArtist.value = withContext(computeDispatcher) {
                    tracks.groupBy { it.artist }
                }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { musicLibrary.refresh() }
    }

    fun onScanRequested() = refresh()

    companion object {
        fun factory(
            musicLibrary: MusicLibrary,
            computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { LibraryViewModel(musicLibrary, computeDispatcher) }
        }
    }
}
