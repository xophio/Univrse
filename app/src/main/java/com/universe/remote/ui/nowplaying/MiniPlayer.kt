package com.universe.remote.ui.nowplaying

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.domain.PlaybackState
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.theme.AppTheme

@Composable
fun MiniPlayer(state: PlaybackState, viewModel: NowPlayingViewModel, onOpen: () -> Unit,
    modifier: Modifier = Modifier) {
    val artwork by viewModel.artwork.collectAsStateWithLifecycle()
    Box(modifier.height(AppTheme.miniHeight).clip(AppTheme.card)
        .background(AppTheme.navSolid).border(AppTheme.hairline, AppTheme.border, AppTheme.card)
        .clickable(role = Role.Button, onClickLabel = "Open Now Playing", onClick = onOpen)) {
        Row(Modifier.fillMaxSize().padding(horizontal = AppTheme.medium),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
            Box(Modifier.size(AppTheme.miniCover).clip(AppTheme.artShape).background(AppTheme.surface),
                contentAlignment = Alignment.Center) {
                if (artwork == null) Label("♫", AppTheme.headline)
                else Image(artwork!!.asImageBitmap(), contentDescription = "Album cover",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
                BasicText(state.title, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(state.artist, style = AppTheme.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(AppTheme.iconButton).clip(CircleShape).background(AppTheme.lime)
                .clickable(role = Role.Button, onClickLabel = if (state.paused) "Play" else "Pause") {
                    viewModel.togglePlayback()
                }, contentAlignment = Alignment.Center) {
                PlayPauseIcon(state.paused, AppTheme.ink, AppTheme.navGlyph)
            }
        }
        MiniProgress(state.durationMs, viewModel, Modifier.fillMaxWidth().align(Alignment.BottomStart))
    }
}

@Composable
private fun MiniProgress(durationMs: Long, viewModel: NowPlayingViewModel, modifier: Modifier) {
    val position by viewModel.positionMs.collectAsStateWithLifecycle()
    val fraction = if (durationMs > 0) (position.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(modifier.height(AppTheme.miniProgress).background(AppTheme.border)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(AppTheme.lime))
    }
}
