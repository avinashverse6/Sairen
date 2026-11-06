package com.sairen.player

import android.content.Context
import com.sairen.player.data.MediaStoreDataSource
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.MusicRepository
import com.sairen.player.data.PlaybackSessionStore
import com.sairen.player.data.RecentPlayedStore
import com.sairen.player.data.SettingsStore
import com.sairen.player.data.sairenDataStore
import com.sairen.player.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection.
 *
 * One object graph for the process, created eagerly in [SairenApplication] but
 * cheap: no I/O, no MediaStore query and no MediaController connection happen
 * during construction, which keeps cold start fast. Hilt would add a compiler,
 * an extra Gradle plugin and runtime startup cost for an app with exactly one
 * activity, one service and a handful of ViewModels.
 */
class AppContainer(baseContext: Context) {

    private val appContext = baseContext.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsStore = SettingsStore(appContext.sairenDataStore)
    val recentPlayedStore = RecentPlayedStore(appContext.sairenDataStore)
    val playbackSessionStore = PlaybackSessionStore(appContext.sairenDataStore)

    val musicLibrary: MusicLibrary = MusicRepository(
        dataSource = MediaStoreDataSource(appContext),
        recentPlayedStore = recentPlayedStore,
        scope = appScope,
    )

    /** Created on first use: connecting a MediaController starts the service. */
    val playerConnection: PlayerConnection by lazy { PlayerConnection(appContext) }

}
