package com.sairen.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Recently played history, kept in DataStore instead of Room.
 *
 * Rationale: the payload is a short list of MediaStore ids. A Room database
 * (plus KSP, an annotation processor and a schema) is far heavier than one
 * preferences key, and losing the history is harmless if the file is wiped.
 */
class RecentPlayedStore(private val dataStore: DataStore<Preferences>) {

    val trackIds: Flow<List<Long>> = dataStore.data.map { preferences ->
        RecentPlayedList.decode(preferences[KEY_RECENT_TRACK_IDS])
    }

    suspend fun record(trackId: Long) {
        if (trackId <= 0L) return
        dataStore.edit { preferences ->
            val updated = RecentPlayedList.updated(
                current = RecentPlayedList.decode(preferences[KEY_RECENT_TRACK_IDS]),
                trackId = trackId,
                limit = RecentPlayedList.DEFAULT_LIMIT,
            )
            preferences[KEY_RECENT_TRACK_IDS] = RecentPlayedList.encode(updated)
        }
    }

    private companion object {
        val KEY_RECENT_TRACK_IDS = stringPreferencesKey("recent_track_ids")
    }
}
