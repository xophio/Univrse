package com.universe.remote.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.domain.LyricsDocument
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.Pill
import com.universe.remote.ui.components.motionEnabled
import com.universe.remote.ui.theme.AppTheme

@Composable
internal fun LyricsOverlay(
    visible: Boolean,
    fullScreen: Boolean,
    viewModel: NowPlayingViewModel,
    onClose: () -> Unit,
    onToggleFullScreen: () -> Unit,
) {
    val motion = motionEnabled()
    val enter = if (motion) slideInVertically(tween(AppTheme.lyricsSheetMs)) { it } + fadeIn() else fadeIn(tween(0))
    val exit = if (motion) slideOutVertically(tween(AppTheme.lyricsSheetMs)) { it } + fadeOut() else fadeOut(tween(0))
    AnimatedVisibility(visible = visible, enter = enter, exit = exit, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
          Column(
            Modifier.fillMaxWidth()
                .then(if (fullScreen) Modifier.fillMaxHeight() else Modifier.fillMaxHeight(AppTheme.lyricsPanelFraction))
                .clip(if (fullScreen) RectangleShape else AppTheme.lyricsSheet)
                .background(AppTheme.navSolid)
                .safeDrawingPadding()
                .padding(horizontal = AppTheme.page)
          ) {
            Spacer(Modifier.height(AppTheme.small))
            Box(Modifier.fillMaxWidth().height(AppTheme.medium).pointerInput(fullScreen) {
                var drag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount -> drag += amount },
                    onDragEnd = {
                        if (drag > 90.dp.toPx()) onClose()
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                )
            }, contentAlignment = Alignment.Center) {
                Box(Modifier.width(AppTheme.lyricsHandleWidth).height(AppTheme.lyricsHandleHeight)
                    .clip(CircleShape).background(AppTheme.border))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label("LYRICS", AppTheme.eyebrow)
                Spacer(Modifier.weight(1f))
                Label(
                    if (fullScreen) "COMPACT" else "FULL SCREEN",
                    AppTheme.time.copy(color = AppTheme.foreground),
                    Modifier.clip(AppTheme.pill).clickable(role = Role.Button, onClick = onToggleFullScreen)
                        .padding(horizontal = AppTheme.medium, vertical = AppTheme.small),
                )
            }
            Spacer(Modifier.height(AppTheme.small))
            LyricsContent(viewModel, Modifier.weight(1f))
            Label(
                "Lyrics by LRCLIB",
                AppTheme.time,
                Modifier.fillMaxWidth().padding(bottom = AppTheme.lyricsCreditBottom),
            )
          }
        }
    }
}

@Composable
private fun LyricsContent(viewModel: NowPlayingViewModel, modifier: Modifier) {
    val state by viewModel.lyrics.collectAsStateWithLifecycle()
    when (val value = state) {
        LyricsUiState.Idle -> CenteredLyricsMessage("Nothing playing", modifier)
        LyricsUiState.Loading -> CenteredLyricsMessage("Finding lyrics…", modifier)
        LyricsUiState.Missing -> CenteredLyricsMessage("No lyrics found", modifier)
        is LyricsUiState.Error -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Label(value.message, AppTheme.body.copy(textAlign = TextAlign.Center), Modifier.fillMaxWidth())
            Spacer(Modifier.height(AppTheme.medium))
            Pill("Try again", onClick = viewModel::retryLyrics)
        }
        is LyricsUiState.Ready -> LyricsDocumentView(value.document, value.trackId, viewModel, modifier)
    }
}

@Composable
private fun CenteredLyricsMessage(text: String, modifier: Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Label(text, AppTheme.body.copy(color = AppTheme.muted, textAlign = TextAlign.Center))
    }
}

@Composable
private fun LyricsDocumentView(
    document: LyricsDocument,
    trackId: String,
    viewModel: NowPlayingViewModel,
    modifier: Modifier,
) {
    if (document.lines.isEmpty()) {
        LazyColumn(modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AppTheme.large)) {
            item {
                BasicText(
                    document.plainText.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    style = AppTheme.lyricsLine.copy(color = AppTheme.foreground),
                )
            }
        }
        return
    }

    // Position is intentionally collected here so the Now Playing shell does not recompose every tick.
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val currentIndex = remember(document.lines, positionMs) {
        val displayPosition = positionMs + AppTheme.lyricsSyncLeadMs
        document.lines.binarySearchBy(displayPosition) { it.timeMs }.let { if (it >= 0) it else -it - 2 }
    }
    val listState = rememberLazyListState()
    val motion = motionEnabled()
    val centerLineOffset = with(LocalDensity.current) { 28.dp.roundToPx() }
    LaunchedEffect(trackId, currentIndex, motion) {
        if (currentIndex < 0) return@LaunchedEffect
        val viewport = listState.layoutInfo.viewportSize.height
        val offset = if (viewport > 0) -(viewport / 2) + centerLineOffset else 0
        if (motion) listState.animateScrollToItem(currentIndex, offset)
        else listState.scrollToItem(currentIndex, offset)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AppTheme.large),
        verticalArrangement = Arrangement.spacedBy(AppTheme.lyricsLineSpacing),
    ) {
        itemsIndexed(document.lines, key = { index, line -> "${line.timeMs}:$index" }) { index, line ->
            val active = index == currentIndex
            val past = index < currentIndex
            val floatSpec = if (motion) spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ) else snap()
            val colorSpec = if (motion) tween<androidx.compose.ui.graphics.Color>(AppTheme.lyricsLineTransitionMs) else snap()
            val color by animateColorAsState(
                targetValue = when { active -> AppTheme.lime; past -> AppTheme.muted.copy(alpha = .38f); else -> AppTheme.muted },
                animationSpec = colorSpec,
                label = "lyric-color",
            )
            val glow by animateColorAsState(
                targetValue = if (active) AppTheme.lime.copy(alpha = .09f) else AppTheme.transparent,
                animationSpec = colorSpec,
                label = "lyric-glow",
            )
            val rim by animateColorAsState(
                targetValue = if (active) AppTheme.lime.copy(alpha = .22f) else AppTheme.transparent,
                animationSpec = colorSpec,
                label = "lyric-rim",
            )
            val scale by animateFloatAsState(if (active) 1.018f else 1f, floatSpec, label = "lyric-scale")
            val shift by animateFloatAsState(if (active) 1f else 0f, floatSpec, label = "lyric-shift")
            val fontSize by animateFloatAsState(if (active) 22f else 17f, floatSpec, label = "lyric-size")
            val lineHeight by animateFloatAsState(if (active) 28f else 24f, floatSpec, label = "lyric-height")
            val shiftPx = with(LocalDensity.current) { AppTheme.small.toPx() }
            BasicText(
                line.text,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = shift * shiftPx
                    }.clip(AppTheme.card).background(glow)
                    .border(AppTheme.hairline, rim, AppTheme.card)
                    .clickable(role = Role.Button, onClickLabel = "Seek to lyric") { viewModel.seekTo(line.timeMs) }
                    .padding(vertical = AppTheme.small, horizontal = AppTheme.small),
                style = AppTheme.lyricsLine.copy(
                    color = color,
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp,
                    fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                ),
            )
        }
    }
}
