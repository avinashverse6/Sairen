package com.sairen.player.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sairen.player.data.model.Track

/**
 * Maps the app's [Track] model onto Media3 [MediaItem]s.
 *
 * The MediaStore id is the `mediaId`, which lets the service answer questions
 * like "which track is playing?" without a database lookup. The album id rides
 * along in the metadata extras so the Now Playing screen can offer "go to album".
 */
object MediaItems {

    const val EXTRA_ALBUM_ID = "com.sairen.player.extra.ALBUM_ID"

    fun fromTrack(track: Track): MediaItem {
        val extras = Bundle().apply { putLong(EXTRA_ALBUM_ID, track.albumId) }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUri?.let(Uri::parse))
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun toQueueEntry(index: Int, item: MediaItem): QueueEntry {
        val metadata = item.mediaMetadata
        return QueueEntry(
            index = index,
            mediaId = item.mediaId.toLongOrNull() ?: 0L,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            artworkUri = metadata.artworkUri?.toString(),
        )
    }
}
