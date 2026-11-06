package com.sairen.player.data

import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * The single seam between the UI/playback layers and the device library.
 * ViewModels depend on this interface, never on [MusicRepository], so they can
 * be unit tested with a fake that has no Android dependencies.
 */
interface MusicLibrary {

    val state: StateFlow<LibraryState>

    /** MediaStore ids of the most recently played tracks, newest first. */
    val recentlyPlayedIds: StateFlow<List<Long>>

    /** Re-scans MediaStore. Safe to call repeatedly. */
    suspend fun refresh()

    suspend fun recordPlayed(trackId: Long)

    /** Resolves ids to tracks, preserving the given order and skipping unknown ids. */
    suspend fun tracksByIds(ids: List<Long>): List<Track>
}
