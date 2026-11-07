package com.sairen.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.Playlist
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the library scan state, the recent history projection and the user's own
 * data (likes, play counts, playlists).
 *
 * The scan itself runs in the repository on IO/Default dispatchers; this class
 * only triggers it and maps ids back to tracks, so it stays unit testable with a
 * fake [MusicLibrary]. Grouping work is keyed to the *track list* only, so a new
 * entry in the recent history no longer rebuilds the album and artist maps.
 */
class LibraryViewModel(
    private val musicLibrary: MusicLibrary,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    val state: StateFlow<LibraryState> = musicLibrary.state

    val likedIds: StateFlow<Set<Long>> = musicLibrary.likedIds
    val playCounts: StateFlow<Map<Long, Int>> = musicLibrary.playCounts
    val playlists: StateFlow<List<Playlist>> = musicLibrary.playlists

    private val recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = recentTracks.asStateFlow()

    /** Song lists for detail screens, resolved from the in-memory snapshot. */
    val tracksByAlbum = MutableStateFlow<Map<Long, List<Track>>>(emptyMap())
    val tracksByArtist = MutableStateFlow<Map<String, List<Track>>>(emptyMap())

    /** Id → track index, used to resolve playlist ids (missing ids are skipped). */
    private val tracksById = MutableStateFlow<Map<Long, Track>>(emptyMap())

    val tracks: StateFlow<Map<Long, Track>> = tracksById.asStateFlow()

    init {
        viewModelScope.launch {
            musicLibrary.state
                .map { it.library.tracks }
                .distinctUntilChanged()
                .collect { tracks -> recomputeGroupings(tracks) }
        }
        viewModelScope.launch {
            combine(
                musicLibrary.state.map { it.library.tracks }.distinctUntilChanged(),
                musicLibrary.recentlyPlayedIds,
            ) { tracks, ids -> tracks to ids }
                .collect { (tracks, ids) -> resolveRecent(tracks, ids) }
        }
        refresh()
    }

    private suspend fun recomputeGroupings(tracks: List<Track>) {
        val byId = withContext(computeDispatcher) { tracks.associateBy { it.id } }
        val byAlbum = withContext(computeDispatcher) {
            tracks.groupBy { it.albumId }
                .mapValues { (_, albumTracks) -> albumTracks.sortedBy { it.trackNumber } }
        }
        val byArtist = withContext(computeDispatcher) { tracks.groupBy { it.artist } }
        tracksById.value = byId
        tracksByAlbum.value = byAlbum
        tracksByArtist.value = byArtist
    }

    private suspend fun resolveRecent(tracks: List<Track>, ids: List<Long>) {
        recentTracks.value = withContext(computeDispatcher) {
            if (ids.isEmpty()) {
                emptyList()
            } else {
                val byId = tracks.associateBy { it.id }
                ids.mapNotNull { byId[it] }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { musicLibrary.refresh() }
    }

    fun onScanRequested() = refresh()

    // ------------------------------------------------------------------ likes

    fun toggleLike(trackId: Long) {
        viewModelScope.launch { musicLibrary.toggleLike(trackId) }
    }

    // -------------------------------------------------------------- playlists

    fun createPlaylist(name: String) {
        viewModelScope.launch { musicLibrary.createPlaylist(name) }
    }

    /** Creates a playlist and immediately adds [trackId] to it. */
    fun createPlaylistWithTrack(name: String, trackId: Long) {
        viewModelScope.launch {
            val playlistId = musicLibrary.createPlaylist(name) ?: return@launch
            musicLibrary.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun renamePlaylist(playlistId: String, name: String) {
        viewModelScope.launch { musicLibrary.renamePlaylist(playlistId, name) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { musicLibrary.deletePlaylist(playlistId) }
    }

    fun addToPlaylist(playlistId: String, trackId: Long) {
        viewModelScope.launch { musicLibrary.addTrackToPlaylist(playlistId, trackId) }
    }

    fun removeFromPlaylist(playlistId: String, index: Int) {
        viewModelScope.launch { musicLibrary.removeTrackFromPlaylist(playlistId, index) }
    }

    fun movePlaylistTrack(playlistId: String, index: Int, offset: Int) {
        viewModelScope.launch { musicLibrary.movePlaylistTrack(playlistId, index, offset) }
    }

    companion object {
        fun factory(
            musicLibrary: MusicLibrary,
            computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { LibraryViewModel(musicLibrary, computeDispatcher) }
        }
    }
}
