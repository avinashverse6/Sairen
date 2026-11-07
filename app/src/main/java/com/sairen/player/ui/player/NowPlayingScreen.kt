package com.sairen.player.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sairen.player.R
import com.sairen.player.playback.PlayerProgress
import com.sairen.player.playback.PlayerUiState
import com.sairen.player.playback.RepeatMode
import com.sairen.player.ui.components.Artwork
import com.sairen.player.ui.components.LikeButton
import com.sairen.player.ui.components.rememberArtworkAccent
import com.sairen.player.ui.util.SairenWindowSize
import com.sairen.player.ui.util.formatDuration
import kotlinx.coroutines.flow.StateFlow

/**
 * Expandable Now Playing screen. On phones it is a single vertical column; on
 * tablets/foldables (expanded width class) artwork and controls sit side by side
 * and the queue can dock as a third pane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    progress: StateFlow<PlayerProgress>,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    actions: PlayerActions,
    windowSize: SairenWindowSize,
    contentPadding: PaddingValues,
    onCollapse: () -> Unit,
    onGoToAlbum: (Long) -> Unit,
    onGoToArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val expanded = windowSize.isExpanded

    // A soft wash pulled from the current cover: the screen picks up the album's
    // colour without copying any app's layout, and text stays on near-black.
    val accent = rememberArtworkAccent(
        artworkUri = state.artworkUri,
        fallback = MaterialTheme.colorScheme.primary,
    )
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.36f),
                        accent.copy(alpha = 0.12f),
                        background,
                    ),
                ),
            )
            .padding(contentPadding),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerTopBar(
                onCollapse = onCollapse,
                onToggleQueue = { showQueue = !showQueue },
                queueVisible = showQueue,
                isLiked = isLiked,
                onToggleLike = onToggleLike,
            )
            if (expanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    ArtworkPane(
                        state = state,
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .padding(24.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .widthIn(max = 460.dp)
                            .padding(end = 24.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TrackInfo(
                            state = state,
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist,
                        )
                        Spacer(Modifier.height(20.dp))
                        SeekSection(
                            progress = progress,
                            currentMediaId = state.currentMediaId,
                            actions = actions,
                        )
                        Spacer(Modifier.height(12.dp))
                        TransportControls(state = state, actions = actions)
                    }
                    if (showQueue) {
                        Column(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                                .padding(bottom = 16.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.queue_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            )
                            QueueContent(state = state, actions = actions)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ArtworkPane(
                        state = state,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .widthIn(max = 420.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    TrackInfo(
                        state = state,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(Modifier.height(20.dp))
                    SeekSection(
                        progress = progress,
                        currentMediaId = state.currentMediaId,
                        actions = actions,
                    )
                    Spacer(Modifier.height(12.dp))
                    TransportControls(state = state, actions = actions)
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }

    if (!expanded && showQueue) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
        ) {
            Text(
                text = stringResource(R.string.queue_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Box(modifier = Modifier.fillMaxHeight(0.75f)) {
                QueueContent(state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    onCollapse: () -> Unit,
    onToggleQueue: () -> Unit,
    queueVisible: Boolean,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(R.string.player_collapse),
            )
        }
        Text(
            text = stringResource(R.string.player_now_playing),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        LikeButton(
            isLiked = isLiked,
            onToggle = onToggleLike,
            modifier = Modifier.size(48.dp),
        )
        IconButton(onClick = onToggleQueue) {
            Icon(
                painter = painterResource(R.drawable.ic_queue),
                contentDescription = stringResource(R.string.player_queue),
                tint = if (queueVisible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun ArtworkPane(
    state: PlayerUiState,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f),
    )
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Artwork(
            artworkUri = state.artworkUri,
            contentDescription = state.album.ifBlank { null },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun TrackInfo(
    state: PlayerUiState,
    onGoToAlbum: (Long) -> Unit,
    onGoToArtist: (String) -> Unit,
    textAlign: TextAlign = TextAlign.Start,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = state.title.ifBlank { stringResource(R.string.app_name) },
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = state.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.artist.isNotBlank()) { onGoToArtist(state.artist) },
        )
        val albumId = state.albumId
        if (state.album.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = state.album,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = albumId != null) {
                        albumId?.let(onGoToAlbum)
                    },
            )
        }
    }
}

@Composable
private fun SeekSection(
    progress: StateFlow<PlayerProgress>,
    currentMediaId: Long?,
    actions: PlayerActions,
) {
    // Only this composable reads the 2 Hz position tick.
    val position by progress.collectAsStateWithLifecycle()
    var scrubPosition by remember(currentMediaId) { mutableStateOf<Float?>(null) }
    val durationMs = position.durationMs.coerceAtLeast(0L)
    val duration = durationMs.coerceAtLeast(1L)
    val sliderValue = (scrubPosition ?: position.positionMs.toFloat())
        .coerceIn(0f, duration.toFloat())
    val shownPositionMs = scrubPosition?.toLong() ?: position.positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                scrubPosition?.let { actions.seekTo(it.toLong()) }
                scrubPosition = null
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration(shownPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransportControls(
    state: PlayerUiState,
    actions: PlayerActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = state.shuffleEnabled,
            onCheckedChange = { actions.toggleShuffle() },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = stringResource(R.string.player_shuffle),
                tint = if (state.shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = actions.previous, modifier = Modifier.size(52.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = stringResource(R.string.player_previous),
                modifier = Modifier.size(30.dp),
            )
        }
        FilledIconButton(
            onClick = actions.togglePlayPause,
            modifier = Modifier.size(70.dp),
            shape = CircleShape,
        ) {
            Icon(
                painter = painterResource(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                ),
                contentDescription = stringResource(
                    if (state.isPlaying) R.string.player_pause else R.string.player_play,
                ),
                modifier = Modifier.size(32.dp),
            )
        }
        IconButton(onClick = actions.next, modifier = Modifier.size(52.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.player_next),
                modifier = Modifier.size(30.dp),
            )
        }
        IconToggleButton(
            checked = state.repeatMode != RepeatMode.OFF,
            onCheckedChange = { actions.cycleRepeatMode() },
        ) {
            Icon(
                painter = painterResource(
                    when (state.repeatMode) {
                        RepeatMode.ONE -> R.drawable.ic_repeat_one
                        else -> R.drawable.ic_repeat
                    },
                ),
                contentDescription = stringResource(R.string.player_repeat),
                tint = if (state.repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
