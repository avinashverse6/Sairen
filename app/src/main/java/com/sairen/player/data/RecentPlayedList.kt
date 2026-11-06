package com.sairen.player.data

/**
 * Pure list maths and (de)serialisation for the recently-played history, kept
 * free of DataStore so it can be tested — and reasoned about — on its own.
 */
object RecentPlayedList {

    /** How many entries the history keeps. */
    const val DEFAULT_LIMIT = 50

    private const val SEPARATOR = ","

    /** Moves [trackId] to the front, de-duplicates and trims to [limit]. */
    fun updated(current: List<Long>, trackId: Long, limit: Int = DEFAULT_LIMIT): List<Long> {
        if (trackId <= 0L) return current
        if (limit <= 1) return listOf(trackId)
        val result = ArrayList<Long>(minOf(current.size + 1, limit))
        result += trackId
        for (id in current) {
            if (id == trackId || id <= 0L) continue
            if (result.size >= limit) break
            result += id
        }
        return result
    }

    fun encode(ids: List<Long>): String = ids.joinToString(SEPARATOR)

    /** Tolerant decoder: blank, malformed or non-positive entries are dropped. */
    fun decode(raw: String?): List<Long> =
        raw.orEmpty()
            .split(SEPARATOR)
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
}
