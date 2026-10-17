package com.universe.remote.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import com.universe.remote.ui.theme.AppTheme

@Composable
fun PlayPauseIcon(paused: Boolean, color: Color, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(iconSize).drawWithCache {
        val triangle = Path().apply {
            moveTo(this@drawWithCache.size.width * .30f, this@drawWithCache.size.height * .14f)
            lineTo(this@drawWithCache.size.width * .81f, this@drawWithCache.size.height * .5f)
            lineTo(this@drawWithCache.size.width * .30f, this@drawWithCache.size.height * .86f)
            close()
        }
        onDrawBehind {
            if (paused) drawPath(triangle, color)
            else {
                drawRect(color, Offset(size.width * .27f, size.height * .18f),
                    Size(size.width * .17f, size.height * .64f))
                drawRect(color, Offset(size.width * .56f, size.height * .18f),
                    Size(size.width * .17f, size.height * .64f))
            }
        }
    })
}

@Composable
fun SkipIcon(next: Boolean, color: Color, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(iconSize).drawWithCache {
        val triangle = Path().apply {
            if (next) {
                moveTo(this@drawWithCache.size.width * .19f, this@drawWithCache.size.height * .18f)
                lineTo(this@drawWithCache.size.width * .73f, this@drawWithCache.size.height * .5f)
                lineTo(this@drawWithCache.size.width * .19f, this@drawWithCache.size.height * .82f)
            } else {
                moveTo(this@drawWithCache.size.width * .81f, this@drawWithCache.size.height * .18f)
                lineTo(this@drawWithCache.size.width * .27f, this@drawWithCache.size.height * .5f)
                lineTo(this@drawWithCache.size.width * .81f, this@drawWithCache.size.height * .82f)
            }
            close()
        }
        onDrawBehind {
            drawPath(triangle, color)
            val x = if (next) size.width * .78f else size.width * .12f
            drawRect(color, Offset(x, size.height * .18f), Size(size.width * .10f, size.height * .64f))
        }
    })
}

@Composable
fun RepeatIcon(track: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(AppTheme.shuffleGlyph)) {
        val w = size.width
        val h = size.height
        val s = AppTheme.iconStroke.toPx()
        drawLine(color, Offset(w * .19f, h * .34f), Offset(w * .82f, h * .34f), s)
        drawLine(color, Offset(w * .82f, h * .34f), Offset(w * .68f, h * .18f), s)
        drawLine(color, Offset(w * .82f, h * .34f), Offset(w * .68f, h * .5f), s)
        drawLine(color, Offset(w * .81f, h * .68f), Offset(w * .18f, h * .68f), s)
        drawLine(color, Offset(w * .18f, h * .68f), Offset(w * .32f, h * .52f), s)
        drawLine(color, Offset(w * .18f, h * .68f), Offset(w * .32f, h * .84f), s)
        if (track) drawCircle(color, radius = w * .07f, center = Offset(w * .5f, h * .5f))
    }
}

@Composable
fun DownChevronIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(AppTheme.navGlyph)) {
        val s = AppTheme.iconStroke.toPx()
        drawLine(AppTheme.foreground, Offset(size.width * .18f, size.height * .36f),
            Offset(size.width * .5f, size.height * .68f), s)
        drawLine(AppTheme.foreground, Offset(size.width * .5f, size.height * .68f),
            Offset(size.width * .82f, size.height * .36f), s)
    }
}
