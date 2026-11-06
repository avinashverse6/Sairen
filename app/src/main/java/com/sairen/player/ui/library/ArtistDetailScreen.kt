package com.sairen.player.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.model.Album
import com.sairen.player.data.model.Track
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.SectionHeader
import com.sairen.player.ui.components.TrackRow
import com.sairen.player.ui.player.PlayerActions
import com.sairen.player.ui.util.formatTotalDuration

@Composable
fun ArtistDetailScreen(
    artistName: String,
    tracks: List<Track>,
    albums: List<Album>,
    currentTrackId: Long?,
    likedIds: Set<Long>,
    actions: PlayerActions,
    onAlbumClick: (Album) -> Unit,
    onTrackMore: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    if (tracks.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.artist_detail_title),
            message = stringResource(R.string.library_empty_message),
            modifier = modifier,
        )
        return
    }
    val artwork = tracks.firstNotNullOfOrNull { it.artworkUri }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            DetailHeader(
                artworkUri = artwork,
                title = artistName,
                subtitle = stringResource(
                    R.string.artist_album_song_count,
                    albums.size.toString(),
                    tracks.size.toString(),
                ),
                meta = formatTotalDuration(tracks.sumOf { it.durationMs }),
                onPlay = { actions.playTracks(tracks, 0) },
                onShuffle = { actions.shuffleTracks(tracks, 0) },
            )
        }
        if (albums.isNotEmpty()) {
            item(key = "albums-header") {
                SectionHeader(title = stringResource(R.string.nav_albums))
            }
            item(key = "albums-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            modifier = Modifier.width(136.dp),
                        )
                    }
                }
            }
        }
        item(key = "songs-header") {
            SectionHeader(title = stringResource(R.string.nav_songs))
        }
        itemsIndexed(
            items = tracks,
            key = { _, track -> track.id },
        ) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.id == currentTrackId,
                isLiked = track.id in likedIds,
                onClick = { actions.playTracks(tracks, index) },
                onMoreClick = { onTrackMore(track) },
                onToggleLike = { onToggleLike(track) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
