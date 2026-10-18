package com.universe.remote.ui.nowplaying

import android.os.SystemClock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.universe.remote.data.spotify.SpotifyConnectionManager
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.RepeatMode
import com.universe.remote.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val player: PlayerController,
    manager: SpotifyConnectionManager,
) : ViewModel() {
    val playback = player.playback
    // Position is collected separately by the scrubber/progress line.
    val controls = playback.map { it.copy(positionMs = 0) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), playback.value.copy(positionMs = 0))
    val hasTrack = playback.map { it.uri.isNotEmpty() }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), playback.value.uri.isNotEmpty())
    val libraryState = player.libraryState
    val artwork = manager.artwork
    val connection = manager.connection
    val error = player.error

    private val _positionMs = MutableStateFlow(0L)
    val positionMs = _positionMs.asStateFlow()
    private val _coverTone = MutableStateFlow(AppTheme.background)
    val coverTone = _coverTone.asStateFlow()

    init {
        viewModelScope.launch {
            playback.collectLatest { state ->
                _positionMs.value = state.positionMs.coerceIn(0, state.durationMs.coerceAtLeast(0))
                if (state.uri.isEmpty() || state.paused) return@collectLatest
                val started = SystemClock.elapsedRealtime()
                while (true) {
                    delay(250)
                    _positionMs.value = (state.positionMs + SystemClock.elapsedRealtime() - started)
                        .coerceIn(0, state.durationMs.coerceAtLeast(0))
                }
            }
        }
        viewModelScope.launch {
            artwork.collectLatest { bitmap ->
                if (bitmap == null) { _coverTone.value = AppTheme.background; return@collectLatest }
                _coverTone.value = withContext(Dispatchers.Default) {
                    val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                    val rgb = palette.darkMutedSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                    rgb?.let { lerp(AppTheme.background, Color(it), AppTheme.coverToneBlend) } ?: AppTheme.background
                }
            }
        }
    }

    fun togglePlayback() {
        if (playback.value.paused) player.resume() else player.pause()
    }
    fun previous() = player.previous()
    fun next() = player.next()
    fun toggleShuffle() = player.shuffle(!playback.value.shuffle)
    fun cycleRepeat() {
        val state = playback.value
        val modes = buildList {
            add(RepeatMode.OFF)
            if (state.canRepeatContext) add(RepeatMode.CONTEXT)
            if (state.canRepeatTrack) add(RepeatMode.TRACK)
        }
        if (modes.size > 1) player.repeat(modes[(modes.indexOf(state.repeat) + 1).mod(modes.size)])
    }
    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0, playback.value.durationMs.coerceAtLeast(0))
        _positionMs.value = target
        player.seek(target)
    }
    fun toggleLike() = player.toggleLike()
}
