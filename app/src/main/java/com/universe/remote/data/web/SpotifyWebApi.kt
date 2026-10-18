package com.universe.remote.data.web

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class Page<T>(val items: List<T?>?, val next: String?)
data class PlaylistCountDto(val total: Int?)
data class PlaylistDto(val id: String?, val name: String?, val items: PlaylistCountDto? = null,
    val tracks: PlaylistCountDto? = null, val images: List<ImageDto>? = null)
data class ArtistDto(val name: String?)
data class ImageDto(val url: String?, val width: Int?, val height: Int?)
data class AlbumDto(val images: List<ImageDto>?, val name: String?)
data class TrackDto(val id: String?, val uri: String?, val name: String?, val artists: List<ArtistDto>?,
    val type: String?, val album: AlbumDto? = null, @SerializedName("duration_ms") val durationMs: Long? = null)
data class TrackItemDto(@SerializedName(value = "item", alternate = ["track"]) val track: TrackDto?,
    @SerializedName("added_at") val addedAt: String? = null)
data class SearchDto(val tracks: Page<TrackDto>?)
interface SpotifyWebApi {
    @GET("me/playlists") suspend fun playlists(@Query("offset") offset: Int, @Query("limit") limit: Int = 50): Page<PlaylistDto>
    @GET("playlists/{id}/items") suspend fun playlistTracks(@Path("id") id: String, @Query("offset") offset: Int,
        @Query("limit") limit: Int = 50): Page<TrackItemDto>
    @GET("me/tracks") suspend fun liked(@Query("offset") offset: Int, @Query("limit") limit: Int = 50): Page<TrackItemDto>
    @GET("search") suspend fun search(@Query("q") query: String, @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10): SearchDto
}
