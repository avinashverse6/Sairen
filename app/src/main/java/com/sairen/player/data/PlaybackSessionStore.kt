package com.sairen.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaybackSessionStore(private val dataStore: DataStore<Preferences>) {

    val savedSession: Flow<SavedSession?> = dataStore.data.map { preferences ->
        SavedSessionCodec.decode(preferences[KEY_SESSION])
    }

    suspend fun save(session: SavedSession) {
        if (session.queueIds.isEmpty()) return
        dataStore.edit { preferences ->
            preferences[KEY_SESSION] = SavedSessionCodec.encode(session)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(KEY_SESSION) }
    }

    private companion object {
        val KEY_SESSION = stringPreferencesKey("playback_session")
    }
}
