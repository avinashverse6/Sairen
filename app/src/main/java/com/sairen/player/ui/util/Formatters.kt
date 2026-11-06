package com.sairen.player.ui.util

import java.util.Locale
import kotlin.math.max

/**
 * `m:ss` for tracks under an hour and `h:mm:ss` beyond, matching the way
 * hardware players display positions. Negative or unknown (-1) durations
 * render as `0:00` instead of overflowing.
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = max(0L, durationMs) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val locale = Locale.getDefault()
    return if (hours > 0L) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%d:%02d", minutes, seconds)
    }
}

/** Coarse summary used on library headers, e.g. "1 h 12 min" or "48 min". */
fun formatTotalDuration(durationMs: Long): String {
    val totalMinutes = max(0L, durationMs) / 60_000L
    return when {
        totalMinutes < 60L -> "$totalMinutes min"
        else -> "${totalMinutes / 60L} h ${totalMinutes % 60L} min"
    }
}
