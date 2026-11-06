package com.sairen.player.playback

/** Repeat modes, mirroring `androidx.media3.common.Player.REPEAT_MODE_*`. */
enum class RepeatMode(val playerValue: Int) {
    OFF(0),
    ALL(1),
    ONE(2),
    ;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromPlayerMode(mode: Int): RepeatMode =
            entries.firstOrNull { it.playerValue == mode } ?: OFF
    }
}

/** One row in the play queue. Mirrors [androidx.media3.common.MediaItem] metadata. */
data class QueueEntry(
    val index: Int,
    val mediaId: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
)

/**
 * Everything the player UI observes. Produced on the main thread by
 * [PlayerConnection] from a Media3 `MediaController`.
 */
data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentMediaId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumId: Long? = null,
    val artworkUri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
) {
    val hasTrack: Boolean get() = currentMediaId != null

    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
