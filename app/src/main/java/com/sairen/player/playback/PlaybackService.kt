package com.sairen.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sairen.player.MainActivity
import com.sairen.player.SairenApplication
import com.sairen.player.data.MusicLibrary
import com.sairen.player.data.PlaybackSessionStore
import com.sairen.player.data.SavedSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Background playback host.
 *
 * Responsibilities, all delegated to Media3 where possible:
 *  * the ExoPlayer instance (audio focus + becoming-noisy handling included),
 *  * the `MediaSession` that exposes transport controls to the system UI,
 *    Bluetooth, lock screen, assistant and Android Auto,
 *  * the foreground notification (`MediaSessionService` starts/stops it based
 *    on the player state — no custom wake locks, no manual foreground calls).
 *
 * Sairen additionally keeps a "last session" snapshot so the next cold start can
 * show the previous queue paused where it was left.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null

    private lateinit var musicLibrary: MusicLibrary
    private lateinit var playbackSessionStore: PlaybackSessionStore

    override fun onCreate() {
        super.onCreate()
        val container = (application as SairenApplication).container
        musicLibrary = container.musicLibrary
        playbackSessionStore = container.playbackSessionStore

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Pause when headphones are unplugged / Bluetooth disconnects.
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                persistSession()
                val trackId = mediaItem?.mediaId?.toLongOrNull() ?: return
                serviceScope.launch { musicLibrary.recordPlayed(trackId) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) persistSession()
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .build()

        restoreLastSession()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.release()
            player.release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Snapshot of the queue + position, written on track changes and pauses. */
    private fun persistSession() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val ids = ArrayList<Long>(player.mediaItemCount)
        for (index in 0 until player.mediaItemCount) {
            player.getMediaItemAt(index).mediaId.toLongOrNull()?.let(ids::add)
        }
        if (ids.isEmpty()) return
        val session = SavedSession(
            queueIds = ids,
            startIndex = player.currentMediaItemIndex.coerceIn(0, ids.lastIndex),
            positionMs = player.currentPosition.coerceAtLeast(0L),
        )
        serviceScope.launch { playbackSessionStore.save(session) }
    }

    /**
     * Restores the previous queue paused at the previous position (no autoplay:
     * resuming audio the user did not ask for is not polite). The MediaStore
     * index is scanned on demand if the process died since the last launch.
     */
    private fun restoreLastSession() {
        serviceScope.launch {
            if (player.mediaItemCount > 0) return@launch
            val saved = playbackSessionStore.savedSession.first() ?: return@launch
            var tracks = musicLibrary.tracksByIds(saved.queueIds)
            if (tracks.isEmpty()) {
                musicLibrary.refresh()
                tracks = musicLibrary.tracksByIds(saved.queueIds)
            }
            if (tracks.isEmpty()) return@launch
            player.setMediaItems(
                tracks.map(MediaItems::fromTrack),
                saved.startIndex.coerceIn(0, tracks.lastIndex),
                saved.positionMs,
            )
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
