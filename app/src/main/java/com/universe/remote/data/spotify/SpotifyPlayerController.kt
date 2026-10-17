package com.universe.remote.data.spotify

import com.spotify.android.appremote.api.PlayerApi
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.types.Empty
import com.universe.remote.domain.PlaybackState
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.RepeatMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

@Singleton
class SpotifyPlayerController @Inject constructor(private val manager: SpotifyConnectionManager) : PlayerController {
    // Application-lived scope: only transforms hot state; no polling or background playback service.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override val onDemand = manager.onDemand
    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()
    override val playback = manager.playerState.map { state ->
        if (state == null) PlaybackState() else PlaybackState(
            uri = state.track?.uri.orEmpty(), title = state.track?.name ?: "Nothing playing",
            artist = state.track?.artist?.name.orEmpty(), paused = state.isPaused,
            positionMs = state.playbackPosition, durationMs = state.track?.duration ?: 0,
            shuffle = state.playbackOptions.isShuffling,
            repeat = RepeatMode.entries.firstOrNull { it.sdkValue == state.playbackOptions.repeatMode } ?: RepeatMode.OFF,
            canSeek = state.playbackRestrictions.canSeek, canNext = state.playbackRestrictions.canSkipNext,
            canPrevious = state.playbackRestrictions.canSkipPrev, canShuffle = state.playbackRestrictions.canToggleShuffle,
            canRepeatTrack = state.playbackRestrictions.canRepeatTrack, canRepeatContext = state.playbackRestrictions.canRepeatContext,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackState())

    private fun command(allowed: Boolean = true, action: (PlayerApi) -> CallResult<Empty>) {
        val remote = manager.remote
        if (remote == null || !remote.isConnected) { _error.value = "Reconnect to Spotify first."; return }
        if (!allowed) { _error.value = "Spotify does not allow this action for the current account or content."; return }
        _error.value = null
        try {
            action(remote.playerApi).setErrorCallback {
                if (manager.remote === remote) _error.value = "Spotify could not complete the command. Check playback in Spotify and try again."
            }
        } catch (_: Exception) { _error.value = "Playback command failed. Reconnect and try again." }
    }
    override fun play(uri: String) = command(onDemand.value == true) { it.play(uri) }
    override fun pause() = command { it.pause() }
    override fun resume() = command { it.resume() }
    override fun next() = command(playback.value.canNext) { it.skipNext() }
    override fun previous() = command(playback.value.canPrevious) { it.skipPrevious() }
    override fun seek(positionMs: Long) = command(playback.value.canSeek) {
        it.seekTo(positionMs.coerceIn(0, playback.value.durationMs.coerceAtLeast(0)))
    }
    override fun shuffle(enabled: Boolean) = command(playback.value.canShuffle) { it.setShuffle(enabled) }
    override fun repeat(mode: RepeatMode) = command(when (mode) {
        RepeatMode.OFF -> true
        RepeatMode.TRACK -> playback.value.canRepeatTrack
        RepeatMode.CONTEXT -> playback.value.canRepeatContext
    }) { it.setRepeat(mode.sdkValue) }
}
