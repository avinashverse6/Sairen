package com.sairen.player.data

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the persisted theme so ViewModels can be unit tested without
 * a real DataStore file.
 */
interface ThemePreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
