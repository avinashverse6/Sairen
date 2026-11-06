package com.sairen.player.data

/** The last queue/position, so the app can offer "pick up where you left off". */
data class SavedSession(
    val queueIds: List<Long>,
    val startIndex: Int,
    val positionMs: Long,
)

/**
 * Serialises [SavedSession] into a single preferences string.
 *
 * Format: `v1;<startIndex>;<positionMs>;<id,id,id…>` — no reflection, no JSON
 * library, and trivially testable.
 */
object SavedSessionCodec {

    private const val PREFIX = "v1"
    private const val FIELD = ";"
    private const val ID_SEPARATOR = ","

    fun encode(session: SavedSession): String = buildString {
        append(PREFIX).append(FIELD)
        append(session.startIndex).append(FIELD)
        append(session.positionMs).append(FIELD)
        append(session.queueIds.joinToString(ID_SEPARATOR))
    }

    fun decode(raw: String?): SavedSession? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(FIELD)
        if (parts.size < 4 || parts[0] != PREFIX) return null
        val startIndex = parts[1].toIntOrNull() ?: return null
        val positionMs = parts[2].toLongOrNull() ?: return null
        val ids = parts[3]
            .split(ID_SEPARATOR)
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it > 0L }
            .distinct()
        if (ids.isEmpty()) return null
        return SavedSession(
            queueIds = ids,
            startIndex = startIndex.coerceIn(0, ids.lastIndex),
            positionMs = positionMs.coerceAtLeast(0L),
        )
    }
}
