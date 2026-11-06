package com.sairen.player.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sairen.player.R
import com.sairen.player.ui.components.Artwork
import com.sairen.player.ui.components.PrimaryPillButton
import com.sairen.player.ui.components.SecondaryPillButton

/** Header used by album/artist detail screens. */
@Composable
fun DetailHeader(
    artworkUri: String?,
    title: String,
    subtitle: String,
    meta: String,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 20.dp)) {
            Artwork(
                artworkUri = artworkUri,
                contentDescription = null,
                modifier = Modifier.size(132.dp),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryPillButton(
                    label = stringResource(R.string.action_play_all),
                    onClick = onPlay,
                )
                Spacer(Modifier.width(10.dp))
                SecondaryPillButton(
                    label = stringResource(R.string.action_shuffle_all),
                    onClick = onShuffle,
                )
            }
        }
    }
}
