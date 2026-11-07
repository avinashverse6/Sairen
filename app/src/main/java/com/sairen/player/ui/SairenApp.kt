package com.sairen.player.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sairen.player.R
import com.sairen.player.data.Playlist
import com.sairen.player.data.ThemeMode
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import com.sairen.player.playback.PlayerConnection
import com.sairen.player.playback.PlayerProgress
import com.sairen.player.playback.PlayerUiState
import com.sairen.player.ui.components.AddToPlaylistDialog
import com.sairen.player.ui.components.MiniPlayer
import com.sairen.player.ui.components.TrackActionsSheet
import com.sairen.player.ui.library.AlbumDetailScreen
import com.sairen.player.ui.library.AlbumsScreen
import com.sairen.player.ui.library.ArtistDetailScreen
import com.sairen.player.ui.library.ArtistsScreen
import com.sairen.player.ui.library.LibraryViewModel
import com.sairen.player.ui.library.PlaylistDetailScreen
import com.sairen.player.ui.library.PlaylistsScreen
import com.sairen.player.ui.library.RecentScreen
import com.sairen.player.ui.library.SongsScreen
import com.sairen.player.ui.navigation.SairenDestinations
import com.sairen.player.ui.permissions.PermissionDecision
import com.sairen.player.ui.permissions.Permissions
import com.sairen.player.ui.player.NowPlayingScreen
import com.sairen.player.ui.player.PlayerActions
import com.sairen.player.ui.player.rememberPlayerActions
import com.sairen.player.ui.search.SearchScreen
import com.sairen.player.ui.search.SearchViewModel
import com.sairen.player.ui.settings.SettingsScreen
import com.sairen.player.ui.settings.SettingsViewModel
import com.sairen.player.ui.util.SairenWindowSize
import com.sairen.player.ui.util.currentWindowSize
import kotlinx.coroutines.flow.StateFlow

/**
 * Root composable: scaffold (top bar, mini player, navigation), the nav graph and
 * the one-off notification permission rationale.
 *
 * Layout adapts by window width class: bottom navigation on phones, a navigation
 * rail on tablets/foldables, and a two-pane Now Playing screen when expanded.
 */
@Composable
fun SairenApp(
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
    playerConnection: PlayerConnection,
    searchViewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val windowSize = currentWindowSize()
    val actions = rememberPlayerActions(playerConnection)

    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val recentTracks by libraryViewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val tracksByAlbum by libraryViewModel.tracksByAlbum.collectAsStateWithLifecycle()
    val tracksByArtist by libraryViewModel.tracksByArtist.collectAsStateWithLifecycle()
    val tracksById by libraryViewModel.tracks.collectAsStateWithLifecycle()
    val likedIds by libraryViewModel.likedIds.collectAsStateWithLifecycle()
    val playCounts by libraryViewModel.playCounts.collectAsStateWithLifecycle()
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val playbackState by playerConnection.state.collectAsStateWithLifecycle()
    val progress = playerConnection.progress
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    var sheetTrack by remember { mutableStateOf<Track?>(null) }
    var playlistPickerTrack by remember { mutableStateOf<Track?>(null) }

    val onToggleLike: (Track) -> Unit = { track -> libraryViewModel.toggleLike(track.id) }
    val currentTrackId = playbackState.currentMediaId
    val isCurrentLiked = currentTrackId != null && currentTrackId in likedIds

    // Notification permission state, refreshed when returning from system settings.
    var notificationsAllowed by remember { mutableStateOf(Permissions.hasNotificationAccess(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { notificationsAllowed = Permissions.hasNotificationAccess(context) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = Permissions.hasNotificationAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val requestNotifications: () -> Unit = {
        if (Permissions.requiresNotificationPermission()) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isNowPlaying = currentRoute == SairenDestinations.NOW_PLAYING

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigateToAlbum: (Long) -> Unit = { albumId ->
        navController.navigate(SairenDestinations.albumDetail(albumId))
    }
    val navigateToArtist: (String) -> Unit = { name ->
        if (name.isNotBlank()) navController.navigate(SairenDestinations.artistDetail(name))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SairenTopBar(
                route = currentRoute,
                onBack = { navController.popBackStack() },
                onSearch = { navController.navigate(SairenDestinations.SEARCH) },
                onSettings = { navController.navigate(SairenDestinations.SETTINGS) },
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = playbackState.hasTrack && !isNowPlaying,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    MiniPlayer(
                        state = playbackState,
                        progress = progress,
                        isLiked = isCurrentLiked,
                        onToggleLike = {
                            currentTrackId?.let(libraryViewModel::toggleLike)
                        },
                        onExpand = { navController.navigate(SairenDestinations.NOW_PLAYING) },
                        onTogglePlayPause = actions.togglePlayPause,
                        onNext = actions.next,
                    )
                }
                if (currentRoute in SairenDestinations.topLevel && !windowSize.isExpanded) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        TopLevelNavItem(
                            label = stringResource(R.string.nav_songs),
                            iconRes = R.drawable.ic_music_note,
                            selected = currentRoute == SairenDestinations.SONGS,
                            onClick = { navigateTopLevel(SairenDestinations.SONGS) },
                        )
                        TopLevelNavItem(
                            label = stringResource(R.string.nav_albums),
                            iconRes = R.drawable.ic_album,
                            selected = currentRoute == SairenDestinations.ALBUMS,
                            onClick = { navigateTopLevel(SairenDestinations.ALBUMS) },
                        )
                        TopLevelNavItem(
                            label = stringResource(R.string.nav_artists),
                            iconRes = R.drawable.ic_artist,
                            selected = currentRoute == SairenDestinations.ARTISTS,
                            onClick = { navigateTopLevel(SairenDestinations.ARTISTS) },
                        )
                        TopLevelNavItem(
                            label = stringResource(R.string.nav_playlists),
                            iconRes = R.drawable.ic_playlist,
                            selected = currentRoute == SairenDestinations.PLAYLISTS,
                            onClick = { navigateTopLevel(SairenDestinations.PLAYLISTS) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (windowSize.isExpanded) {
                SairenNavigationRail(
                    currentRoute = currentRoute,
                    onNavigate = navigateTopLevel,
                    onSearch = { navController.navigate(SairenDestinations.SEARCH) },
                    onSettings = { navController.navigate(SairenDestinations.SETTINGS) },
                )
            }
            SairenNavHost(
                navController = navController,
                innerPadding = innerPadding,
                libraryViewModel = libraryViewModel,
                libraryState = libraryState,
                recentTracks = recentTracks,
                tracksByAlbum = tracksByAlbum,
                tracksByArtist = tracksByArtist,
                tracksById = tracksById,
                likedIds = likedIds,
                playCounts = playCounts,
                playlists = playlists,
                currentTrackId = currentTrackId,
                playbackState = playbackState,
                progress = progress,
                isCurrentLiked = isCurrentLiked,
                onToggleLike = onToggleLike,
                actions = actions,
                windowSize = windowSize,
                themeMode = themeMode,
                settingsViewModel = settingsViewModel,
                searchViewModelFactory = searchViewModelFactory,
                notificationsAllowed = notificationsAllowed,
                onRequestNotifications = requestNotifications,
                onTrackMore = { sheetTrack = it },
                onNavigateToAlbum = navigateToAlbum,
                onNavigateToArtist = navigateToArtist,
            )
        }
    }

    sheetTrack?.let { track ->
        TrackActionsSheet(
            track = track,
            isLiked = track.id in likedIds,
            onDismiss = { sheetTrack = null },
            onPlay = {
                actions.playTracks(listOf(track), 0)
                sheetTrack = null
            },
            onPlayNext = {
                actions.playNext(track)
                sheetTrack = null
            },
            onAddToQueue = {
                actions.addToQueue(track)
                sheetTrack = null
            },
            onToggleLike = { libraryViewModel.toggleLike(track.id) },
            onAddToPlaylist = {
                sheetTrack = null
                playlistPickerTrack = track
            },
            onGoToAlbum = {
                sheetTrack = null
                navigateToAlbum(track.albumId)
            },
            onGoToArtist = {
                sheetTrack = null
                navigateToArtist(track.artist)
            },
        )
    }

    playlistPickerTrack?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { playlistPickerTrack = null },
            onPick = { playlist ->
                libraryViewModel.addToPlaylist(playlist.id, track.id)
                playlistPickerTrack = null
            },
            onCreate = { name ->
                libraryViewModel.createPlaylistWithTrack(name, track.id)
                playlistPickerTrack = null
            },
        )
    }

    var notificationPromptDismissed by rememberSaveable { mutableStateOf(false) }
    if (PermissionDecision.shouldPromptForNotifications(
            requiresRuntimePermission = Permissions.requiresNotificationPermission(),
            hasPermission = notificationsAllowed,
            alreadyPromptedThisSession = notificationPromptDismissed,
        )
    ) {
        NotificationRationaleDialog(
            onResult = {
                notificationPromptDismissed = true
                notificationsAllowed = Permissions.hasNotificationAccess(context)
            },
            onDismiss = { notificationPromptDismissed = true },
        )
    }
}

/**
 * One-time (per process) explanation before the POST_NOTIFICATIONS dialog, so the
 * system prompt is not shown cold with no context.
 */
@Composable
private fun NotificationRationaleDialog(onResult: () -> Unit, onDismiss: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { onResult() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_body)) },
        confirmButton = {
            TextButton(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text(stringResource(R.string.notification_permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_not_now))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SairenTopBar(
    route: String?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    // Search and Now Playing draw their own full-bleed headers.
    if (route == null ||
        route == SairenDestinations.SEARCH ||
        route == SairenDestinations.NOW_PLAYING
    ) {
        return
    }
    val title = when (route) {
        SairenDestinations.SONGS -> stringResource(R.string.nav_songs)
        SairenDestinations.ALBUMS -> stringResource(R.string.nav_albums)
        SairenDestinations.ARTISTS -> stringResource(R.string.nav_artists)
        SairenDestinations.PLAYLISTS -> stringResource(R.string.nav_playlists)
        SairenDestinations.RECENT -> stringResource(R.string.library_recent)
        SairenDestinations.SETTINGS -> stringResource(R.string.settings_title)
        SairenDestinations.ALBUM_DETAIL -> stringResource(R.string.album_detail_title)
        SairenDestinations.ARTIST_DETAIL -> stringResource(R.string.artist_detail_title)
        SairenDestinations.PLAYLIST_DETAIL -> stringResource(R.string.playlists_detail_title)
        else -> stringResource(R.string.app_name)
    }
    val isTopLevel = route in SairenDestinations.topLevel
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        navigationIcon = {
            if (!isTopLevel) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.nav_back),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.nav_search),
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.nav_settings),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun RowScope.TopLevelNavItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        },
        label = { Text(label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SairenNavigationRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(12.dp))
        RailItem(
            label = stringResource(R.string.nav_songs),
            iconRes = R.drawable.ic_music_note,
            selected = currentRoute == SairenDestinations.SONGS,
            onClick = { onNavigate(SairenDestinations.SONGS) },
        )
        RailItem(
            label = stringResource(R.string.nav_albums),
            iconRes = R.drawable.ic_album,
            selected = currentRoute == SairenDestinations.ALBUMS,
            onClick = { onNavigate(SairenDestinations.ALBUMS) },
        )
        RailItem(
            label = stringResource(R.string.nav_artists),
            iconRes = R.drawable.ic_artist,
            selected = currentRoute == SairenDestinations.ARTISTS,
            onClick = { onNavigate(SairenDestinations.ARTISTS) },
        )
        RailItem(
            label = stringResource(R.string.nav_playlists),
            iconRes = R.drawable.ic_playlist,
            selected = currentRoute == SairenDestinations.PLAYLISTS,
            onClick = { onNavigate(SairenDestinations.PLAYLISTS) },
        )
        Spacer(Modifier.weight(1f))
        RailItem(
            label = stringResource(R.string.nav_search),
            iconRes = R.drawable.ic_search,
            selected = false,
            onClick = onSearch,
        )
        RailItem(
            label = stringResource(R.string.nav_settings),
            iconRes = R.drawable.ic_settings,
            selected = false,
            onClick = onSettings,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RailItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        },
        label = { Text(label) },
    )
}

@Suppress("LongParameterList")
@Composable
private fun SairenNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    libraryViewModel: LibraryViewModel,
    libraryState: LibraryState,
    recentTracks: List<Track>,
    tracksByAlbum: Map<Long, List<Track>>,
    tracksByArtist: Map<String, List<Track>>,
    tracksById: Map<Long, Track>,
    likedIds: Set<Long>,
    playCounts: Map<Long, Int>,
    playlists: List<Playlist>,
    currentTrackId: Long?,
    playbackState: PlayerUiState,
    progress: StateFlow<PlayerProgress>,
    isCurrentLiked: Boolean,
    onToggleLike: (Track) -> Unit,
    actions: PlayerActions,
    windowSize: SairenWindowSize,
    themeMode: ThemeMode,
    settingsViewModel: SettingsViewModel,
    searchViewModelFactory: ViewModelProvider.Factory,
    notificationsAllowed: Boolean,
    onRequestNotifications: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
) {
    val bottomInset = innerPadding.calculateBottomPadding()
    val listPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = bottomInset + 12.dp,
    )
    NavHost(
        navController = navController,
        startDestination = SairenDestinations.SONGS,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(SairenDestinations.SONGS) {
            SongsScreen(
                state = libraryState,
                recentTracks = recentTracks,
                likedIds = likedIds,
                playCounts = playCounts,
                currentTrackId = currentTrackId,
                actions = actions,
                contentPadding = listPadding,
                onOpenRecent = { navController.navigate(SairenDestinations.RECENT) },
                onTrackMore = onTrackMore,
                onToggleLike = onToggleLike,
                onRetry = libraryViewModel::refresh,
            )
        }
        composable(SairenDestinations.ALBUMS) {
            AlbumsScreen(
                state = libraryState,
                windowSize = windowSize,
                onAlbumClick = { album -> onNavigateToAlbum(album.id) },
                onRetry = libraryViewModel::refresh,
                contentPadding = listPadding,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        composable(SairenDestinations.ARTISTS) {
            ArtistsScreen(
                state = libraryState,
                onArtistClick = { artist -> onNavigateToArtist(artist.name) },
                onRetry = libraryViewModel::refresh,
                contentPadding = listPadding,
            )
        }
        composable(SairenDestinations.PLAYLISTS) {
            PlaylistsScreen(
                playlists = playlists,
                onOpenPlaylist = { playlist ->
                    navController.navigate(SairenDestinations.playlistDetail(playlist.id))
                },
                onCreatePlaylist = libraryViewModel::createPlaylist,
                onRenamePlaylist = { playlist, name ->
                    libraryViewModel.renamePlaylist(playlist.id, name)
                },
                onDeletePlaylist = { playlist -> libraryViewModel.deletePlaylist(playlist.id) },
                contentPadding = listPadding,
            )
        }
        composable(SairenDestinations.RECENT) {
            RecentScreen(
                recentTracks = recentTracks,
                currentTrackId = currentTrackId,
                likedIds = likedIds,
                actions = actions,
                onTrackMore = onTrackMore,
                onToggleLike = onToggleLike,
                contentPadding = listPadding,
            )
        }
        composable(
            route = SairenDestinations.ALBUM_DETAIL,
            arguments = listOf(
                navArgument(SairenDestinations.ARG_ALBUM_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val albumId = entry.arguments?.getLong(SairenDestinations.ARG_ALBUM_ID) ?: -1L
            AlbumDetailScreen(
                album = libraryState.library.albums.firstOrNull { it.id == albumId },
                tracks = tracksByAlbum[albumId].orEmpty(),
                currentTrackId = currentTrackId,
                likedIds = likedIds,
                actions = actions,
                onTrackMore = onTrackMore,
                onToggleLike = onToggleLike,
                contentPadding = listPadding,
            )
        }
        composable(
            route = SairenDestinations.ARTIST_DETAIL,
            arguments = listOf(
                navArgument(SairenDestinations.ARG_ARTIST_NAME) { type = NavType.StringType },
            ),
        ) { entry ->
            val artistName = entry.arguments?.getString(SairenDestinations.ARG_ARTIST_NAME).orEmpty()
            ArtistDetailScreen(
                artistName = artistName,
                tracks = tracksByArtist[artistName].orEmpty(),
                albums = libraryState.library.albums.filter {
                    it.artist.equals(artistName, ignoreCase = true)
                },
                currentTrackId = currentTrackId,
                likedIds = likedIds,
                actions = actions,
                onAlbumClick = { album -> onNavigateToAlbum(album.id) },
                onTrackMore = onTrackMore,
                onToggleLike = onToggleLike,
                contentPadding = listPadding,
            )
        }
        composable(
            route = SairenDestinations.PLAYLIST_DETAIL,
            arguments = listOf(
                navArgument(SairenDestinations.ARG_PLAYLIST_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val playlistId = entry.arguments?.getString(SairenDestinations.ARG_PLAYLIST_ID).orEmpty()
            PlaylistDetailScreen(
                playlist = playlists.firstOrNull { it.id == playlistId },
                tracksById = tracksById,
                currentTrackId = currentTrackId,
                likedIds = likedIds,
                actions = actions,
                contentPadding = listPadding,
                onToggleLike = onToggleLike,
                onRemove = { index -> libraryViewModel.removeFromPlaylist(playlistId, index) },
                onMove = { index, offset ->
                    libraryViewModel.movePlaylistTrack(playlistId, index, offset)
                },
            )
        }
        composable(SairenDestinations.SEARCH) {
            val searchViewModel: SearchViewModel = viewModel(factory = searchViewModelFactory)
            SearchScreen(
                viewModel = searchViewModel,
                currentTrackId = currentTrackId,
                likedIds = likedIds,
                actions = actions,
                onTrackMore = onTrackMore,
                onToggleLike = onToggleLike,
                onAlbumClick = { album -> onNavigateToAlbum(album.id) },
                onArtistClick = onNavigateToArtist,
                onBack = { navController.popBackStack() },
                contentPadding = PaddingValues(bottom = bottomInset + 12.dp),
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )
        }
        composable(SairenDestinations.SETTINGS) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = settingsViewModel::setThemeMode,
                notificationsAllowed = notificationsAllowed,
                onRequestNotifications = onRequestNotifications,
                onRescan = libraryViewModel::refresh,
                isScanning = libraryState.isLoading,
                contentPadding = innerPadding,
            )
        }
        composable(
            route = SairenDestinations.NOW_PLAYING,
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
            popEnterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
        ) {
            NowPlayingScreen(
                state = playbackState,
                progress = progress,
                isLiked = isCurrentLiked,
                onToggleLike = { currentTrackId?.let(libraryViewModel::toggleLike) },
                actions = actions,
                windowSize = windowSize,
                contentPadding = innerPadding,
                onCollapse = { navController.popBackStack() },
                onGoToAlbum = { albumId ->
                    navController.popBackStack()
                    onNavigateToAlbum(albumId)
                },
                onGoToArtist = { artist ->
                    navController.popBackStack()
                    onNavigateToArtist(artist)
                },
            )
        }
    }
}
