package com.sairen.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sairen.player.R

/**
 * Album artwork with a graceful fallback.
 *
 * The model is the MediaStore `albumart` content URI (or `null`). If the device
 * cannot supply embedded art — missing art, revoked permission, an OEM provider
 * that does not implement `albumart` — Coil shows the tinted note placeholder
 * instead of a broken image.
 *
 * The request is remembered per URI so scrolling does not allocate a new request
 * (and re-resolve the memory cache key) on every recomposition; Coil derives the
 * decode size from the composable's constraints, so a 46 dp row thumb is never
 * decoded at full album-cover resolution.
 */
@Composable
fun Artwork(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    crossfade: Boolean = true,
) {
    val context = LocalContext.current
    val hasArt = !artworkUri.isNullOrBlank()
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (hasArt) {
            val request = remember(context, artworkUri, crossfade) {
                ImageRequest.Builder(context)
                    .data(artworkUri)
                    .crossfade(crossfade)
                    .allowHardware(true)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(R.drawable.placeholder_art),
                error = painterResource(R.drawable.placeholder_art),
                fallback = painterResource(R.drawable.placeholder_art),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
