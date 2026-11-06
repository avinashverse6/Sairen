package com.sairen.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

/**
 * Accent colour derived from the current artwork, for the Now Playing gradient.
 *
 * Decodes the cover at 24×24 through Coil (and therefore through the same disk
 * and memory caches the artwork itself uses), averages it with
 * [ArtworkPalette], and falls back to the theme accent whenever there is no art
 * or the decode fails. Nothing here touches the main thread or adds a dependency.
 */
@Composable
fun rememberArtworkAccent(artworkUri: String?, fallback: Color): Color {
    val context = LocalContext.current
    val accent by produceState(initialValue = fallback, artworkUri, fallback) {
        value = fallback
        if (artworkUri.isNullOrBlank()) return@produceState
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .size(24, 24)
            .allowHardware(false)
            .build()
        val drawable = (runCatching { context.imageLoader.execute(request) }.getOrNull()
            as? SuccessResult)?.drawable
            ?: return@produceState
        val bitmap = runCatching { drawable.toBitmap() }.getOrNull() ?: return@produceState
        if (bitmap.width <= 0 || bitmap.height <= 0) return@produceState
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // The bitmap belongs to Coil's cache (toBitmap() can hand back the cached
        // instance), so it is read and left alone — never recycled here.
        value = Color(ArtworkPalette.accentFrom(pixels, fallback.toArgb()))
    }
    return accent
}
