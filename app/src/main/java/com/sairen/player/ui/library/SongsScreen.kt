package com.sairen.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import com.sairen.player.ui.components.Artwork
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.LoadingState
import com.sairen.player.ui.components.SectionHeader
import com.sairen.player.ui.components.TrackRow
import com.sairen.player.ui.player.PlayerActions

/**
 * All songs, with a horizontally scrolling "Recently played" shelf on top.
 * Lists are lazy from the first frame; nothing is composed for off-screen rows.
 */
@Composable
fun SongsScreen(
    state: LibraryState,
    recentTracks: List<Track>,
    currentTrackId: Long?,
    actions: PlayerActions,
    contentPadding: PaddingValues,
    onOpenRecent: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks = state.library.tracks
    when {
        state.isLoading && !state.hasLoadedOnce -> LoadingState(modifier = modifier)
        state.errorMessage != null && tracks.isEmpty() -> EmptyState(
            title = stringResource(R.string.library_error),
            message = state.errorMessage.orEmpty(),
            actionLabel = stringResource(R.string.library_retry),
            onActionClick = onRetry,
            modifier = modifier,
        )
        tracks.isEmpty() -> EmptyState(
            title = stringResource(R.string.library_empty_title),
            message = stringResource(R.string.library_empty_message),
            actionLabel = stringResource(R.string.library_retry),
            onActionClick = onRetry,
            modifier = modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (recentTracks.isNotEmpty()) {
                item(key = "recent-header") {
                    SectionHeader(
                        title = stringResource(R.string.library_recent),
                        actionLabel = stringResource(R.string.library_recent_see_all),
                        onActionClick = onOpenRecent,
                    )
                }
                item(key = "recent-shelf") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recentTracks, key = { it.id }) { track ->
                            RecentTrackCard(
                                track = track,
                                onClick = {
                                    actions.playTracks(recentTracks, recentTracks.indexOf(track))
                                },
                            )
                        }
                    }
                }
            }
            item(key = "all-songs-header") {
                SectionHeader(
                    title = stringResource(R.string.library_song_count, tracks.size.toString()),
                )
            }
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id },
            ) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { actions.playTracks(tracks, index) },
                    onMoreClick = { onTrackMore(track) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RecentTrackCard(
    track: Track,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .clickable(onClick = onClick),
    ) {
        Artwork(
            artworkUri = track.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(116.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
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
}
