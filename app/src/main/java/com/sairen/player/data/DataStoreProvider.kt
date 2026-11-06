package com.sairen.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * One process-wide Preferences DataStore file. Every store in this package
 * (settings, recently played, playback session) is a thin typed facade over it,
 * which keeps a single writer and avoids "multiple DataStore instances" crashes.
 */
val Context.sairenDataStore: DataStore<Preferences> by preferencesDataStore(name = "sairen_prefs")
