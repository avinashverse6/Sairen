package com.sairen.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * User playlists.
 *
 * Every mutation is a read-modify-write inside one DataStore `edit`, so two
 * quick taps cannot lose an update, and the persisted payload is the same
 * line-based string [PlaylistCodec] produces.
 */
interface PlaylistsStore {

    val playlists: Flow<List<Playlist>>

    /** Creates a playlist and returns its id, or `null` when the name is blank. */
    suspend fun createPlaylist(name: String): String?

    suspend fun renamePlaylist(playlistId: String, name: String)

    suspend fun deletePlaylist(playlistId: String)

    suspend fun addTrackToPlaylist(playlistId: String, trackId: Long)

    suspend fun removeTrackFromPlaylist(playlistId: String, index: Int)

    /** Moves an entry by [offset] (-1 up, +1 down). */
    suspend fun movePlaylistTrack(playlistId: String, index: Int, offset: Int)
}

class PlaylistStore(private val dataStore: DataStore<Preferences>) : PlaylistsStore {

    override val playlists: Flow<List<Playlist>> = dataStore.data.map { preferences ->
        PlaylistCodec.decode(preferences[KEY_PLAYLISTS])
    }

    override suspend fun createPlaylist(name: String): String? {
        if (name.isBlank()) return null
        val id = UUID.randomUUID().toString()
        var created = false
        dataStore.edit { preferences ->
            val current = PlaylistCodec.decode(preferences[KEY_PLAYLISTS])
            val updated = PlaylistEditor.create(current, id = id, name = name)
            if (updated.size != current.size) {
                created = true
                preferences[KEY_PLAYLISTS] = PlaylistCodec.encode(updated)
            }
        }
        return if (created) id else null
    }

    override suspend fun renamePlaylist(playlistId: String, name: String) = mutate { current ->
        PlaylistEditor.rename(current, playlistId, name)
    }

    override suspend fun deletePlaylist(playlistId: String) = mutate { current ->
        PlaylistEditor.delete(current, playlistId)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: Long) = mutate { current ->
        PlaylistEditor.addTrack(current, playlistId, trackId)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, index: Int) = mutate { current ->
        PlaylistEditor.removeAt(current, playlistId, index)
    }

    override suspend fun movePlaylistTrack(playlistId: String, index: Int, offset: Int) =
        mutate { current -> PlaylistEditor.move(current, playlistId, index, offset) }

    private suspend fun mutate(transform: (List<Playlist>) -> List<Playlist>) {
        dataStore.edit { preferences ->
            val current = PlaylistCodec.decode(preferences[KEY_PLAYLISTS])
            val updated = transform(current)
            if (updated != current) preferences[KEY_PLAYLISTS] = PlaylistCodec.encode(updated)
        }
    }

    private companion object {
        val KEY_PLAYLISTS = stringPreferencesKey("playlists")
    }
}
