package com.sairen.player.data

import com.sairen.player.data.model.Album
import com.sairen.player.data.model.Artist
import com.sairen.player.data.model.LibrarySnapshot
import com.sairen.player.data.model.Track

/**
 * Pure grouping helpers: albums/artists are derived from the flat MediaStore
 * song list instead of issuing extra, slower cursor queries. Everything here is
 * side-effect free and therefore unit tested.
 */
object LibraryGrouping {

    private val caseInsensitiveTitle: Comparator<String> = String.CASE_INSENSITIVE_ORDER

    fun snapshot(tracks: List<Track>): LibrarySnapshot = LibrarySnapshot(
        tracks = tracks,
        albums = albumsOf(tracks),
        artists = artistsOf(tracks),
    )

    fun albumsOf(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumId }
            .map { (albumId, albumTracks) ->
                val artwork = albumTracks.firstNotNullOfOrNull { it.artworkUri }
                val artist = albumTracks
                    .map { it.artist }
                    .firstOrNull { it.isNotBlank() }
                    .orEmpty()
                Album(
                    id = albumId,
                    title = albumTracks.first().album,
                    artist = artist,
                    trackCount = albumTracks.size,
                    durationMs = albumTracks.sumOf { it.durationMs },
                    artworkUri = artwork,
                )
            }
            .sortedWith(compareBy(caseInsensitiveTitle) { it.title })

    fun artistsOf(tracks: List<Track>): List<Artist> =
        tracks.groupBy { it.artist.lowercase() }
            .map { (_, artistTracks) ->
                Artist(
                    name = artistTracks.first().artist,
                    trackCount = artistTracks.size,
                    albumCount = artistTracks.map { it.albumId }.distinct().size,
                    artworkUri = artistTracks.firstNotNullOfOrNull { it.artworkUri },
                )
            }
            .sortedWith(compareBy(caseInsensitiveTitle) { it.name })
}
