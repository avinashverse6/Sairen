package com.sairen.player.ui.search

import com.sairen.player.data.model.Album
import com.sairen.player.data.model.Artist
import com.sairen.player.data.model.LibrarySnapshot
import com.sairen.player.data.model.Track

/** Everything a query matched, grouped the way the search screen renders it. */
data class SearchResults(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val query: String = "",
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()

    companion object {
        val Empty = SearchResults()
    }
}

/**
 * Case-insensitive, token-based search over songs, albums and artists.
 *
 * Every whitespace-separated token must appear somewhere in `"title artist album"`,
 * so "miles kind" matches "Kind of Blue" by "Miles Davis". Songs are ranked:
 * title prefix, title contains, artist contains, album contains, anything else.
 *
 * The matching is pure and unit tested; the haystack strings are pre-lowercased
 * once per library load so typing stays cheap even with thousands of tracks.
 */
class SearchIndex private constructor(
    private val tracks: List<Track>,
    private val albums: List<Album>,
    private val artists: List<Artist>,
    private val trackHaystacks: List<String>,
    private val albumHaystacks: List<String>,
    private val artistHaystacks: List<String>,
) {

    fun search(query: String, limit: Int = DEFAULT_LIMIT): SearchResults {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return SearchResults.Empty.copy(query = query)

        val matchedTracks = tracks.indices.asSequence()
            .filter { index -> matches(trackHaystacks[index], tokens) }
            .map { index -> index to rankOf(tracks[index], tokens) }
            .sortedWith(
                compareBy<Pair<Int, Int>> { it.second }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { tracks[it.first].title },
            )
            .map { tracks[it.first] }
            .take(limit)
            .toList()

        val matchedAlbums = albums.filterIndexed { index, _ ->
            matches(albumHaystacks[index], tokens)
        }.take(limit)

        val matchedArtists = artists.filterIndexed { index, _ ->
            matches(artistHaystacks[index], tokens)
        }.take(limit)

        return SearchResults(
            tracks = matchedTracks,
            albums = matchedAlbums,
            artists = matchedArtists,
            query = query,
        )
    }

    private fun matches(haystack: String, tokens: List<String>): Boolean =
        tokens.all { haystack.contains(it) }

    private fun rankOf(track: Track, tokens: List<String>): Int {
        val title = track.title.lowercase()
        val artist = track.artist.lowercase()
        val album = track.album.lowercase()
        val first = tokens.first()
        return when {
            title.startsWith(first) -> 0
            tokens.all { title.contains(it) } -> 1
            tokens.all { artist.contains(it) } -> 2
            tokens.all { album.contains(it) } -> 3
            else -> 4
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 200

        val Empty: SearchIndex = SearchIndex(
            tracks = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            trackHaystacks = emptyList(),
            albumHaystacks = emptyList(),
            artistHaystacks = emptyList(),
        )

        fun of(snapshot: LibrarySnapshot): SearchIndex = SearchIndex(
            tracks = snapshot.tracks,
            albums = snapshot.albums,
            artists = snapshot.artists,
            trackHaystacks = snapshot.tracks.map { "${it.title} ${it.artist} ${it.album}".lowercase() },
            albumHaystacks = snapshot.albums.map { "${it.title} ${it.artist}".lowercase() },
            artistHaystacks = snapshot.artists.map { it.name.lowercase() },
        )

        /** Lowercases and splits on whitespace, dropping empties. */
        fun tokenize(query: String): List<String> =
            query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    }
}
