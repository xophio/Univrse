package com.universe.remote.data.web

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class Page<T>(val items: List<T?>?, val next: String?)
data class PlaylistCountDto(val total: Int?)
data class PlaylistDto(val id: String?, val name: String?, val items: PlaylistCountDto? = null, val tracks: PlaylistCountDto? = null)
data class ArtistDto(val name: String?)
data class ImageDto(val url: String?, val width: Int?, val height: Int?)
data class AlbumDto(val images: List<ImageDto>?)
data class TrackDto(val id: String?, val uri: String?, val name: String?, val artists: List<ArtistDto>?, val type: String?, val album: AlbumDto? = null)
data class TrackItemDto(@SerializedName(value = "item", alternate = ["track"]) val track: TrackDto?)
interface SpotifyWebApi {
    @GET("me/playlists") suspend fun playlists(@Query("offset") offset: Int, @Query("limit") limit: Int = 50): Page<PlaylistDto>
    @GET("playlists/{id}/items") suspend fun playlistTracks(@Path("id") id: String, @Query("offset") offset: Int,
        @Query("limit") limit: Int = 50): Page<TrackItemDto>
    @GET("me/tracks") suspend fun liked(@Query("offset") offset: Int, @Query("limit") limit: Int = 50): Page<TrackItemDto>
}
