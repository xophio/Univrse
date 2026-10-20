package com.universe.remote.ui.nowplaying

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.domain.PlaybackState
import com.universe.remote.domain.RepeatMode
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.Pill
import com.universe.remote.ui.home.HeartIcon
import com.universe.remote.ui.home.HomeGrainOverlay
import com.universe.remote.ui.home.PixelPlanet
import com.universe.remote.ui.home.ShuffleIcon
import com.universe.remote.ui.theme.AppTheme
import kotlin.math.roundToLong
import kotlin.math.abs

@Composable
fun NowPlayingScreen(viewModel: NowPlayingViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.controls.collectAsStateWithLifecycle()
    val cover by viewModel.artwork.collectAsStateWithLifecycle()
    val tone by viewModel.coverTone.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsFullScreen by rememberSaveable { mutableStateOf(false) }
    val background = remember(tone) { Brush.verticalGradient(listOf(tone, AppTheme.background, AppTheme.background)) }
    BackHandler {
        when {
            lyricsFullScreen -> lyricsFullScreen = false
            lyricsOpen -> lyricsOpen = false
            else -> onClose()
        }
    }
    Box(modifier.fillMaxSize().background(background).pointerInput(onClose, lyricsOpen) {
        val threshold = AppTheme.nowDismissDistance.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var end = down.position
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                end = change.position
            } while (change.pressed)
            val vertical = end.y - down.position.y
            if (!lyricsOpen && abs(end.x - down.position.x) < threshold) {
                if (vertical > threshold) onClose()
                else if (vertical < -threshold) lyricsOpen = true
            }
        }
    }) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.page), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(AppTheme.nowPlayControl),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(AppTheme.iconButton).clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Close Now Playing", onClick = onClose),
                    contentAlignment = Alignment.Center) { DownChevronIcon() }
                Spacer(Modifier.weight(1f))
                Label("NOW PLAYING", AppTheme.eyebrow)
                Spacer(Modifier.weight(1f))
                PixelPlanet(Modifier.size(AppTheme.nowPlanet))
            }
            Spacer(Modifier.height(AppTheme.medium))
            if (state.uri.isEmpty()) {
                Spacer(Modifier.height(AppTheme.section))
                Label("Nothing playing", AppTheme.title)
                Spacer(Modifier.height(AppTheme.medium))
                Label(connection.message, AppTheme.caption)
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.widthIn(max = AppTheme.nowCoverMax).fillMaxWidth().aspectRatio(1f)
                        .clip(AppTheme.card).background(AppTheme.surface), contentAlignment = Alignment.Center) {
                        if (cover == null) com.universe.remote.ui.components.UniverseIcon(
                            com.universe.remote.ui.components.UniverseGlyph.MUSIC, size = AppTheme.nowHeart)
                        else Image(cover!!.asImageBitmap(), contentDescription = "Cover for ${state.title}",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
                Spacer(Modifier.height(AppTheme.large))
                TrackHeading(state, viewModel)
                Spacer(Modifier.height(AppTheme.large))
                Scrubber(state, viewModel)
                Spacer(Modifier.height(AppTheme.large))
                PlayerControls(state, viewModel)
                Spacer(Modifier.height(AppTheme.large))
                Pill("Lyrics ↑", Modifier.fillMaxWidth(), onClick = { lyricsOpen = true })
                if (error != null) Label(error!!, AppTheme.caption.copy(color = AppTheme.error),
                    Modifier.fillMaxWidth().padding(top = AppTheme.medium))
                val trackId = state.uri.takeIf { it.startsWith("spotify:track:") }?.substringAfterLast(':')
                if (trackId != null) {
                    Spacer(Modifier.height(AppTheme.large))
                    Label("Open in Spotify ↗", AppTheme.eyebrow,
                        Modifier.clickable(role = Role.Button, onClickLabel = "Open in Spotify") {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://open.spotify.com/track/$trackId"))) }
                            catch (_: ActivityNotFoundException) { }
                        }.padding(AppTheme.small))
                }
            }
            Spacer(Modifier.height(AppTheme.large))
        }
        HomeGrainOverlay(Modifier.matchParentSize())
        LyricsOverlay(
            visible = lyricsOpen,
            fullScreen = lyricsFullScreen,
            viewModel = viewModel,
            onClose = {
                if (lyricsFullScreen) lyricsFullScreen = false else lyricsOpen = false
            },
            onToggleFullScreen = { lyricsFullScreen = !lyricsFullScreen },
        )
    }
}

@Composable
private fun TrackHeading(state: PlaybackState, viewModel: NowPlayingViewModel) {
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    val current = library.takeIf { it.uri == state.uri }
    val liked = current?.liked == true
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(state.title, style = AppTheme.nowTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(state.artist, style = AppTheme.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(AppTheme.iconButton).clip(CircleShape)
            .clickable(enabled = current?.canChange == true && current.busy.not(), role = Role.Button,
                onClickLabel = if (liked) "Unlike song" else "Like song") { viewModel.toggleLike() },
            contentAlignment = Alignment.Center) {
            HeartIcon(color = if (liked) AppTheme.lime else AppTheme.foreground,
                filled = liked, iconSize = AppTheme.nowHeart)
        }
    }
}

@Composable
private fun Scrubber(state: PlaybackState, viewModel: NowPlayingViewModel) {
    val livePosition by viewModel.positionMs.collectAsStateWithLifecycle()
    var dragPosition by remember(state.uri) { mutableStateOf<Long?>(null) }
    var widthPx by remember { mutableIntStateOf(1) }
    val duration = state.durationMs.coerceAtLeast(0)
    val position = (dragPosition ?: livePosition).coerceIn(0, duration)
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    fun at(x: Float): Long = ((x / widthPx.coerceAtLeast(1)).coerceIn(0f, 1f) * duration).roundToLong()

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.fillMaxWidth().height(AppTheme.scrubberHeight).onSizeChanged { widthPx = it.width }
            .pointerInput(duration, widthPx, state.canSeek) {
                if (state.canSeek) detectTapGestures(onTap = { viewModel.seekTo(at(it.x)) })
            }.pointerInput(duration, widthPx, state.canSeek) {
                if (state.canSeek) detectHorizontalDragGestures(
                    onDragStart = { dragPosition = at(it.x) },
                    onHorizontalDrag = { change, _ -> dragPosition = at(change.position.x) },
                    onDragEnd = { dragPosition?.let(viewModel::seekTo); dragPosition = null },
                    onDragCancel = { dragPosition = null })
            }.semantics {
                contentDescription = "Track position"
                progressBarRangeInfo = ProgressBarRangeInfo(position.toFloat(), 0f..duration.toFloat())
                if (state.canSeek && duration > 0) setProgress { target -> viewModel.seekTo(target.toLong()); true }
            }.drawWithCache {
            val thickness = AppTheme.scrubberTrack.toPx()
            val y = (size.height - thickness) / 2
            val fullSize = Size(size.width, thickness)
            val fillSize = Size(size.width * fraction, thickness)
            val offset = Offset(0f, y)
            val radius = CornerRadius(thickness / 2)
            val thumbCenter = Offset(size.width * fraction, size.height / 2)
            val thumbRadius = AppTheme.scrubberThumb.toPx()
            onDrawBehind {
                drawRoundRect(AppTheme.border, offset, fullSize, radius)
                drawRoundRect(AppTheme.lime, offset, fillSize, radius)
                drawCircle(AppTheme.lime, thumbRadius, thumbCenter)
            }
        })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(formatTime(position), AppTheme.time)
            Label(formatTime(duration), AppTheme.time)
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
private fun PlayerControls(state: PlaybackState, viewModel: NowPlayingViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Control("Shuffle", state.canShuffle, state.shuffle, viewModel::toggleShuffle) {
            ShuffleIcon(color = if (state.shuffle) AppTheme.lime else AppTheme.foreground)
        }
        Control("Previous", state.canPrevious, false, viewModel::previous) {
            SkipIcon(false, AppTheme.foreground, AppTheme.navGlyph)
        }
        Box(Modifier.size(AppTheme.nowPlayControl).clip(CircleShape).background(AppTheme.lime)
            .clickable(role = Role.Button, onClickLabel = if (state.paused) "Play" else "Pause") {
                viewModel.togglePlayback()
            }, contentAlignment = Alignment.Center) {
            PlayPauseIcon(state.paused, AppTheme.ink, AppTheme.nowHeart)
        }
        Control("Next", state.canNext, false, viewModel::next) {
            SkipIcon(true, AppTheme.foreground, AppTheme.navGlyph)
        }
        Control("Repeat", state.canRepeatContext || state.canRepeatTrack,
            state.repeat != RepeatMode.OFF, viewModel::cycleRepeat) {
            Box(contentAlignment = Alignment.Center) {
                RepeatIcon(state.repeat == RepeatMode.TRACK,
                    if (state.repeat == RepeatMode.OFF) AppTheme.foreground else AppTheme.lime)
            }
        }
    }
}

@Composable
private fun Control(label: String, enabled: Boolean, selected: Boolean, action: () -> Unit,
    content: @Composable () -> Unit) {
    Box(Modifier.size(AppTheme.nowControl).clip(CircleShape)
        .background(if (selected) AppTheme.surface else AppTheme.transparent)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = action),
        contentAlignment = Alignment.Center) { content() }
}
