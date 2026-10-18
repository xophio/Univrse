package com.universe.remote.data.spotify

import com.spotify.android.appremote.api.PlayerApi
import com.spotify.protocol.client.CallResult
import com.spotify.protocol.types.Empty
import com.universe.remote.domain.PlaybackState
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.RepeatMode
import com.universe.remote.domain.TrackLibraryState
import com.universe.remote.data.local.SeekIntentTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Singleton
class SpotifyPlayerController @Inject constructor(private val manager: SpotifyConnectionManager,
    private val seeks: SeekIntentTracker) : PlayerController {
    // Application-lived scope: only transforms hot state; no polling or background playback service.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override val onDemand = manager.onDemand
    private val _error = MutableStateFlow<String?>(null)
    override val error = _error.asStateFlow()
    private val _libraryState = MutableStateFlow(TrackLibraryState())
    override val libraryState = _libraryState.asStateFlow()
    private var orderedUris: List<String> = emptyList()
    private var orderedIndex = -1
    private var queuedIndex = -1
    override val playback = manager.playerState.map { state ->
        if (state == null) PlaybackState() else PlaybackState(
            uri = state.track?.uri.orEmpty(), title = state.track?.name ?: "Nothing playing",
            artist = state.track?.artist?.name.orEmpty(),
            album = state.track?.album?.name.orEmpty(),
            coverUrl = state.track?.imageUri?.raw?.let { image ->
                if (image.startsWith("spotify:image:")) "https://i.scdn.co/image/" + image.substringAfterLast(':')
                else image
            },
            paused = state.isPaused,
            positionMs = state.playbackPosition, durationMs = state.track?.duration ?: 0,
            shuffle = state.playbackOptions.isShuffling,
            repeat = RepeatMode.entries.firstOrNull { it.sdkValue == state.playbackOptions.repeatMode } ?: RepeatMode.OFF,
            canSeek = state.playbackRestrictions.canSeek, canNext = state.playbackRestrictions.canSkipNext,
            canPrevious = state.playbackRestrictions.canSkipPrev, canShuffle = state.playbackRestrictions.canToggleShuffle,
            canRepeatTrack = state.playbackRestrictions.canRepeatTrack, canRepeatContext = state.playbackRestrictions.canRepeatContext,
        )
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackState())

    init {
        scope.launch {
            playback.map { it.uri }.distinctUntilChanged().collect { uri ->
                _libraryState.value = TrackLibraryState(uri)
                val remote = manager.remote
                if (!uri.startsWith("spotify:track:") || remote == null || !remote.isConnected) return@collect
                try {
                    remote.userApi.getLibraryState(uri).setResultCallback { state ->
                        if (manager.remote === remote && playback.value.uri == uri && !_libraryState.value.busy) {
                            _libraryState.value = TrackLibraryState(uri, state.isAdded, state.canAdd || state.isAdded)
                        }
                    }.setErrorCallback {
                        if (manager.remote === remote && playback.value.uri == uri)
                            _error.value = "Spotify could not check whether this track is liked."
                    }
                } catch (_: Exception) {
                    _error.value = "Spotify could not check whether this track is liked."
                }
            }
        }
        scope.launch {
            playback.map { it.uri }.filter { it.isNotBlank() }.distinctUntilChanged().collect(::advanceOrderedQueue)
        }
    }

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
    override fun play(uri: String) {
        clearOrderedQueue()
        command(onDemand.value == true) { it.play(uri) }
    }
    override fun playOrdered(uris: List<String>) {
        val sequence = uris.filter { it.startsWith("spotify:track:") }
        val remote = manager.remote
        if (sequence.isEmpty()) return
        if (remote == null || !remote.isConnected) { _error.value = "Reconnect to Spotify first."; return }
        if (onDemand.value != true) {
            _error.value = "Spotify does not allow this action for the current account or content."
            return
        }
        orderedUris = sequence
        orderedIndex = 0
        queuedIndex = -1
        _error.value = null
        fun start() {
            remote.playerApi.play(sequence.first()).setResultCallback {
                if (manager.remote === remote && orderedUris === sequence) queueOrderedNext(remote)
            }.setErrorCallback {
                if (manager.remote === remote && orderedUris === sequence) {
                    clearOrderedQueue()
                    _error.value = "Spotify could not start the selected song."
                }
            }
        }
        try {
            if (playback.value.shuffle && playback.value.canShuffle) {
                remote.playerApi.setShuffle(false).setResultCallback { start() }.setErrorCallback {
                    if (manager.remote === remote && orderedUris === sequence) {
                        clearOrderedQueue()
                        _error.value = "Spotify could not turn shuffle off."
                    }
                }
            } else start()
        } catch (_: Exception) {
            clearOrderedQueue()
            _error.value = "Playback command failed. Reconnect and try again."
        }
    }
    override fun queue(uri: String) = command(onDemand.value == true) { it.queue(uri) }
    override fun pause() = command { it.pause() }
    override fun resume() = command { it.resume() }
    override fun next() = command(playback.value.canNext) { it.skipNext() }
    override fun previous() = command(playback.value.canPrevious) { it.skipPrevious() }
    override fun seek(positionMs: Long) = command(playback.value.canSeek) {
        seeks.mark()
        it.seekTo(positionMs.coerceIn(0, playback.value.durationMs.coerceAtLeast(0)))
    }
    override fun shuffle(enabled: Boolean) = command(playback.value.canShuffle) { it.setShuffle(enabled) }
    override fun repeat(mode: RepeatMode) = command(when (mode) {
        RepeatMode.OFF -> true
        RepeatMode.TRACK -> playback.value.canRepeatTrack
        RepeatMode.CONTEXT -> playback.value.canRepeatContext
    }) { it.setRepeat(mode.sdkValue) }

    private fun advanceOrderedQueue(uri: String) {
        if (orderedUris.isEmpty()) return
        val index = orderedUris.indexOf(uri)
        if (index < 0) { clearOrderedQueue(); return }
        orderedIndex = index
        queueOrderedNext(manager.remote ?: return)
    }

    private fun queueOrderedNext(remote: com.spotify.android.appremote.api.SpotifyAppRemote) {
        val next = orderedIndex + 1
        if (next !in orderedUris.indices || queuedIndex >= next || manager.remote !== remote) return
        val expectedSequence = orderedUris
        try {
            remote.playerApi.queue(expectedSequence[next]).setResultCallback {
                if (manager.remote === remote && orderedUris === expectedSequence) queuedIndex = next
            }.setErrorCallback {
                if (manager.remote === remote && orderedUris === expectedSequence)
                    _error.value = "Spotify could not queue the next liked song."
            }
        } catch (_: Exception) {
            _error.value = "Spotify could not queue the next liked song."
        }
    }

    private fun clearOrderedQueue() {
        orderedUris = emptyList()
        orderedIndex = -1
        queuedIndex = -1
    }

    override fun toggleLike() {
        val state = _libraryState.value
        val remote = manager.remote
        if (remote == null || !remote.isConnected) { _error.value = "Reconnect to Spotify first."; return }
        if (!state.canChange || state.liked == null || state.busy || state.uri != playback.value.uri) return
        _libraryState.value = state.copy(busy = true)
        _error.value = null
        try {
            val result = if (state.liked) remote.userApi.removeFromLibrary(state.uri)
                else remote.userApi.addToLibrary(state.uri)
            result.setResultCallback {
                if (manager.remote === remote && playback.value.uri == state.uri)
                    _libraryState.value = state.copy(liked = !state.liked, busy = false)
            }.setErrorCallback {
                if (manager.remote === remote && playback.value.uri == state.uri) {
                    _libraryState.value = state.copy(busy = false)
                    _error.value = "Spotify could not update your liked songs. Try again."
                }
            }
        } catch (_: Exception) {
            _libraryState.value = state.copy(busy = false)
            _error.value = "Spotify could not update your liked songs. Try again."
        }
    }
}
