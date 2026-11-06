package com.sairen.player.playback

/**
 * Playback position, published on its own flow.
 *
 * This is the only value that changes ~2x per second, so it is deliberately kept
 * out of [PlayerUiState]: only the seek bar and the mini player's progress
 * hairline read it, and only those composables recompose on a tick.
 */
data class PlayerProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val fraction: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
