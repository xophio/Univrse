package com.universe.remote.data.web

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WebLibraryRepositoryTest {
    @Test fun `pagination uses source item count including null entries`() = runBlocking {
        val offsets = mutableListOf<Int>()
        val api = object : SpotifyWebApi {
            override suspend fun playlists(offset: Int, limit: Int): Page<PlaylistDto> {
                offsets.add(offset)
                return if (offset == 0) Page(listOf(PlaylistDto("a", "First"), null), "https://untrusted.example/next")
                    else Page(listOf(PlaylistDto("b", "Second")), null)
            }
            override suspend fun playlistTracks(id: String, offset: Int, limit: Int) = Page<TrackItemDto>(emptyList(), null)
            override suspend fun liked(offset: Int, limit: Int) = Page(listOf(TrackItemDto(null),
                TrackItemDto(TrackDto("t", "spotify:track:t", "Song", listOf(ArtistDto("Artist")), "track"))), null)
        }
        val repository = WebLibraryRepository(api)
        assertEquals(listOf("First", "Second"), repository.getMyPlaylists().map { it.name })
        assertEquals(listOf(0, 2), offsets)
        assertEquals(listOf("Song"), repository.getLikedSongs().map { it.name })
    }
}
