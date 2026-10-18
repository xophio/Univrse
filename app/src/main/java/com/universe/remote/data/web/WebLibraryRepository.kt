package com.universe.remote.data.web

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.universe.remote.domain.*
import java.lang.reflect.Type
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class WebLibraryRepository(
    private val api: SpotifyWebApi,
    private val cache: WebCacheStore = NoOpWebCacheStore,
) : SpotifyLibraryRepository {
    private val gson = Gson()
    private val libraryLock = Mutex()
    private val playlistType: Type = object : TypeToken<List<WebPlaylist>>() {}.type
    private val trackType: Type = object : TypeToken<List<WebTrack>>() {}.type

    override suspend fun getMyPlaylists() = cachedList("playlists", playlistType) {
        pages { api.playlists(it) }.mapNotNull {
            it.id?.let { id -> WebPlaylist(id, it.name ?: "Untitled playlist",
                it.items?.total ?: it.tracks?.total, cover(it.images)) }
        }
    }
    override suspend fun getPlaylistTracks(id: String): List<WebTrack> {
        require(id.matches(Regex("[a-zA-Z0-9]+"))) { "Invalid playlist ID" }
        return cachedList("playlist_$id", trackType, PLAYLIST_TRACK_TTL_MS) {
            tracks(pages { api.playlistTracks(id, it) })
        }
    }
    override suspend fun getLikedSongs() = cachedList("liked", trackType) {
        tracks(pages { api.liked(it) })
    }
    override suspend fun searchTracks(query: String): List<WebTrack> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return retryAfterRateLimit { api.search(q) }.tracks?.items.orEmpty()
            .filterNotNull().filter { it.type == "track" }
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
            val page = retryAfterRateLimit { load(offset) }
            val items = page.items.orEmpty()
            all.addAll(items.filterNotNull())
            offset += items.size
            // Reconstruct offsets; never follow an arbitrary next URL with a bearer token.
        } while (page.next != null && items.isNotEmpty())
        return all
    }

    private suspend fun <T> cachedList(
        key: String,
        type: Type,
        ttlMs: Long = LIBRARY_TTL_MS,
        load: suspend () -> List<T>,
    ): List<T> {
        readList<T>(key, type, ttlMs)?.let { return it }
        return libraryLock.withLock {
            readList<T>(key, type, ttlMs)?.let { return@withLock it }
            val stale = readList<T>(key, type, null)
            try {
                load().also { writeList(key, it) }
            } catch (failure: Exception) {
                if (failure is HttpException && failure.code() == 429 && stale != null) stale
                else throw failure
            }
        }
    }

    private fun <T> readList(key: String, type: Type, maxAgeMs: Long?): List<T>? = runCatching {
        val savedAt = cache.savedAt(key)
        if (savedAt <= 0L || (maxAgeMs != null && System.currentTimeMillis() - savedAt > maxAgeMs)) {
            return@runCatching null
        }
        cache.json(key)?.let { gson.fromJson<List<T>>(it, type) }
    }.getOrNull()

    private fun <T> writeList(key: String, value: List<T>) {
        cache.write(key, gson.toJson(value), System.currentTimeMillis())
    }

    private suspend fun <T> retryAfterRateLimit(load: suspend () -> T): T {
        try {
            return load()
        } catch (failure: HttpException) {
            if (failure.code() != 429) throw failure
            val seconds = failure.response()?.headers()?.get("Retry-After")
                ?.toLongOrNull()?.coerceIn(1L, MAX_RETRY_AFTER_SECONDS) ?: throw failure
            delay(seconds * 1_000L)
            currentCoroutineContext().ensureActive()
            return load()
        }
    }

    private companion object {
        const val LIBRARY_TTL_MS = 15 * 60 * 1_000L
        const val PLAYLIST_TRACK_TTL_MS = 5 * 60 * 1_000L
        const val MAX_RETRY_AFTER_SECONDS = 60L
    }
}
