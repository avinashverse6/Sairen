package com.sairen.player.ui.navigation

import android.net.Uri

/**
 * String routes keep navigation free of the kotlinx-serialization plugin that
 * type-safe navigation would require; with seven screens a sealed constant list
 * is simpler and cheaper.
 */
object SairenDestinations {

    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val RECENT = "recent"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"
    const val PLAYLISTS = "playlists"

    const val ARG_ALBUM_ID = "albumId"
    const val ARG_ARTIST_NAME = "artistName"
    const val ARG_PLAYLIST_ID = "playlistId"

    const val ALBUM_DETAIL = "album/{$ARG_ALBUM_ID}"
    const val ARTIST_DETAIL = "artist/{$ARG_ARTIST_NAME}"
    const val PLAYLIST_DETAIL = "playlist/{$ARG_PLAYLIST_ID}"

    val topLevel: List<String> = listOf(SONGS, ALBUMS, ARTISTS, PLAYLISTS)

    fun albumDetail(albumId: Long): String = "album/$albumId"

    fun artistDetail(artistName: String): String = "artist/${Uri.encode(artistName)}"

    fun playlistDetail(playlistId: String): String = "playlist/${Uri.encode(playlistId)}"
}
