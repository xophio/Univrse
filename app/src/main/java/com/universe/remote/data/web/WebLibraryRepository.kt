package com.universe.remote.data.web

import com.universe.remote.domain.*
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class WebLibraryRepository @Inject constructor(private val api: SpotifyWebApi) : SpotifyLibraryRepository {
    override suspend fun getMyPlaylists() = pages { api.playlists(it) }.mapNotNull {
        it.id?.let { id -> WebPlaylist(id, it.name ?: "Untitled playlist", it.items?.total ?: it.tracks?.total) }
    }
    override suspend fun getPlaylistTracks(id: String): List<WebTrack> {
        require(id.matches(Regex("[a-zA-Z0-9]+"))) { "Invalid playlist ID" }
        return tracks(pages { api.playlistTracks(id, it) })
    }
    override suspend fun getLikedSongs() = tracks(pages { api.liked(it) })
    private fun tracks(items: List<TrackItemDto>) = items.mapNotNull { it.track }.filter {
        it.type == null || it.type == "track"
    }.map { WebTrack(it.id, it.uri, it.name ?: "Unavailable track", it.artists.orEmpty().mapNotNull { artist -> artist.name },
        it.album?.images.orEmpty().filter { image -> image.url?.startsWith("https://") == true }
            .minByOrNull { image -> kotlin.math.abs((image.width ?: 300) - 300) }?.url) }

    private suspend fun <T : Any> pages(load: suspend (Int) -> Page<T>): List<T> {
        val all = mutableListOf<T>()
        var offset = 0
        do {
            currentCoroutineContext().ensureActive()
            val page = load(offset)
            val items = page.items.orEmpty()
            all.addAll(items.filterNotNull())
            offset += items.size
            // Reconstruct offsets; never follow an arbitrary next URL with a bearer token.
        } while (page.next != null && items.isNotEmpty())
        return all
    }
}
