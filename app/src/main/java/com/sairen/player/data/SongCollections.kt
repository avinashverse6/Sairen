package com.sairen.player.data

import com.sairen.player.data.model.Track

/**
 * The curated slices of the library offered as filter chips on the Songs tab.
 */
enum class SongCollection {
    ALL,
    LIKED,
    RECENTLY_ADDED,
    MOST_PLAYED,
    RECENTLY_PLAYED,
    NOT_PLAYED,
}

/**
 * Pure, deterministic projections of the library snapshot.
 *
 * Every collection returns a plain list so the UI can hand it straight to the
 * player: tapping a row plays *within the filtered view*.
 */
object SongCollections {

    /** "Recently added" is a window, not the whole library. */
    const val RECENTLY_ADDED_LIMIT = 100

    fun tracks(
        collection: SongCollection,
        tracks: List<Track>,
        likedIds: Set<Long> = emptySet(),
        playCounts: Map<Long, Int> = emptyMap(),
        recentIds: List<Long> = emptyList(),
        recentlyAddedLimit: Int = RECENTLY_ADDED_LIMIT,
    ): List<Track> = when (collection) {
        SongCollection.ALL -> tracks

        SongCollection.LIKED -> tracks.filter { it.id in likedIds }

        SongCollection.RECENTLY_ADDED -> tracks
            .sortedWith(compareByDescending<Track> { it.dateAddedMs }.thenBy { it.title.lowercase() })
            .take(recentlyAddedLimit.coerceAtLeast(0))

        SongCollection.MOST_PLAYED -> tracks
            .filter { (playCounts[it.id] ?: 0) > 0 }
            .sortedWith(
                compareByDescending<Track> { playCounts[it.id] ?: 0 }
                    .thenBy { it.title.lowercase() },
            )

        SongCollection.RECENTLY_PLAYED -> {
            val byId = tracks.associateBy { it.id }
            val result = ArrayList<Track>(recentIds.size)
            val seen = HashSet<Long>(recentIds.size)
            for (id in recentIds) {
                if (!seen.add(id)) continue
                byId[id]?.let(result::add)
            }
            result
        }

        SongCollection.NOT_PLAYED -> tracks.filter { (playCounts[it.id] ?: 0) == 0 }
    }

    /** Stable, human-readable ordering used for the chip row. */
    val displayOrder: List<SongCollection> = listOf(
        SongCollection.ALL,
        SongCollection.LIKED,
        SongCollection.RECENTLY_ADDED,
        SongCollection.MOST_PLAYED,
        SongCollection.RECENTLY_PLAYED,
        SongCollection.NOT_PLAYED,
    )
}
