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
        decode(preferences[KEY_RECENT_TRACK_IDS])
    }

    suspend fun record(trackId: Long) {
        if (trackId <= 0L) return
        dataStore.edit { preferences ->
            val updated = RecentPlayedList.updated(
                current = decode(preferences[KEY_RECENT_TRACK_IDS]),
                trackId = trackId,
                limit = MAX_ENTRIES,
            )
            preferences[KEY_RECENT_TRACK_IDS] = updated.joinToString(SEPARATOR)
        }
    }

    companion object {
        const val MAX_ENTRIES = 50
        private const val SEPARATOR = ","
        private val KEY_RECENT_TRACK_IDS = stringPreferencesKey("recent_track_ids")

        fun decode(raw: String?): List<Long> =
            raw.orEmpty()
                .split(SEPARATOR)
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it > 0L }
    }
}

/** Pure list maths for the history, so it can be tested without DataStore. */
object RecentPlayedList {

    /** Moves [trackId] to the front, de-duplicates and trims to [limit]. */
    fun updated(current: List<Long>, trackId: Long, limit: Int = RecentPlayedStore.MAX_ENTRIES): List<Long> {
        if (trackId <= 0L) return current
        if (limit <= 1) return listOf(trackId)
        val result = ArrayList<Long>(minOf(current.size + 1, limit))
        result += trackId
        for (id in current) {
            if (id == trackId || id <= 0L) continue
            if (result.size >= limit) break
            result += id
        }
        return result
    }
}
