package com.universe.remote.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.universe.remote.ui.theme.AppTheme
import androidx.compose.foundation.layout.height
import kotlin.random.Random

private val titleGlyphs = listOf(
    listOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"), // U
    listOf("00000", "00000", "11010", "10101", "10001", "10001", "10001"), // n
    listOf("00100", "00000", "01100", "00100", "00100", "00100", "01110"), // i
    listOf("00000", "00000", "10001", "10001", "10001", "01010", "00100"), // v
    listOf("00000", "00000", "01110", "10001", "11111", "10000", "01111"), // e
    listOf("00000", "00000", "10110", "11001", "10000", "10000", "10000"), // r
    listOf("00000", "00000", "01111", "10000", "01110", "00001", "11110"), // s
    listOf("00000", "00000", "01110", "10001", "11111", "10000", "01111"), // e
)

@Composable
fun PixelTitle(modifier: Modifier = Modifier) {
    Canvas(modifier.height(AppTheme.titleHeight).semantics { contentDescription = "Universe" }) {
        val cell = AppTheme.titlePixel.toPx()
        titleGlyphs.forEachIndexed { letter, glyph ->
            glyph.forEachIndexed { y, row ->
                row.forEachIndexed { x, pixel ->
                    if (pixel == '1') drawRect(AppTheme.foreground,
                        topLeft = Offset((letter * 6 + x) * cell, y * cell),
                        size = Size(cell, cell))
                }
            }
        }
    }
}

@Composable
fun HomeGrainOverlay(modifier: Modifier = Modifier) {
    Box(modifier.drawWithCache {
        val random = Random(3405)
        val points = List((size.width * size.height / 180).toInt()) {
            Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
        }
        val color = AppTheme.grain.copy(alpha = AppTheme.homeGrainAlpha)
        onDrawBehind { drawPoints(points, PointMode.Points, color, strokeWidth = 1f) }
    })
}

@Composable
fun HeartIcon(modifier: Modifier = Modifier) {
    Box(modifier.size(AppTheme.heartWidth).drawWithCache {
        val path = Path().apply {
            moveTo(size.width * .5f, size.height * .9f)
            cubicTo(size.width * .2f, size.height * .68f, 0f, size.height * .48f,
                size.width * .08f, size.height * .25f)
            cubicTo(size.width * .17f, -size.height * .04f, size.width * .45f, size.height * .08f,
                size.width * .5f, size.height * .27f)
            cubicTo(size.width * .55f, size.height * .08f, size.width * .83f, -size.height * .04f,
                size.width * .92f, size.height * .25f)
            cubicTo(size.width, size.height * .48f, size.width * .8f, size.height * .68f,
                size.width * .5f, size.height * .9f)
            close()
        }
        onDrawBehind { drawPath(path, AppTheme.lime) }
    })
}

@Composable
fun BellIcon(modifier: Modifier = Modifier) {
    Box(modifier.size(AppTheme.bellSize).drawWithCache {
        val bell = Path().apply {
            moveTo(size.width * .22f, size.height * .72f)
            lineTo(size.width * .31f, size.height * .62f)
            lineTo(size.width * .31f, size.height * .38f)
            cubicTo(size.width * .31f, size.height * .1f, size.width * .69f, size.height * .1f,
                size.width * .69f, size.height * .38f)
            lineTo(size.width * .69f, size.height * .62f)
            lineTo(size.width * .78f, size.height * .72f)
            close()
        }
        val stroke = Stroke(AppTheme.iconStroke.toPx())
        onDrawBehind {
            drawPath(bell, AppTheme.foreground, style = stroke)
            drawLine(AppTheme.foreground, Offset(size.width * .43f, size.height * .81f),
                Offset(size.width * .57f, size.height * .81f), AppTheme.iconStroke.toPx())
        }
    })
}

@Composable
fun ShuffleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(AppTheme.shuffleGlyph)) {
        val w = size.width
        val h = size.height
        val stroke = AppTheme.shuffleStroke.toPx()
        drawLine(AppTheme.foreground, Offset(w * .1f, h * .24f), Offset(w * .28f, h * .24f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .28f, h * .24f), Offset(w * .72f, h * .76f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .72f, h * .76f), Offset(w * .9f, h * .76f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .1f, h * .76f), Offset(w * .28f, h * .76f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .28f, h * .76f), Offset(w * .72f, h * .24f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .72f, h * .24f), Offset(w * .9f, h * .24f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .78f, h * .11f), Offset(w * .9f, h * .24f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .78f, h * .37f), Offset(w * .9f, h * .24f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .78f, h * .63f), Offset(w * .9f, h * .76f), stroke)
        drawLine(AppTheme.foreground, Offset(w * .78f, h * .89f), Offset(w * .9f, h * .76f), stroke)
    }
}

@Composable
fun RainbowIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(AppTheme.navGlyph)) {
        val stroke = Stroke(AppTheme.navStroke.toPx())
        drawArc(AppTheme.foreground, 180f, 180f, false, Offset(size.width * .05f, size.height * .1f),
            Size(size.width * .9f, size.height * .9f), style = stroke)
        drawArc(AppTheme.foreground, 180f, 180f, false, Offset(size.width * .22f, size.height * .27f),
            Size(size.width * .56f, size.height * .56f), style = stroke)
        drawArc(AppTheme.foreground, 180f, 180f, false, Offset(size.width * .39f, size.height * .44f),
            Size(size.width * .22f, size.height * .22f), style = stroke)
    }
}

@Composable
fun StackedCardsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(AppTheme.navGlyph)) {
        val stroke = Stroke(AppTheme.navStroke.toPx())
        drawRoundRect(AppTheme.foreground, Offset(size.width * .29f, size.height * .09f),
            Size(size.width * .61f, size.height * .64f), CornerRadius(size.width * .06f), style = stroke)
        drawRoundRect(AppTheme.foreground, Offset(size.width * .1f, size.height * .27f),
            Size(size.width * .61f, size.height * .64f), CornerRadius(size.width * .06f), style = stroke)
    }
}
