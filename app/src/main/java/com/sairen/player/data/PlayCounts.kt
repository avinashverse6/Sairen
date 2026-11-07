package com.sairen.player.data

/**
 * Persistent per-track play counts, serialised as `id:count,id:count`.
 *
 * Counts are small integers; the payload stays a single DataStore string, which
 * is why no database is involved. Decoding is deliberately tolerant: a corrupt
 * or truncated payload loses history, never crashes the app.
 */
object PlayCounts {

    /** Upper bound per track, so a runaway counter can never overflow storage. */
    const val MAX_COUNT = 1_000_000

    private const val PAIR_SEPARATOR = ","
    private const val COUNT_SEPARATOR = ":"

    fun encode(counts: Map<Long, Int>): String =
        counts.entries
            .filter { (id, count) -> id > 0L && count > 0 }
            .sortedBy { it.key }
            .joinToString(PAIR_SEPARATOR) { (id, count) -> "$id$COUNT_SEPARATOR$count" }

    fun decode(raw: String?): Map<Long, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = LinkedHashMap<Long, Int>()
        for (entry in raw.split(PAIR_SEPARATOR)) {
            val separator = entry.indexOf(COUNT_SEPARATOR)
            if (separator <= 0) continue
            val id = entry.substring(0, separator).trim().toLongOrNull() ?: continue
            val count = entry.substring(separator + 1).trim().toIntOrNull() ?: continue
            if (id <= 0L || count <= 0) continue
            result[id] = count.coerceAtMost(MAX_COUNT)
        }
        return result
    }

    /** Returns [counts] with one more play for [trackId]. */
    fun increment(counts: Map<Long, Int>, trackId: Long): Map<Long, Int> {
        if (trackId <= 0L) return counts
        val current = counts[trackId] ?: 0
        if (current >= MAX_COUNT) return counts
        return counts + (trackId to current + 1)
    }

    fun countOf(counts: Map<Long, Int>, trackId: Long): Int =
        if (trackId <= 0L) 0 else counts[trackId] ?: 0
}
