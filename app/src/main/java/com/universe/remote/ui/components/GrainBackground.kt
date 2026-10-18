package com.universe.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import com.universe.remote.ui.theme.AppTheme
import kotlin.random.Random

// Same physical-pixel density as before; a small texture replaces thousands of draw points.
fun grainTexture(seed: Int, areaPerPoint: Int, alpha: Float): Modifier = Modifier.drawWithCache {
    val edge = 256
    val bitmap = ImageBitmap(edge, edge)
    val random = Random(seed)
    val points = List(edge * edge / areaPerPoint) { Offset(random.nextFloat() * edge, random.nextFloat() * edge) }
    CanvasDrawScope().draw(this, layoutDirection, Canvas(bitmap), Size(edge.toFloat(), edge.toFloat())) {
        drawPoints(points, PointMode.Points, AppTheme.grain.copy(alpha = alpha), strokeWidth = 1f)
    }
    val brush = ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }
}

@Composable
fun GrainBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(AppTheme.background).then(grainTexture(81, 120, AppTheme.grainAlpha)), content = content)
}
