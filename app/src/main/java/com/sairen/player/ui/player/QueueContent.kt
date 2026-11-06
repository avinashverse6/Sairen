package com.sairen.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import com.sairen.player.playback.PlayerUiState
import com.sairen.player.playback.QueueEntry
import com.sairen.player.ui.components.Artwork

/**
 * Queue list shared by the bottom-sheet (compact widths) and the side panel
 * (expanded widths). Reordering uses explicit up/down buttons rather than
 * drag-and-drop: it is fully accessible, works with rotary input, and needs no
 * experimental Compose APIs.
 */
@Composable
fun QueueContent(
    state: PlayerUiState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
) {
    if (state.queue.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.queue_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        itemsIndexed(
            items = state.queue,
            key = { index, entry -> "${entry.mediaId}-$index" },
        ) { index, entry ->
            QueueRow(
                entry = entry,
                isCurrent = index == state.currentIndex,
                canMoveUp = index > 0,
                canMoveDown = index < state.queue.lastIndex,
                canRemove = index != state.currentIndex && state.queue.size > 1,
                onPlay = { actions.playQueueItem(index) },
                onMoveUp = { actions.moveQueueItemUp(index) },
                onMoveDown = { actions.moveQueueItemDown(index) },
                onRemove = { actions.removeQueueItem(index) },
            )
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val background = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            artworkUri = entry.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(6.dp),
            crossfade = false,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
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
                text = if (isCurrent) {
                    stringResource(R.string.queue_playing)
                } else {
                    entry.artist
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(34.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_less),
                contentDescription = stringResource(R.string.queue_move_up),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(34.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(R.string.queue_move_down),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.size(34.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.queue_remove, entry.title),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
