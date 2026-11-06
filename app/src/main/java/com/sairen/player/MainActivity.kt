package com.sairen.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sairen.player.ui.SairenApp
import com.sairen.player.ui.library.LibraryViewModel
import com.sairen.player.ui.permissions.AudioPermissionGate
import com.sairen.player.ui.search.SearchViewModel
import com.sairen.player.ui.settings.SettingsViewModel
import com.sairen.player.ui.theme.SairenTheme

/**
 * The app's only activity.
 *
 * Order of operations on the main thread is deliberately tiny: read the theme
 * preference, gate on the audio permission, then compose. The MediaStore scan
 * starts inside [LibraryViewModel] on a background dispatcher, so the first frame
 * is not blocked by I/O.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SairenApplication).container

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(container.settingsStore),
            )
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            SairenTheme(themeMode = themeMode) {
                AudioPermissionGate {
                    val libraryViewModel: LibraryViewModel = viewModel(
                        factory = LibraryViewModel.factory(container.musicLibrary),
                    )
                    SairenApp(
                        libraryViewModel = libraryViewModel,
                        settingsViewModel = settingsViewModel,
                        playerConnection = container.playerConnection,
                        searchViewModelFactory = SearchViewModel.factory(container.musicLibrary),
                    )
                }
            }
        }
    }
}
