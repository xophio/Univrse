package com.universe.remote.data.lyrics

import com.universe.remote.data.local.LyricsDao
import com.universe.remote.data.local.LyricsEntity
import com.universe.remote.domain.LyricsDocument
import com.universe.remote.domain.LyricsRepository
import com.universe.remote.domain.LyricsResult
import com.universe.remote.domain.LyricsTrack
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.delay
import retrofit2.Response

class LrclibLyricsRepository @Inject constructor(
    private val api: LrclibApi,
    private val dao: LyricsDao,
) : LyricsRepository {
    override suspend fun lyricsFor(track: LyricsTrack, refresh: Boolean): LyricsResult {
        val cached = dao.find(track.spotifyId)
        if (!refresh && cached != null && (!cached.missing || System.currentTimeMillis() - cached.fetchedAt < MISSING_TTL)) {
            return cached.toResult()
        }

        val durationSeconds = ((track.durationMs + 500) / 1_000).toInt().takeIf { it in 1..3600 }
        val exact = requestWithRetry {
            api.get(track.title, track.artist, track.album.ifBlank { null }, durationSeconds)
        }
        val chosen = when {
            exact.isSuccessful -> exact.body()
            exact.code() == 404 -> {
                delay(300)
                val search = requestWithRetry {
                    api.search(track.title, track.artist, track.album.ifBlank { null })
                }
                if (!search.isSuccessful) throw IOException("LRCLIB search failed (${search.code()})")
                search.body().orEmpty().maxByOrNull { candidateScore(it, track) }
            }
            else -> throw IOException("LRCLIB lookup failed (${exact.code()})")
        }

        val entity = LyricsEntity(
            trackId = track.spotifyId,
            syncedLyrics = chosen?.syncedLyrics?.takeIf(String::isNotBlank),
            plainLyrics = chosen?.plainLyrics?.takeIf(String::isNotBlank),
            instrumental = chosen?.instrumental == true,
            lrclibId = chosen?.id,
            missing = chosen == null || (chosen.syncedLyrics.isNullOrBlank() && chosen.plainLyrics.isNullOrBlank()),
            fetchedAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
        return entity.toResult()
    }

    private suspend fun <T> requestWithRetry(block: suspend () -> Response<T>): Response<T> {
        var last: Response<T>? = null
        repeat(3) { attempt ->
            try {
                val response = block()
                last = response
                if (response.code() !in setOf(429, 503) || attempt == 2) return response
                val retrySeconds = response.headers()["Retry-After"]?.toLongOrNull()?.coerceIn(1, 15) ?: (attempt + 1L)
                delay(retrySeconds * 1_000)
            } catch (error: IOException) {
                if (attempt == 2) throw error
                delay((attempt + 1L) * 700)
            }
        }
        return checkNotNull(last)
    }

    private fun candidateScore(dto: LrclibLyricsDto, track: LyricsTrack): Int {
        fun norm(value: String?) = value.orEmpty().lowercase().filter(Char::isLetterOrDigit)
        var score = 0
        if (norm(dto.trackName) == norm(track.title)) score += 8
        if (norm(dto.artistName) == norm(track.artist)) score += 6
        if (track.album.isNotBlank() && norm(dto.albumName) == norm(track.album)) score += 3
        if (dto.duration != null && kotlin.math.abs(dto.duration * 1_000 - track.durationMs) <= 4_000) score += 4
        if (!dto.syncedLyrics.isNullOrBlank()) score += 2
        return score
    }

    private fun LyricsEntity.toResult(): LyricsResult {
        if (missing || instrumental) return LyricsResult.Missing
        val lines = syncedLyrics?.let(LrcParser::parse).orEmpty()
        val text = plainLyrics?.takeIf(String::isNotBlank)
        return if (lines.isNotEmpty() || text != null) LyricsResult.Found(
            LyricsDocument(lines = lines, plainText = text, instrumental = instrumental)
        ) else LyricsResult.Missing
    }

    private companion object { const val MISSING_TTL = 24 * 60 * 60 * 1_000L }
}
