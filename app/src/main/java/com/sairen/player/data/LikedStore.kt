package com.sairen.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Liked songs, keyed by MediaStore id. */
interface LikedTracksStore {

    val likedIds: Flow<Set<Long>>

    /** Adds the track when absent, removes it when present. */
    suspend fun toggleLike(trackId: Long)
}

class LikedStore(private val dataStore: DataStore<Preferences>) : LikedTracksStore {

    override val likedIds: Flow<Set<Long>> = dataStore.data.map { preferences ->
        LikedTracks.decode(preferences[KEY_LIKED_IDS])
    }

    override suspend fun toggleLike(trackId: Long) {
        if (trackId <= 0L) return
        dataStore.edit { preferences ->
            val updated = LikedTracks.toggle(
                ids = LikedTracks.decode(preferences[KEY_LIKED_IDS]),
                trackId = trackId,
            )
            preferences[KEY_LIKED_IDS] = LikedTracks.encode(updated)
        }
    }

    private companion object {
        val KEY_LIKED_IDS = stringPreferencesKey("liked_track_ids")
    }
}
