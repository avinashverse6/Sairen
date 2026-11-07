package com.sairen.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.SongCollection
import com.sairen.player.data.SongCollections
import com.sairen.player.data.model.LibraryState
import com.sairen.player.data.model.Track
import com.sairen.player.ui.components.Artwork
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.PrimaryPillButton
import com.sairen.player.ui.components.SecondaryPillButton
import com.sairen.player.ui.components.ScreenActionsRow
import com.sairen.player.ui.components.SectionHeader
import com.sairen.player.ui.components.TrackRow
import com.sairen.player.ui.player.PlayerActions

/**
 * All songs plus the curated collections, selected with a horizontally scrolling
 * chip row: All, Liked, Recently added, Most played, Recently played, Not played.
 *
 * Nothing is shown while the very first MediaStore scan runs — the list simply
 * appears when it is ready. There is no spinner, banner or "scanning" text
 * anywhere in the startup path.
 *
 * Tapping a row plays *within the filtered view*, so "shuffle" on Not played only
 * shuffles songs that have never been played.
 */
@Composable
fun SongsScreen(
    state: LibraryState,
    recentTracks: List<Track>,
    likedIds: Set<Long>,
    playCounts: Map<Long, Int>,
    currentTrackId: Long?,
    actions: PlayerActions,
    contentPadding: PaddingValues,
    onOpenRecent: () -> Unit,
    onTrackMore: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks = state.library.tracks
    var collection by rememberSaveable { mutableStateOf(SongCollection.ALL) }
    val recentIds = remember(recentTracks) { recentTracks.map { it.id } }
    val visibleTracks = remember(tracks, likedIds, playCounts, recentIds, collection) {
        SongCollections.tracks(
            collection = collection,
            tracks = tracks,
            likedIds = likedIds,
            playCounts = playCounts,
            recentIds = recentIds,
        )
    }

    when {
        // First scan still running: stay silent rather than showing a loader.
        !state.hasLoadedOnce -> Box(modifier = modifier.fillMaxSize())

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
            item(key = "chips") {
                CollectionChips(
                    selected = collection,
                    onSelect = { collection = it },
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                )
            }
            if (visibleTracks.isNotEmpty()) {
                item(key = "collection-actions") {
                    ScreenActionsRow(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                        PrimaryPillButton(
                            label = stringResource(R.string.action_play_all),
                            onClick = { actions.playTracks(visibleTracks, 0) },
                        )
                        SecondaryPillButton(
                            label = stringResource(R.string.action_shuffle_all),
                            onClick = { actions.shuffleTracks(visibleTracks, 0) },
                        )
                    }
                }
            }
            if (collection == SongCollection.ALL && recentTracks.isNotEmpty()) {
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
            item(key = "collection-header") {
                SectionHeader(
                    title = if (collection == SongCollection.ALL) {
                        stringResource(R.string.library_song_count, visibleTracks.size.toString())
                    } else {
                        stringResource(collection.labelRes())
                    },
                )
            }
            if (visibleTracks.isEmpty()) {
                item(key = "collection-empty") {
                    CollectionEmpty(collection = collection)
                }
            } else {
                itemsIndexed(
                    items = visibleTracks,
                    key = { _, track -> track.id },
                ) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isLiked = track.id in likedIds,
                        onClick = { actions.playTracks(visibleTracks, index) },
                        onMoreClick = { onTrackMore(track) },
                        onToggleLike = { onToggleLike(track) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionChips(
    selected: SongCollection,
    onSelect: (SongCollection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SongCollections.displayOrder, key = { it.name }) { collection ->
            FilterChip(
                selected = collection == selected,
                onClick = { onSelect(collection) },
                label = { Text(text = stringResource(collection.labelRes())) },
            )
        }
    }
}

@Composable
private fun CollectionEmpty(collection: SongCollection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(collection.emptyMessageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun SongCollection.labelRes(): Int = when (this) {
    SongCollection.ALL -> R.string.collection_all
    SongCollection.LIKED -> R.string.collection_liked
    SongCollection.RECENTLY_ADDED -> R.string.collection_recently_added
    SongCollection.MOST_PLAYED -> R.string.collection_most_played
    SongCollection.RECENTLY_PLAYED -> R.string.collection_recently_played
    SongCollection.NOT_PLAYED -> R.string.collection_not_played
}

private fun SongCollection.emptyMessageRes(): Int = when (this) {
    SongCollection.ALL -> R.string.library_empty_message
    SongCollection.LIKED -> R.string.collection_empty_liked
    SongCollection.RECENTLY_ADDED -> R.string.collection_empty_recently_added
    SongCollection.MOST_PLAYED -> R.string.collection_empty_most_played
    SongCollection.RECENTLY_PLAYED -> R.string.collection_empty_recently_played
    SongCollection.NOT_PLAYED -> R.string.collection_empty_not_played
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
