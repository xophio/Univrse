package com.universe.remote.data.web

import com.universe.remote.domain.*
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class WebLibraryRepository @Inject constructor(private val api: SpotifyWebApi) : SpotifyLibraryRepository {
    override suspend fun getMyPlaylists() = pages { api.playlists(it) }.mapNotNull {
        it.id?.let { id -> WebPlaylist(id, it.name ?: "Untitled playlist",
            it.items?.total ?: it.tracks?.total, cover(it.images)) }
    }
    override suspend fun getPlaylistTracks(id: String): List<WebTrack> {
        require(id.matches(Regex("[a-zA-Z0-9]+"))) { "Invalid playlist ID" }
        return tracks(pages { api.playlistTracks(id, it) })
    }
    override suspend fun getLikedSongs() = tracks(pages { api.liked(it) })
    override suspend fun searchTracks(query: String): List<WebTrack> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return api.search(q).tracks?.items.orEmpty().filterNotNull().filter { it.type == "track" }
            .map { mapTrack(it, 0) }
    }
    private fun tracks(items: List<TrackItemDto>) = items.mapNotNull { row ->
        row.track?.takeIf { it.type == null || it.type == "track" }?.let {
            mapTrack(it, runCatching { java.time.Instant.parse(row.addedAt).toEpochMilli() }.getOrDefault(0))
        }
    }
    private fun mapTrack(track: TrackDto, addedAt: Long) = WebTrack(track.id, track.uri,
        track.name ?: "Unavailable track", track.artists.orEmpty().mapNotNull { it.name },
        cover(track.album?.images), addedAt, track.album?.name.orEmpty(), track.durationMs ?: 0)
    private fun cover(images: List<ImageDto>?) = images.orEmpty()
        .filter { it.url?.startsWith("https://") == true }
        .minByOrNull { kotlin.math.abs((it.width ?: 300) - 300) }?.url

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
