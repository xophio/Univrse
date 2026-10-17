package com.universe.remote.ui.connection

import androidx.lifecycle.ViewModel
import com.universe.remote.data.spotify.SpotifyConnectionManager
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionManager: SpotifyConnectionManager,
    private val player: PlayerController,
) : ViewModel() {
    val connection = connectionManager.connection
    val playback = player.playback
    val onDemand = player.onDemand
    val error = player.error
    val artwork = connectionManager.artwork
    fun retry() = connectionManager.retry()
    fun playTestTrack() = player.play(TEST_TRACK)
    fun togglePlayback() { if (playback.value.paused) player.resume() else player.pause() }
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekForward() = player.seek(playback.value.positionMs + 15_000)
    fun toggleShuffle() = player.shuffle(!playback.value.shuffle)
    fun cycleRepeat() {
        val state = playback.value
        val available = RepeatMode.entries.filter {
            it == RepeatMode.OFF || (it == RepeatMode.TRACK && state.canRepeatTrack) ||
                (it == RepeatMode.CONTEXT && state.canRepeatContext)
        }
        player.repeat(available[(available.indexOf(state.repeat) + 1).mod(available.size)])
    }
    companion object {
        // Replace with any playable track URI in your Spotify account's market.
        const val TEST_TRACK = "spotify:track:11dFghVXANMlKmJXsNCbNl"
    }
}
