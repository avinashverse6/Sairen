package com.sairen.player.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Reads the current window width in dp and maps it onto [SairenWindowSize].
 * Uses the window (not the display) so foldables and multi-window behave.
 */
@Composable
fun currentWindowSize(): SairenWindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    return SairenWindowSize.fromWidthDp(widthDp)
}
