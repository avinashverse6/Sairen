package com.sairen.player.data

import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * The single seam between the UI/playback layers and the device library.
 * ViewModels depend on this interface, never on [MusicRepository], so they can
 * be unit tested with a fake that has no Android dependencies.
 *
 * It also owns the user's own data about the library — liked ids, play counts
 * and playlists — because those are keyed by MediaStore id and change together
 * with the scan snapshot.
 */
interface MusicLibrary : LikedTracksStore, PlayCountsStore, PlaylistsStore {

    val state: StateFlow<LibraryState>

    /** MediaStore ids of the most recently played tracks, newest first. */
    val recentlyPlayedIds: StateFlow<List<Long>>

    /** Re-scans MediaStore. Safe to call repeatedly. */
    suspend fun refresh()

    suspend fun recordPlayed(trackId: Long)

    /**
     * Adds one play to the persistent counter. Called by the playback host once
     * per listen, after [PlayCountRules] says the listen counted.
     */
    suspend fun recordCompletedPlay(trackId: Long)

    /** Resolves ids to tracks, preserving the given order and skipping unknown ids. */
    suspend fun tracksByIds(ids: List<Long>): List<Track>
}
