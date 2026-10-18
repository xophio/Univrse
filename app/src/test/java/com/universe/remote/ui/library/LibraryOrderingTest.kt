package com.universe.remote.ui.library

import com.universe.remote.domain.WebTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryOrderingTest {
    private val a = WebTrack("a", "spotify:track:a", "Zebra", listOf("Alpha"), addedAt = 100)
    private val b = WebTrack("b", "spotify:track:b", "Apple", listOf("Zulu"), addedAt = 200)

    @Test fun recentTitleArtistAndMostPlayed() {
        val songs = listOf(a, b)
        fun ids(sort: LibrarySort) = LibraryOrdering.apply(songs, sort, false, emptySet(),
            mapOf("a" to 5, "b" to 1)).map { it.id }
        assertEquals(listOf("b", "a"), ids(LibrarySort.RECENT))
        assertEquals(listOf("b", "a"), ids(LibrarySort.TITLE))
        assertEquals(listOf("a", "b"), ids(LibrarySort.ARTIST))
        assertEquals(listOf("a", "b"), ids(LibrarySort.MOST_PLAYED))
    }

    @Test fun moodFilterUsesRoomMatchedIds() {
        assertEquals(listOf("b"), LibraryOrdering.apply(listOf(a, b), LibrarySort.RECENT,
            true, setOf("b"), emptyMap()).map { it.id })
    }
}
