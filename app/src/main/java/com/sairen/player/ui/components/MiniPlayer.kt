package com.sairen.player.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sairen.player.R
import com.sairen.player.playback.PlayerProgress
import com.sairen.player.playback.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Compact playback bar shown above the navigation bar. Tapping it expands the
 * Now Playing screen.
 *
 * The progress hairline reads [progress] on its own, so the 2 Hz position tick
 * recomposes a 2 dp line instead of the whole bar (and everything above it).
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    progress: StateFlow<PlayerProgress>,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onExpand)
                .animateContentSize(),
        ) {
            MiniPlayerProgressLine(progress)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(
                    artworkUri = state.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LikeButton(
                    isLiked = isLiked,
                    onToggle = onToggleLike,
                    modifier = Modifier.size(36.dp),
                )
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        ),
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.player_pause else R.string.player_play,
                        ),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = stringResource(R.string.player_next),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressLine(progress: StateFlow<PlayerProgress>) {
    val value by progress.collectAsStateWithLifecycle()
    LinearProgressIndicator(
        progress = { value.fraction },
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
}
