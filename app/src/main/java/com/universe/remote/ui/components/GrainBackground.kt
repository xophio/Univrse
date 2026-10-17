package com.universe.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PointMode
import com.universe.remote.ui.theme.AppTheme
import kotlin.random.Random

@Composable
fun GrainBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(AppTheme.background).drawWithCache {
        val random = Random(81)
        val points = List((size.width * size.height / 120).toInt()) {
            Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        }
        val color = AppTheme.grain.copy(alpha = AppTheme.grainAlpha)
        onDrawBehind { drawPoints(points, PointMode.Points, color, strokeWidth = 1f) }
    }, content = content)
}
