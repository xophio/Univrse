package com.universe.remote.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.universe.remote.ui.theme.AppTheme

/** Opens an anchored menu from its top-right trigger without changing popup behavior. */
@Composable
fun AnimatedPopup(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    alignment: Alignment = Alignment.TopEnd,
    offset: IntOffset = IntOffset.Zero,
    content: @Composable () -> Unit,
) {
    if (!visible) return

    val motion = motionEnabled()
    var entered by remember { mutableStateOf(!motion) }
    LaunchedEffect(motion) { entered = true }
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (motion) tween(
            durationMillis = AppTheme.menuOpenMs,
            easing = FastOutSlowInEasing,
        ) else snap(),
        label = "anchored menu opening",
    )
    val slidePx = with(LocalDensity.current) { AppTheme.menuOpenSlide.toPx() }

    Popup(alignment = alignment, offset = offset, onDismissRequest = onDismissRequest) {
        androidx.compose.foundation.layout.Box(Modifier.graphicsLayer {
            alpha = progress
            val scale = AppTheme.menuInitialScale +
                (1f - AppTheme.menuInitialScale) * progress
            scaleX = scale
            scaleY = scale
            translationY = -slidePx * (1f - progress)
            transformOrigin = TransformOrigin(1f, 0f)
        }) {
            content()
        }
    }
}
