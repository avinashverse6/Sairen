package com.sairen.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sairen.player.data.ThemeMode
import com.sairen.player.data.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferences: ThemePreferences) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeMode.SYSTEM,
    )

    fun setThemeMode(mode: ThemeMode) {
        if (themeMode.value == mode) return
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    companion object {
        fun factory(preferences: ThemePreferences): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(preferences) }
        }
    }
}
