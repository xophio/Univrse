package com.universe.remote.ui.library

import com.universe.remote.domain.WebTrack

/** Run by LibraryViewModel's Default-dispatcher flow, not from Compose. */
object LibraryOrdering {
    fun apply(songs: List<WebTrack>, order: LibrarySort, hasTagFilter: Boolean,
        matchedIds: Set<String>, plays: Map<String, Int>): List<WebTrack> {
        val filtered = if (!hasTagFilter) songs else songs.filter { it.id in matchedIds }
        return when (order) {
            LibrarySort.RECENT -> filtered.sortedByDescending { it.addedAt }
            LibrarySort.TITLE -> filtered.sortedBy { it.name.lowercase() }
            LibrarySort.ARTIST -> filtered.sortedBy { it.artists.firstOrNull()?.lowercase().orEmpty() }
            LibrarySort.MOST_PLAYED -> filtered.sortedWith(compareByDescending<WebTrack> {
                plays[it.id] ?: 0 }.thenBy { it.name.lowercase() })
        }
    }
}
