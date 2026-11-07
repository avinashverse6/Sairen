package com.sairen.player.testing

import com.sairen.player.data.model.Track

/** JVM-safe track factory: URIs are plain strings, so no Android framework is needed. */
fun track(
    id: Long,
    title: String,
    artist: String = "Artist $id",
    album: String = "Album $id",
    albumId: Long = id,
    durationMs: Long = 180_000L,
    trackNumber: Int = 0,
    dateAddedMs: Long = 0L,
): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    uri = "content://media/external/audio/media/$id",
    artworkUri = null,
    trackNumber = trackNumber,
    dateAddedMs = dateAddedMs,
)
