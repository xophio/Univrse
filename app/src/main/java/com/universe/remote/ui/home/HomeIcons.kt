package com.universe.remote.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.components.grainTexture
import com.universe.remote.ui.components.motionEnabled
import com.universe.remote.ui.theme.AppTheme

@Composable
fun PixelTitle(modifier: Modifier = Modifier) {
    val title = "Universe"
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val fullWidth = remember(measurer, density) {
        measurer.measure(title, style = AppTheme.universeTitle).size.width
    }
    BoxWithConstraints(modifier.height(AppTheme.titleHeight)) {
        val available = with(density) { maxWidth.toPx() }
        val scale = ((available - 2f) / fullWidth.coerceAtLeast(1)).coerceIn(0.1f, 1f) * .97f
        androidx.compose.foundation.text.BasicText(title,
            style = AppTheme.universeTitle.copy(fontSize = AppTheme.universeTitle.fontSize * scale),
            maxLines = 1, softWrap = false,
            modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun HomeGrainOverlay(modifier: Modifier = Modifier) {
    Box(modifier.then(grainTexture(3405, 180, AppTheme.homeGrainAlpha)))
}

@Composable
fun HeartIcon(modifier: Modifier = Modifier, color: Color = AppTheme.lime,
    filled: Boolean = true, iconSize: Dp = AppTheme.heartWidth) {
    val enabled = motionEnabled()
    val bounce = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(filled) }
    LaunchedEffect(filled, enabled) {
        if (enabled && filled && !previous) {
            bounce.snapTo(1.2f)
            bounce.animateTo(1f, spring(dampingRatio = .45f))
        } else bounce.snapTo(1f)
        previous = filled
    }
    UniverseIcon(UniverseGlyph.HEART,
        modifier.graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        iconSize, color, filled)
}

@Composable fun BellIcon(modifier: Modifier = Modifier) =
    UniverseIcon(UniverseGlyph.ALERTS, modifier, AppTheme.bellSize)

@Composable fun ShuffleIcon(modifier: Modifier = Modifier, color: Color = AppTheme.foreground) =
    UniverseIcon(UniverseGlyph.SHUFFLE, modifier, AppTheme.shuffleGlyph, color)

@Composable fun RainbowIcon(modifier: Modifier = Modifier, color: Color = AppTheme.foreground) =
    UniverseIcon(UniverseGlyph.STATS, modifier, AppTheme.navGlyph, color)

@Composable fun StackedCardsIcon(modifier: Modifier = Modifier, color: Color = AppTheme.foreground) =
    UniverseIcon(UniverseGlyph.LIBRARY, modifier, AppTheme.navGlyph, color)
