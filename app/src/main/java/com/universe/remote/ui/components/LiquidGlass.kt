package com.universe.remote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.universe.remote.ui.theme.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalLiquidGlassHaze = compositionLocalOf { HazeState() }

/** Layered translucent surface used for the app's iOS-style glass controls. */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color = AppTheme.liquidGlass,
): Modifier {
    val hazeState = LocalLiquidGlassHaze.current
    val hazeStyle = HazeStyle(
        backgroundColor = AppTheme.dock,
        tint = HazeTint(tint.copy(alpha = .32f)),
        blurRadius = AppTheme.glassBlurRadius,
        noiseFactor = .08f,
        fallbackTint = HazeTint(tint.copy(alpha = .88f)),
    )
    return shadow(
    elevation = AppTheme.glassShadowElevation,
    shape = shape,
    clip = false,
    ambientColor = AppTheme.liquidGlassShadow,
    spotColor = AppTheme.liquidGlassShadow,
).clip(shape).hazeEffect(state = hazeState, style = hazeStyle) {
    blurEnabled = true
}.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val topSheen = Brush.radialGradient(
        colors = listOf(AppTheme.liquidGlassTop.copy(alpha = .24f), Color.Transparent),
        center = Offset(size.width * .18f, -size.height * .3f),
        radius = size.maxDimension * .95f,
    )
    val edge = Brush.linearGradient(
        colors = listOf(
            AppTheme.liquidGlassEdge,
            AppTheme.glassBorder.copy(alpha = .38f),
            AppTheme.liquidGlassReflection,
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val stroke = Stroke(AppTheme.glassEdgeWidth.toPx())
    fun DrawScope.paintOutline(brush: Brush, style: DrawStyle = Fill) {
        when (val value = outline) {
            is Outline.Rectangle -> drawRect(brush, value.rect.topLeft, value.rect.size, style = style)
            is Outline.Rounded -> drawRoundRect(brush,
                topLeft = Offset(value.roundRect.left, value.roundRect.top),
                size = Size(value.roundRect.width, value.roundRect.height),
                cornerRadius = value.roundRect.topLeftCornerRadius, style = style)
            is Outline.Generic -> drawPath(value.path, brush, style = style)
        }
    }
    onDrawBehind {
        paintOutline(topSheen)
        paintOutline(edge, stroke)
    }
}
}
