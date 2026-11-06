package com.sairen.player.ui.settings

import com.sairen.player.data.ThemeMode
import com.sairen.player.data.ThemePreferences
import com.sairen.player.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeThemePreferences(initial: ThemeMode) : ThemePreferences {
        private val state = MutableStateFlow(initial)
        var writes: Int = 0
            private set

        override val themeMode: Flow<ThemeMode> = state

        override suspend fun setThemeMode(mode: ThemeMode) {
            writes++
            state.value = mode
        }
    }

    @Test
    fun `theme preference is exposed as state`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val preferences = FakeThemePreferences(ThemeMode.DARK)
        val viewModel = SettingsViewModel(preferences)

        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `selecting a theme persists it`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val preferences = FakeThemePreferences(ThemeMode.SYSTEM)
        val viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
        assertEquals(1, preferences.writes)
    }

    @Test
    fun `selecting the active theme does not write again`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val preferences = FakeThemePreferences(ThemeMode.LIGHT)
        val viewModel = SettingsViewModel(preferences)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(0, preferences.writes)
    }
}
