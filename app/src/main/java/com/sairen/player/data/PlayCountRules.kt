package com.sairen.player.data

/**
 * When a listen counts as a play.
 *
 * A track counts once it has been heard for at least 30 seconds, or for half of
 * its own duration when that is shorter (so a 40s interlude counts at 20s).
 * Counting is edge-triggered by the playback host: it happens once per queued
 * item and never on seek, pause or resume.
 *
 * Kept free of Android types so the rule is unit tested directly.
 */
object PlayCountRules {

    /** Heard-for threshold used when the duration is unknown or longer than a minute. */
    const val MIN_PLAY_MS = 30_000L

    /**
     * Milliseconds of playback required for [durationMs].
     *
     * Unknown/zero durations fall back to the fixed 30 s rule instead of the
     * half-duration rule, which would otherwise qualify immediately (0 / 2 = 0).
     */
    fun thresholdMs(durationMs: Long): Long {
        if (durationMs <= 0L) return MIN_PLAY_MS
        return minOf(MIN_PLAY_MS, durationMs / 2L).coerceAtLeast(1L)
    }

    /** True once [positionMs] has reached the threshold for [durationMs]. */
    fun qualifies(positionMs: Long, durationMs: Long): Boolean =
        positionMs > 0L && positionMs >= thresholdMs(durationMs)
}
