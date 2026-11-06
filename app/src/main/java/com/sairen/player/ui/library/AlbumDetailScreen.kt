package com.sairen.player.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.model.Album
import com.sairen.player.data.model.Track
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.TrackRow
import com.sairen.player.ui.player.PlayerActions
import com.sairen.player.ui.util.formatTotalDuration

@Composable
fun AlbumDetailScreen(
    album: Album?,
    tracks: List<Track>,
    currentTrackId: Long?,
    actions: PlayerActions,
    onTrackMore: (Track) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (album == null) {
        EmptyState(
            title = stringResource(R.string.album_detail_title),
            message = stringResource(R.string.library_empty_message),
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            DetailHeader(
                artworkUri = album.artworkUri,
                title = album.title,
                subtitle = album.artist.ifBlank { stringResource(R.string.unknown_artist) },
                meta = "${stringResource(R.string.library_song_count, album.trackCount.toString())} • " +
                    formatTotalDuration(album.durationMs),
                onPlay = { actions.playTracks(tracks, 0) },
                onShuffle = { actions.shuffleTracks(tracks, 0) },
            )
        }
        item(key = "divider") {
            Text(
                text = stringResource(R.string.nav_songs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp),
            )
        }
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.id },
        ) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                showArtwork = false,
                leadingText = (track.trackNumber.takeIf { it > 0 } ?: (index + 1)).toString(),
                onClick = { actions.playTracks(tracks, index) },
                onMoreClick = { onTrackMore(track) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
