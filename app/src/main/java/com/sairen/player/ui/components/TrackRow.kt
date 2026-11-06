package com.sairen.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.model.Track
import com.sairen.player.ui.util.formatDuration

/**
 * One song row. The title turns accent-green while the track is current, which
 * is a cheap, animated-free way to show state without stealing screen space.
 *
 * The heart is optional: pass [onToggleLike] to show it. Its tint is the only
 * thing that changes when a track is liked, so toggling never re-measures the row.
 */
@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    showArtwork: Boolean = true,
    leadingText: String? = null,
    isLiked: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingText != null) {
            Text(
                text = leadingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp),
            )
        }
        if (showArtwork) {
            Artwork(
                artworkUri = track.artworkUri,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(8.dp),
                crossfade = false,
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(track.artist)
                    if (track.album.isNotBlank()) {
                        append(" • ")
                        append(track.album)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onToggleLike != null) {
            LikeButton(
                isLiked = isLiked,
                onToggle = onToggleLike,
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.action_more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Heart toggle shared by rows, the sheet, Now Playing and the mini player. */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    filledTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    idleTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            painter = painterResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            ),
            contentDescription = stringResource(
                if (isLiked) R.string.action_unlike else R.string.action_like,
            ),
            tint = if (isLiked) filledTint else idleTint,
            modifier = Modifier.size(20.dp),
        )
    }
}
