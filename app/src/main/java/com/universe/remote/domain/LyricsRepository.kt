package com.universe.remote.domain

import androidx.compose.runtime.Immutable

@Immutable
data class LyricsTrack(
    val spotifyId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

@Immutable
data class LyricsLine(val timeMs: Long, val text: String)

@Immutable
data class LyricsDocument(
    val lines: List<LyricsLine> = emptyList(),
    val plainText: String? = null,
    val instrumental: Boolean = false,
)

sealed interface LyricsResult {
    data class Found(val lyrics: LyricsDocument) : LyricsResult
    data object Missing : LyricsResult
}

interface LyricsRepository {
    suspend fun lyricsFor(track: LyricsTrack, refresh: Boolean = false): LyricsResult
}
