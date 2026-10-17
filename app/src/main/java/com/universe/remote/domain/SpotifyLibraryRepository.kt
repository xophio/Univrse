package com.universe.remote.domain

data class WebPlaylist(val id: String, val name: String, val trackCount: Int? = null)
data class WebTrack(val id: String?, val uri: String?, val name: String, val artists: List<String>, val coverUrl: String? = null)
interface SpotifyLibraryRepository {
    suspend fun getMyPlaylists(): List<WebPlaylist>
    suspend fun getPlaylistTracks(id: String): List<WebTrack>
    suspend fun getLikedSongs(): List<WebTrack>
}
