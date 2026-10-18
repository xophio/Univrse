package com.universe.remote.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.components.coverRequest
import com.universe.remote.ui.components.motionEnabled
import com.universe.remote.ui.components.liquidGlass
import com.universe.remote.ui.nowplaying.NowPlayingViewModel
import com.universe.remote.ui.nowplaying.PlayPauseIcon
import com.universe.remote.ui.theme.AppTheme

internal enum class HomeTab(val title: String, val glyph: UniverseGlyph) {
    HOME("Home", UniverseGlyph.HOME),
    STATS("Stats", UniverseGlyph.STATS),
    LIBRARY("Library", UniverseGlyph.LIBRARY),
}

@Composable
internal fun PlayerDock(
    selected: HomeTab,
    hasTrack: Boolean,
    player: NowPlayingViewModel,
    onTab: (HomeTab) -> Unit,
    onSearch: () -> Unit,
    searchActive: Boolean,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = motionEnabled()
    Column(modifier.liquidGlass(AppTheme.dockShape)
        .padding(horizontal = AppTheme.dockHorizontalPadding,
            vertical = AppTheme.dockVerticalPadding)) {
        if (hasTrack) {
            DockPlayerRow(player, onOpenPlayer)
            DockProgress(player, Modifier.fillMaxWidth().padding(vertical = AppTheme.small))
            Spacer(Modifier.fillMaxWidth().height(AppTheme.dockDivider)
                .background(AppTheme.dockBorder))
        }
        DockNavigation(selected, searchActive, motion, onTab, onSearch)
    }
}

@Composable
private fun DockNavigation(selected: HomeTab, searchActive: Boolean, motion: Boolean,
    onTab: (HomeTab) -> Unit, onSearch: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(AppTheme.dockNavHeight)) {
        val slotWidth = maxWidth / 4
        val selectedIndex = if (searchActive) 3 else selected.ordinal
        val pillWidth = AppTheme.touch.coerceAtMost(slotWidth)
        val targetOffset = slotWidth * selectedIndex + (slotWidth - pillWidth) / 2
        val pillOffset by animateDpAsState(targetOffset,
            animationSpec = if (motion) spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ) else snap(), label = "dock selection")
        Box(Modifier.offset(x = pillOffset).width(pillWidth).height(AppTheme.touch)
            .align(Alignment.CenterStart).clip(AppTheme.pill).background(AppTheme.lime))
        Row(Modifier.fillMaxSize()) {
            HomeTab.entries.forEach { tab ->
                DockDestination(tab.title, tab.glyph, tab == selected && !searchActive,
                    motion, Modifier.weight(1f)) { onTab(tab) }
            }
            DockDestination("Search", UniverseGlyph.SEARCH, searchActive, motion,
                Modifier.weight(1f), onSearch)
        }
    }
}

@Composable
private fun DockDestination(title: String, glyph: UniverseGlyph, active: Boolean,
    motion: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val iconColor by animateColorAsState(if (active) AppTheme.ink else AppTheme.foreground,
        animationSpec = if (motion) tween(160) else snap(), label = "dock icon color")
    Box(modifier.fillMaxHeight().clickable(role = Role.Tab,
        onClickLabel = title, onClick = onClick), contentAlignment = Alignment.Center) {
        UniverseIcon(glyph, color = iconColor)
    }
}

@Composable
private fun DockPlayerRow(player: NowPlayingViewModel, onOpen: () -> Unit) {
    val state by player.controls.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth().height(AppTheme.dockPlayerHeight)
        .clickable(role = Role.Button, onClickLabel = "Open Now Playing", onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
        Box(Modifier.size(AppTheme.dockCover).clip(AppTheme.artShape)
            .background(AppTheme.surface), contentAlignment = Alignment.Center) {
            if (state.coverUrl.isNullOrBlank()) UniverseIcon(UniverseGlyph.MUSIC)
            else AsyncImage(coverRequest(state.coverUrl, AppTheme.dockCover),
                contentDescription = "Cover for ${state.title}", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f)) {
            BasicText(state.title, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(state.artist, style = AppTheme.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(AppTheme.iconButton).clip(CircleShape).background(AppTheme.lime)
            .clickable(role = Role.Button,
                onClickLabel = if (state.paused) "Play" else "Pause") { player.togglePlayback() },
            contentAlignment = Alignment.Center) {
            PlayPauseIcon(state.paused, AppTheme.ink, AppTheme.navGlyph)
        }
    }
}

@Composable
private fun DockProgress(player: NowPlayingViewModel, modifier: Modifier = Modifier) {
    val state by player.controls.collectAsStateWithLifecycle()
    val position by player.positionMs.collectAsStateWithLifecycle()
    val fraction = if (state.durationMs > 0L) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    val filled = (fraction * AppTheme.dockProgressSegments).toInt()
    Row(modifier.height(AppTheme.dockProgressHeight),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dockProgressGap)) {
        repeat(AppTheme.dockProgressSegments) { index ->
            Box(Modifier.weight(1f).height(AppTheme.dockProgressHeight)
                .background(if (index < filled) AppTheme.lime else AppTheme.dockSegmentEmpty))
        }
    }
}
