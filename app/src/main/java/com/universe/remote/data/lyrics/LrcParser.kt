package com.universe.remote.data.lyrics

import com.universe.remote.domain.LyricsLine

object LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val offset = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE)
    private val enhancedTimestamp = Regex("<\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?>")

    fun parse(source: String): List<LyricsLine> {
        val offsetMs = offset.find(source)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        return source.lineSequence().flatMap { row ->
            val matches = timestamp.findAll(row).toList()
            if (matches.isEmpty()) return@flatMap emptySequence()
            val text = row.substring(matches.last().range.last + 1)
                .replace(enhancedTimestamp, "").trim()
            if (text.isEmpty()) return@flatMap emptySequence()
            matches.asSequence().mapNotNull { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    3 -> fraction.toLong()
                    else -> 0L
                }
                LyricsLine((minutes * 60_000 + seconds * 1_000 + millis + offsetMs).coerceAtLeast(0), text)
            }
        }.distinctBy { it.timeMs to it.text }.sortedBy { it.timeMs }.toList()
    }
}
