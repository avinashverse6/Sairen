package com.sairen.player.ui.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.data.Playlist
import com.sairen.player.ui.components.DeletePlaylistDialog
import com.sairen.player.ui.components.EmptyState
import com.sairen.player.ui.components.PlaylistNameDialog

/**
 * Library tab listing the user's playlists. Creation, renaming and deletion all
 * happen here; songs are added from the track actions sheet or inside a playlist.
 */
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Playlist, String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlists_name_title_new),
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { creating = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                creating = false
            },
        )
    }

    renaming?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(R.string.playlists_name_title_rename),
            initialName = playlist.name,
            confirmLabel = stringResource(R.string.playlists_rename),
            onDismiss = { renaming = null },
            onConfirm = { name ->
                onRenamePlaylist(playlist, name)
                renaming = null
            },
        )
    }

    deleting?.let { playlist ->
        DeletePlaylistDialog(
            playlist = playlist,
            onDismiss = { deleting = null },
            onConfirm = {
                onDeletePlaylist(playlist)
                deleting = null
            },
        )
    }

    if (playlists.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.playlists_empty_title),
            message = stringResource(R.string.playlists_empty_message),
            iconRes = R.drawable.ic_playlist,
            actionLabel = stringResource(R.string.playlists_new),
            onActionClick = { creating = true },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "new-playlist") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { creating = true }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.playlists_new),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRow(
                playlist = playlist,
                onClick = { onOpenPlaylist(playlist) },
                onRename = { renaming = playlist },
                onDelete = { deleting = playlist },
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_playlist),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.playlists_song_count, playlist.size.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.action_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playlists_rename)) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playlists_delete)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}
