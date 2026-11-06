package com.sairen.player.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Window size classes with the Material 3 breakpoints (600dp / 840dp).
 *
 * Implemented from the window width instead of pulling in
 * `material3-window-size-class` (deprecated) or `material3-adaptive`: the app
 * only needs three buckets, and a pure function is trivially unit testable.
 */
enum class SairenWindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    val isExpanded: Boolean get() = this == EXPANDED
    val isCompact: Boolean get() = this == COMPACT

    companion object {
        fun fromWidthDp(widthDp: Float): SairenWindowSize = when {
            widthDp < MEDIUM_BREAKPOINT -> COMPACT
            widthDp < EXPANDED_BREAKPOINT -> MEDIUM
            else -> EXPANDED
        }

        const val MEDIUM_BREAKPOINT = 600f
        const val EXPANDED_BREAKPOINT = 840f
    }
}

@Composable
fun currentWindowSize(): SairenWindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    return SairenWindowSize.fromWidthDp(widthDp)
}
