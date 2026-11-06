package com.sairen.player.data

/**
 * A named, ordered list of MediaStore ids. Playlists are small (tens of tracks),
 * so the whole set lives in one DataStore string instead of a database.
 *
 * [trackIds] may reference tracks that no longer exist on the device. Resolution
 * goes through the current library snapshot, which silently skips unknown ids, so
 * a deleted file can never crash a playlist screen.
 */
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long> = emptyList(),
) {
    val size: Int get() = trackIds.size
}

/**
 * Line-based serialisation: `id|base64(name)|id1,id2,id3`.
 *
 * The name is Base64-encoded (available since API 26) so any user text — pipes,
 * newlines, emoji — round-trips without an escaping scheme to get wrong.
 */
object PlaylistCodec {

    private const val FIELD_SEPARATOR = "|"
    private const val TRACK_SEPARATOR = ","

    fun encode(playlists: List<Playlist>): String =
        playlists
            .filter { it.id.isNotBlank() }
            .joinToString("\n") { playlist ->
                buildString {
                    append(playlist.id)
                    append(FIELD_SEPARATOR)
                    append(encodeName(playlist.name))
                    append(FIELD_SEPARATOR)
                    append(playlist.trackIds.filter { it > 0L }.joinToString(TRACK_SEPARATOR))
                }
            }

    fun decode(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        val result = ArrayList<Playlist>()
        val seen = HashSet<String>()
        for (line in raw.split("\n")) {
            if (line.isBlank()) continue
            val fields = line.split(FIELD_SEPARATOR, limit = 3)
            if (fields.size < 2) continue
            val id = fields[0].trim()
            if (id.isEmpty() || !seen.add(id)) continue
            val name = decodeName(fields[1]).ifBlank { "Playlist" }
            val tracks = if (fields.size >= 3) {
                fields[2].split(TRACK_SEPARATOR)
                    .mapNotNull { it.trim().toLongOrNull() }
                    .filter { it > 0L }
            } else {
                emptyList()
            }
            result += Playlist(id = id, name = name, trackIds = tracks)
        }
        return result
    }

    private fun encodeName(name: String): String =
        java.util.Base64.getEncoder().encodeToString(name.toByteArray(Charsets.UTF_8))

    private fun decodeName(raw: String): String =
        runCatching {
            String(java.util.Base64.getDecoder().decode(raw.trim()), Charsets.UTF_8)
        }.getOrDefault("")
}

/**
 * All playlist mutations as pure list transforms, so creation, renaming,
 * reordering and removal are unit tested without DataStore or Android.
 */
object PlaylistEditor {

    const val MAX_NAME_LENGTH = 60

    fun find(playlists: List<Playlist>, playlistId: String): Playlist? =
        playlists.firstOrNull { it.id == playlistId }

    /** Appends a playlist with a trimmed name; a blank name is ignored. */
    fun create(playlists: List<Playlist>, id: String, name: String): List<Playlist> {
        val clean = sanitizeName(name)
        if (id.isBlank() || clean.isEmpty() || find(playlists, id) != null) return playlists
        return playlists + Playlist(id = id, name = clean)
    }

    fun rename(playlists: List<Playlist>, playlistId: String, name: String): List<Playlist> {
        val clean = sanitizeName(name)
        if (clean.isEmpty()) return playlists
        return playlists.map { if (it.id == playlistId) it.copy(name = clean) else it }
    }

    fun delete(playlists: List<Playlist>, playlistId: String): List<Playlist> =
        playlists.filterNot { it.id == playlistId }

    /** Appends [trackId] unless it is already in the playlist. */
    fun addTrack(playlists: List<Playlist>, playlistId: String, trackId: Long): List<Playlist> {
        if (trackId <= 0L) return playlists
        return playlists.map { playlist ->
            if (playlist.id != playlistId || trackId in playlist.trackIds) {
                playlist
            } else {
                playlist.copy(trackIds = playlist.trackIds + trackId)
            }
        }
    }

    /** Removes the entry at [index]; out-of-range indices are a no-op. */
    fun removeAt(playlists: List<Playlist>, playlistId: String, index: Int): List<Playlist> =
        playlists.map { playlist ->
            if (playlist.id != playlistId || index !in playlist.trackIds.indices) {
                playlist
            } else {
                playlist.copy(trackIds = playlist.trackIds.filterIndexed { i, _ -> i != index })
            }
        }

    /**
     * Moves the entry at [index] by [offset] (-1 up, +1 down). Moves that would
     * leave the list are ignored.
     */
    fun move(
        playlists: List<Playlist>,
        playlistId: String,
        index: Int,
        offset: Int,
    ): List<Playlist> = playlists.map { playlist ->
        if (playlist.id != playlistId) return@map playlist
        val target = index + offset
        if (index !in playlist.trackIds.indices || target !in playlist.trackIds.indices) {
            return@map playlist
        }
        val reordered = playlist.trackIds.toMutableList()
        val moved = reordered.removeAt(index)
        reordered.add(target, moved)
        playlist.copy(trackIds = reordered)
    }

    /** Resolves playlist ids to the ids that still exist, preserving order. */
    fun existingTrackIds(playlist: Playlist, available: Set<Long>): List<Long> =
        playlist.trackIds.filter { it in available }

    private fun sanitizeName(name: String): String =
        name.trim().take(MAX_NAME_LENGTH)
}
