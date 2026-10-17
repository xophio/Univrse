package com.universe.remote.domain

import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, SETUP_REQUIRED, NOT_INSTALLED, LOGIN_REQUIRED, AUTH_REQUIRED, ERROR }
data class ConnectionState(val status: ConnectionStatus = ConnectionStatus.DISCONNECTED, val message: String = "Spotify is disconnected")
enum class RepeatMode(val sdkValue: Int) { OFF(0), TRACK(1), CONTEXT(2) }
data class PlaybackState(
    val uri: String = "", val title: String = "Nothing playing yet", val artist: String = "Connect to Spotify to begin",
    val paused: Boolean = true, val positionMs: Long = 0, val durationMs: Long = 0,
    val shuffle: Boolean = false, val repeat: RepeatMode = RepeatMode.OFF,
    val canSeek: Boolean = false, val canNext: Boolean = false, val canPrevious: Boolean = false,
    val canShuffle: Boolean = false, val canRepeatTrack: Boolean = false, val canRepeatContext: Boolean = false,
)
data class TrackLibraryState(
    val uri: String = "",
    val liked: Boolean? = null,
    val canChange: Boolean = false,
    val busy: Boolean = false,
)
interface PlayerController {
    val playback: StateFlow<PlaybackState>
    val onDemand: StateFlow<Boolean?>
    val error: StateFlow<String?>
    val libraryState: StateFlow<TrackLibraryState>
    fun play(uri: String)
    fun pause()
    fun resume()
    fun next()
    fun previous()
    fun seek(positionMs: Long)
    fun shuffle(enabled: Boolean)
    fun repeat(mode: RepeatMode)
    fun toggleLike()
}
