package com.sairen.player.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sairen.player.data.model.Track
import com.sairen.player.playback.PlayerConnection

/**
 * Player intents bundled into one value object so screens stay free of the
 * Media3 `MediaController` and are trivial to preview and test.
 */
data class PlayerActions(
    val playTracks: (List<Track>, Int) -> Unit = { _, _ -> },
    val shuffleTracks: (List<Track>, Int) -> Unit = { _, _ -> },
    val togglePlayPause: () -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
    val seekTo: (Long) -> Unit = {},
    val toggleShuffle: () -> Unit = {},
    val cycleRepeatMode: () -> Unit = {},
    val playNext: (Track) -> Unit = {},
    val addToQueue: (Track) -> Unit = {},
    val playQueueItem: (Int) -> Unit = {},
    val moveQueueItemUp: (Int) -> Unit = {},
    val moveQueueItemDown: (Int) -> Unit = {},
    val removeQueueItem: (Int) -> Unit = {},
)

@Composable
fun rememberPlayerActions(connection: PlayerConnection): PlayerActions = remember(connection) {
    PlayerActions(
        playTracks = { tracks, index -> connection.play(tracks, index) },
        shuffleTracks = { tracks, index -> connection.shuffleAndPlay(tracks, index) },
        togglePlayPause = connection::togglePlayPause,
        next = connection::next,
        previous = connection::previous,
        seekTo = connection::seekTo,
        toggleShuffle = connection::toggleShuffle,
        cycleRepeatMode = connection::cycleRepeatMode,
        playNext = connection::playNext,
        addToQueue = connection::addToQueue,
        playQueueItem = connection::playQueueItem,
        moveQueueItemUp = connection::moveQueueItemUp,
        moveQueueItemDown = connection::moveQueueItemDown,
        removeQueueItem = connection::removeQueueItem,
    )
}
