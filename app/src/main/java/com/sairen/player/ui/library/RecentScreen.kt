package com.sairen.player.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.model.Track
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.TrackRow
import com.sairen.player.ui.player.PlayerActions

/** Full history of the persisted "recently played" list (max 50 entries). */
@Composable
fun RecentScreen(
    recentTracks: List<Track>,
    currentTrackId: Long?,
    likedIds: Set<Long>,
    actions: PlayerActions,
    onTrackMore: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (recentTracks.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.library_recent),
            message = stringResource(R.string.recent_empty),
            iconRes = R.drawable.ic_history,
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(
            items = recentTracks,
            key = { _, track -> track.id },
        ) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                isLiked = track.id in likedIds,
                onClick = { actions.playTracks(recentTracks, index) },
                onMoreClick = { onTrackMore(track) },
                onToggleLike = { onToggleLike(track) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
