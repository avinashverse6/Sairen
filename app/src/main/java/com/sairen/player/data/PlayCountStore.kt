package com.sairen.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Per-track play counts used by the "Most played" and "Not played" collections. */
interface PlayCountsStore {

    val playCounts: Flow<Map<Long, Int>>

    /** Adds one play for [trackId]; callers decide *when* a listen counts. */
    suspend fun incrementPlayCount(trackId: Long)
}

class PlayCountStore(private val dataStore: DataStore<Preferences>) : PlayCountsStore {

    override val playCounts: Flow<Map<Long, Int>> = dataStore.data.map { preferences ->
        PlayCounts.decode(preferences[KEY_PLAY_COUNTS])
    }

    override suspend fun incrementPlayCount(trackId: Long) {
        if (trackId <= 0L) return
        dataStore.edit { preferences ->
            val updated = PlayCounts.increment(
                counts = PlayCounts.decode(preferences[KEY_PLAY_COUNTS]),
                trackId = trackId,
            )
            preferences[KEY_PLAY_COUNTS] = PlayCounts.encode(updated)
        }
    }

    private companion object {
        val KEY_PLAY_COUNTS = stringPreferencesKey("play_counts")
    }
}
