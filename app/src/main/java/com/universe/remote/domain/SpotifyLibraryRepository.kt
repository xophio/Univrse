package com.universe.remote.domain

import androidx.compose.runtime.Immutable

@Immutable
data class WebPlaylist(val id: String, val name: String, val trackCount: Int? = null,
    val coverUrl: String? = null)
data class WebTrack(val id: String?, val uri: String?, val name: String, val artists: List<String>,
    val coverUrl: String? = null, val addedAt: Long = 0, val album: String = "", val durationMs: Long = 0)
interface SpotifyLibraryRepository {
    suspend fun getMyPlaylists(): List<WebPlaylist>
    suspend fun getPlaylistTracks(id: String): List<WebTrack>
    suspend fun getLikedSongs(): List<WebTrack>
    suspend fun searchTracks(query: String): List<WebTrack>
}
