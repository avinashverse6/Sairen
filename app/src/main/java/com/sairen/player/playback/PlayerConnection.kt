package com.sairen.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sairen.player.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Single owner of the app's `MediaController` connection.
 *
 * The UI never touches Media3 types: it observes [state] and calls the intent
 * methods below. All controller calls happen on the main thread, as Media3
 * requires.
 */
// Media3 marks a handful of controller/session APIs @UnstableApi (they may change
// between minor versions). Opting in locally keeps lint's UnsafeOptInUsageError
// quiet without exposing an unstable API surface to the rest of the app.
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerConnection(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val sessionToken = SessionToken(
        appContext,
        ComponentName(appContext, PlaybackService::class.java),
    )
    private val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

    private var controller: MediaController? = null

    init {
        controllerFuture.addListener({ connectIfReady() }, ContextCompat.getMainExecutor(appContext))
        scope.launch { positionTicker() }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(isPlaying = false, isBuffering = false) }
        }
    }

    private fun connectIfReady() {
        if (controller != null) return
        val connected = try {
            controllerFuture.get()
        } catch (_: Exception) {
            null
        } ?: return
        controller = connected
        connected.addListener(playerListener)
        syncState(connected)
    }

    private fun syncState(player: Player) {
        val metadata = player.mediaMetadata
        val albumId = metadata.extras?.getLong(MediaItems.EXTRA_ALBUM_ID)?.takeIf { it > 0L }
        val duration = player.duration.let { if (it == C.TIME_UNSET || it < 0L) 0L else it }
        val queue = ArrayList<QueueEntry>(player.mediaItemCount)
        for (index in 0 until player.mediaItemCount) {
            queue += MediaItems.toQueueEntry(index, player.getMediaItemAt(index))
        }
        _state.update { previous ->
            previous.copy(
                isConnected = true,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull(),
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
                albumId = albumId,
                artworkUri = metadata.artworkUri?.toString(),
                durationMs = duration,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = RepeatMode.fromPlayerMode(player.repeatMode),
                queue = queue,
                currentIndex = player.currentMediaItemIndex,
            )
        }
    }

    private suspend fun positionTicker() {
        while (scope.isActive) {
            val player = controller
            if (player != null && player.isPlaying) {
                val position = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration.let { if (it == C.TIME_UNSET || it < 0L) 0L else it }
                _state.update { current ->
                    if (current.positionMs != position || current.durationMs != duration) {
                        current.copy(positionMs = position, durationMs = duration)
                    } else {
                        current
                    }
                }
            }
            delay(POSITION_TICK_MS)
        }
    }

    // ----------------------------------------------------------------- commands

    /** Replaces the queue with [tracks] and starts playing at [startIndex]. */
    fun play(tracks: List<Track>, startIndex: Int = 0) {
        val player = controller ?: return
        if (tracks.isEmpty()) return
        val items: List<MediaItem> = tracks.map(MediaItems::fromTrack)
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.play()
    }

    /** Shuffles [tracks] and starts with the one at [startIndex] as the seed. */
    fun shuffleAndPlay(tracks: List<Track>, startIndex: Int = 0) {
        val player = controller ?: return
        if (tracks.isEmpty()) return
        val items = tracks.map(MediaItems::fromTrack)
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        player.shuffleModeEnabled = true
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    /** Restarts the current track when it is already underway, like a hardware remote. */
    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > RESTART_THRESHOLD_MS) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val player = controller ?: return
        player.repeatMode = RepeatMode.fromPlayerMode(player.repeatMode).next().playerValue
    }

    /** Adds a single track to the end of the queue, starting playback if idle. */
    fun addToQueue(track: Track) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) {
            player.setMediaItem(MediaItems.fromTrack(track))
        } else {
            player.addMediaItem(MediaItems.fromTrack(track))
        }
    }

    /** Inserts a track right after the current one. */
    fun playNext(track: Track) {
        val player = controller ?: return
        if (player.mediaItemCount == 0) {
            player.setMediaItem(MediaItems.fromTrack(track))
            return
        }
        val index = QueueEditor.insertionIndexForPlayNext(
            currentIndex = player.currentMediaItemIndex,
            size = player.mediaItemCount,
        )
        player.addMediaItem(index, MediaItems.fromTrack(track))
    }

    fun moveQueueItemUp(index: Int) {
        val player = controller ?: return
        val plan = QueueEditor.moveUp(index, player.currentMediaItemIndex, player.mediaItemCount)
            ?: return
        player.moveMediaItem(plan.from, plan.to)
    }

    fun moveQueueItemDown(index: Int) {
        val player = controller ?: return
        val plan = QueueEditor.moveDown(index, player.currentMediaItemIndex, player.mediaItemCount)
            ?: return
        player.moveMediaItem(plan.from, plan.to)
    }

    fun playQueueItem(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekTo(index, 0L)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    fun removeQueueItem(index: Int) {
        val player = controller ?: return
        val plan = QueueEditor.removeAt(index, player.currentMediaItemIndex, player.mediaItemCount)
            ?: return
        player.removeMediaItem(plan.index)
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }

    private companion object {
        const val POSITION_TICK_MS = 500L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
