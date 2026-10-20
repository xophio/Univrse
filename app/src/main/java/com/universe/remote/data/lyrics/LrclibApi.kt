package com.universe.remote.data.lyrics

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class LrclibLyricsDto(
    val id: Long?,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Double?,
    val instrumental: Boolean = false,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)

interface LrclibApi {
    @GET("api/get")
    suspend fun get(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String,
        @Query("album_name") album: String?,
        @Query("duration") durationSeconds: Int?,
    ): Response<LrclibLyricsDto>

    @GET("api/search")
    suspend fun search(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String,
        @Query("album_name") album: String?,
    ): Response<List<LrclibLyricsDto>>
}
