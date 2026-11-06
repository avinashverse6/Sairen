package com.sairen.player.data.model

/**
 * A single playable audio file discovered through MediaStore.
 *
 * URIs are kept as [String] (not [android.net.Uri]) on purpose: the whole data
 * layer stays free of the Android framework so it can be unit tested on the JVM.
 * Coil and Media3 both accept `content://` strings directly.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String?,
    val trackNumber: Int = 0,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val durationMs: Long,
    val artworkUri: String?,
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val artworkUri: String?,
)

/** Immutable view of everything the device reported to MediaStore. */
data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    val isEmpty: Boolean get() = tracks.isEmpty()

    companion object {
        val Empty = LibrarySnapshot()
    }
}

/** What the library UI needs in order to render, including load/error state. */
data class LibraryState(
    val isLoading: Boolean = true,
    val hasLoadedOnce: Boolean = false,
    val library: LibrarySnapshot = LibrarySnapshot.Empty,
    val errorMessage: String? = null,
)
