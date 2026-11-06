package com.sairen.player.data

/**
 * The set of liked MediaStore ids, stored as a comma-separated list.
 *
 * Only ids are persisted, so a track that is later deleted from the device
 * simply never matches a library row; nothing has to be cleaned up and nothing
 * can crash on a missing track.
 */
object LikedTracks {

    private const val SEPARATOR = ","

    fun encode(ids: Set<Long>): String =
        ids.filter { it > 0L }.sorted().joinToString(SEPARATOR)

    fun decode(raw: String?): Set<Long> {
        if (raw.isNullOrBlank()) return emptySet()
        val result = LinkedHashSet<Long>()
        for (part in raw.split(SEPARATOR)) {
            val id = part.trim().toLongOrNull() ?: continue
            if (id > 0L) result += id
        }
        return result
    }

    /** Adds [trackId] when absent, removes it when present. */
    fun toggle(ids: Set<Long>, trackId: Long): Set<Long> {
        if (trackId <= 0L) return ids
        return if (trackId in ids) ids - trackId else ids + trackId
    }
}
