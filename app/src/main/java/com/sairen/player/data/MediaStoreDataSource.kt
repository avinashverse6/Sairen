package com.sairen.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.sairen.player.data.model.Track
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device music library straight from [MediaStore].
 *
 * Design notes:
 *  * Every cursor read happens on [ioDispatcher]; nothing touches the main thread.
 *  * Only `IS_MUSIC` rows that pass a duration/size filter are returned, which
 *    drops ringtones, notification blips and zero-length stubs.
 *  * One cursor is opened per scan (no per-track queries), so a 5k track library
 *    costs a single query.
 */
class MediaStoreDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun loadTracks(): List<Track> = withContext(ioDispatcher) { queryTracks() }

    private fun queryTracks(): List<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        val selection = buildString {
            append(MediaStore.Audio.Media.IS_MUSIC).append(" != 0")
            append(" AND ").append(MediaStore.Audio.Media.DURATION).append(" >= ?")
            append(" AND ").append(MediaStore.Audio.Media.SIZE).append(" >= ?")
        }
        val selectionArgs = arrayOf(
            MIN_DURATION_MS.toString(),
            MIN_SIZE_BYTES.toString(),
        )

        val tracks = ArrayList<Track>()
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                SORT_ORDER,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    if (id <= 0) continue
                    val albumId = cursor.getLong(albumIdColumn)
                    val fallbackTitle = cursor.getString(displayNameColumn)
                        ?.substringBeforeLast('.')
                        ?.takeIf { it.isNotBlank() }
                        ?: UNKNOWN_TITLE
                    tracks += Track(
                        id = id,
                        title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() }
                            ?: fallbackTitle,
                        artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() }
                            ?: UNKNOWN_ARTIST,
                        album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() }
                            ?: UNKNOWN_ALBUM,
                        albumId = albumId,
                        durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                        uri = ContentUris
                            .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            .toString(),
                        artworkUri = albumArtworkUri(albumId),
                        trackNumber = cursor.getInt(trackColumn),
                        // DATE_ADDED is seconds since the epoch.
                        dateAddedMs = cursor.getLong(dateAddedColumn) * 1_000L,
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission was revoked between the gate check and the scan.
            return emptyList()
        } catch (_: IllegalArgumentException) {
            // Some OEM providers reject parts of the projection; treat as empty.
            return emptyList()
        }
        return tracks
    }

    /**
     * MediaStore exposes embedded album art through the legacy `albumart`
     * provider. It is not part of the public SDK, so it can be missing on a few
     * devices — Coil then falls back to the placeholder artwork.
     */
    private fun albumArtworkUri(albumId: Long): String? {
        if (albumId <= 0L) return null
        return ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
    }

    companion object {
        /** Anything shorter than 30s is almost always a ringtone or notification sound. */
        const val MIN_DURATION_MS = 30_000L

        /** Skip near-empty files (`0`-byte placeholders created by some downloaders). */
        const val MIN_SIZE_BYTES = 100L * 1024L

        private const val SORT_ORDER = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        private const val UNKNOWN_TITLE = "Untitled"
        private const val UNKNOWN_ARTIST = "Unknown artist"
        private const val UNKNOWN_ALBUM = "Unknown album"

        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
