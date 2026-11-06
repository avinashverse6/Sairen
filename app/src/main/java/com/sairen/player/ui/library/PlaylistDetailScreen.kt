package com.sairen.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.sairen.player.data.Playlist
import com.sairen.player.data.model.Track
import com.sairen.player.ui.components.Artwork
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.LikeButton
import com.sairen.player.ui.player.PlayerActions
import com.sairen.player.ui.util.formatTotalDuration

/** A resolved row: the playlist index (for edits) plus the track it points at. */
private data class PlaylistEntry(val index: Int, val track: Track)

/**
 * One playlist: header with Play/Shuffle, then its songs in playlist order.
 *
 * Ids that no longer exist on the device are skipped rather than shown as broken
 * rows; the header mentions how many were dropped. Reordering uses explicit
 * up/down buttons, exactly like the play queue.
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist?,
    tracksById: Map<Long, Track>,
    currentTrackId: Long?,
    likedIds: Set<Long>,
    actions: PlayerActions,
    contentPadding: PaddingValues,
    onToggleLike: (Track) -> Unit,
    onRemove: (index: Int) -> Unit,
    onMove: (index: Int, offset: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlist == null) {
        EmptyState(
            title = stringResource(R.string.playlists_detail_title),
            message = stringResource(R.string.playlists_empty_message),
            iconRes = R.drawable.ic_playlist,
            modifier = modifier,
        )
        return
    }

    val entries = playlist.trackIds.mapIndexedNotNull { index, id ->
        tracksById[id]?.let { PlaylistEntry(index = index, track = it) }
    }
    val playable = entries.map { it.track }
    val missing = playlist.trackIds.size - entries.size
    val totalDuration = playable.sumOf { it.durationMs }
    val artwork = playable.firstOrNull()?.artworkUri

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            DetailHeader(
                artworkUri = artwork,
                title = playlist.name,
                subtitle = stringResource(R.string.playlists_song_count, playable.size.toString()),
                meta = buildString {
                    append(formatTotalDuration(totalDuration))
                    if (missing > 0) {
                        append(" • ")
                        append(stringResource(R.string.playlists_missing_tracks, missing.toString()))
                    }
                },
                onPlay = { if (playable.isNotEmpty()) actions.playTracks(playable, 0) },
                onShuffle = { if (playable.isNotEmpty()) actions.shuffleTracks(playable, 0) },
            )
        }
        if (entries.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.playlists_empty_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            }
        } else {
            itemsIndexed(items = entries, key = { _, entry -> entry.track.id }) { position, entry ->
                PlaylistTrackRow(
                    track = entry.track,
                    isCurrent = entry.track.id == currentTrackId,
                    isLiked = entry.track.id in likedIds,
                    canMoveUp = entry.index > 0,
                    canMoveDown = entry.index < playlist.trackIds.lastIndex,
                    onClick = { actions.playTracks(playable, position) },
                    onToggleLike = { onToggleLike(entry.track) },
                    onMoveUp = { onMove(entry.index, -1) },
                    onMoveDown = { onMove(entry.index, 1) },
                    onRemove = { onRemove(entry.index) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: Track,
    isCurrent: Boolean,
    isLiked: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val background = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            artworkUri = track.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(8.dp),
            crossfade = false,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LikeButton(isLiked = isLiked, onToggle = onToggleLike, modifier = Modifier.size(34.dp))
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_less),
                contentDescription = stringResource(R.string.queue_move_up),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(R.string.queue_move_down),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.action_remove_from_playlist),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
