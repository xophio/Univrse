package com.universe.remote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.remote.data.auth.TokenManager
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.SpotifyLibraryRepository
import com.universe.remote.domain.WebPlaylist
import com.universe.remote.domain.WebTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface Section<out T> {
    data object Loading : Section<Nothing>
    data class Ready<T>(val value: T) : Section<T>
    data class Failed(val message: String) : Section<Nothing>
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SpotifyLibraryRepository,
    private val player: PlayerController,
    private val tokens: TokenManager,
) : ViewModel() {
    private val _playlists = MutableStateFlow<Section<List<WebPlaylist>>>(Section.Loading)
    val playlists = _playlists.asStateFlow()
    private val _likedSongs = MutableStateFlow<Section<List<WebTrack>>>(Section.Loading)
    val likedSongs = _likedSongs.asStateFlow()
    val playbackError = player.error
    private var playlistJob: Job? = null
    private var songsJob: Job? = null

    init {
        viewModelScope.launch {
            tokens.loggedIn.collectLatest { loggedIn ->
                if (loggedIn) refresh()
                else {
                    playlistJob?.cancel()
                    songsJob?.cancel()
                    _playlists.value = Section.Loading
                    _likedSongs.value = Section.Loading
                }
            }
        }
    }

    fun refresh() { refreshPlaylists(); refreshLikedSongs() }
    fun refreshPlaylists() {
        playlistJob?.cancel()
        playlistJob = viewModelScope.launch {
            _playlists.value = Section.Loading
            try { _playlists.value = Section.Ready(repository.getMyPlaylists()) }
            catch (e: Exception) { if (e is CancellationException) throw e; _playlists.value = Section.Failed(message(e)) }
        }
    }
    fun refreshLikedSongs() {
        songsJob?.cancel()
        songsJob = viewModelScope.launch {
            _likedSongs.value = Section.Loading
            try { _likedSongs.value = Section.Ready(repository.getLikedSongs()) }
            catch (e: Exception) { if (e is CancellationException) throw e; _likedSongs.value = Section.Failed(message(e)) }
        }
    }
    fun playPlaylist(playlist: WebPlaylist, shuffle: Boolean) {
        if (shuffle) player.shuffle(true)
        player.play("spotify:playlist:${playlist.id}")
    }
    fun playTrack(track: WebTrack) { track.uri?.let(player::play) }

    private fun message(e: Exception) = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Spotify session expired. Sign in again."
            403 -> "Spotify denied access to this library. Check account and app access."
            429 -> "Spotify is rate limiting requests. Try again shortly."
            else -> "Spotify returned error ${e.code()}. Tap to retry."
        }
        is java.net.UnknownHostException -> "No network connection. Tap to retry."
        is java.io.InterruptedIOException -> "Spotify took too long to respond. Tap to retry."
        else -> "Could not load from Spotify. Tap to retry."
    }
}
