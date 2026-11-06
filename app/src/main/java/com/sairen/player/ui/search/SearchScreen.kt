package com.sairen.player.ui.search

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sairen.player.R
import com.sairen.player.data.model.Album
import com.sairen.player.data.model.Track
import com.sairen.player.ui.library.AlbumCard
import com.sairen.player.ui.components.SectionHeader
import com.sairen.player.ui.components.TrackRow
import com.sairen.player.ui.player.PlayerActions

/**
 * Debounced library search. The field is intentionally local state until it is
 * debounced in the ViewModel, so typing never blocks on a query.
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    currentTrackId: Long?,
    likedIds: Set<Long>,
    actions: PlayerActions,
    onTrackMore: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (String) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Open the keyboard as soon as the screen appears: search is the only action here.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clear) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }

        when {
            query.isBlank() -> CenteredHint(stringResource(R.string.search_empty_prompt))
            results.isEmpty && query.isNotBlank() -> CenteredHint(
                stringResource(R.string.search_no_results, query),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                if (results.albums.isNotEmpty()) {
                    item(key = "albums-header") {
                        SectionHeader(title = stringResource(R.string.search_header_albums))
                    }
                    item(key = "albums") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(results.albums, key = { it.id }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    modifier = Modifier.width(132.dp),
                                )
                            }
                        }
                    }
                }
                if (results.artists.isNotEmpty()) {
                    item(key = "artists-header") {
                        SectionHeader(title = stringResource(R.string.search_header_artists))
                    }
                    items(results.artists, key = { it.name }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_artist),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(
                                    R.string.library_song_count,
                                    artist.trackCount.toString(),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (results.tracks.isNotEmpty()) {
                    item(key = "songs-header") {
                        SectionHeader(title = stringResource(R.string.search_header_songs))
                    }
                    itemsIndexed(
                        items = results.tracks,
                        key = { _, track -> track.id },
                    ) { index, track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isLiked = track.id in likedIds,
                            onClick = { actions.playTracks(results.tracks, index) },
                            onMoreClick = { onTrackMore(track) },
                            onToggleLike = { onToggleLike(track) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
