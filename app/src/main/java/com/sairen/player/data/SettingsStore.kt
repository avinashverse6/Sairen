package com.sairen.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore-backed [ThemePreferences] implementation. */
class SettingsStore(private val dataStore: DataStore<Preferences>) : ThemePreferences {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorage(preferences[KEY_THEME_MODE])
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY_THEME_MODE] = mode.storageValue }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
