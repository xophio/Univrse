package com.universe.remote.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.universe.remote.ui.theme.AppTheme

private data class Pixel(val x: Int, val y: Int, val color: Color)

private fun disc(radius: Int, paint: (Int, Int) -> Color): List<Pixel> = buildList {
    for (y in -radius..radius) for (x in -radius..radius) {
        if (x * x + y * y <= radius * radius) add(Pixel(x, y, paint(x, y)))
    }
}

private fun speck(x: Int, y: Int): Int {
    val seed = x * 57 + y * 131 + x * y * 17
    return ((seed * seed * 13 + seed * 7) ushr 3) % 100
}

private val planetPixels = buildList {
    for (y in -14..14) for (x in -14..14) {
        val edge = 13 + when ((y + 16) % 6) { 0 -> -1; 3 -> 1; else -> 0 }
        val distance = x * x + y * y
        if (distance > edge * edge) continue
        val wave = when { x < -8 -> -1; x < -2 -> 0; x < 4 -> 1; x < 9 -> 0; else -> -1 }
        val band = y + wave
        val grain = speck(x, y)
        val color = when {
            distance > (edge - 1) * (edge - 1) -> AppTheme.ink
            band == -4 || band == 1 || band == 6 || (band == 9 && grain < 80) -> AppTheme.ink
            band < -9 -> if (grain < 30) AppTheme.blueShadow else AppTheme.sky
            band < -6 -> if (grain < 28) AppTheme.blueDeep else AppTheme.blue
            band < -3 -> if (grain < 25) AppTheme.mintShadow else AppTheme.mint
            band < 1 -> if (grain < 28) AppTheme.blueShadow else AppTheme.sky
            band < 6 -> if (grain < 26) AppTheme.mintShadow else AppTheme.mint
            band < 9 -> if (grain < 34) AppTheme.blueDeep else AppTheme.blue
            else -> if (grain < 34) AppTheme.blueShadow else AppTheme.sky
        }
        add(Pixel(x, y, color))
    }
}
private val yellowMoon = disc(5) { x, y ->
    val distance = x * x + y * y
    when {
        distance > 18 -> AppTheme.ink
        (x + 2) * (x + 2) + (y + 1) * (y + 1) <= 2 -> AppTheme.moonCrater
        (x - 2) * (x - 2) + (y - 1) * (y - 1) <= 2 -> AppTheme.moonCrater
        x == 0 && y == 2 -> AppTheme.blueDeep
        y > 2 || (x > 2 && y > -2) -> AppTheme.blue
        speck(x, y) < 16 -> AppTheme.mint
        else -> AppTheme.moonYellow
    }
}
private val redMoon = disc(4) { x, y ->
    val band = y + if (x > 1) 1 else 0
    when {
        x * x + y * y > 13 -> AppTheme.ink
        band == -1 || band == 2 -> AppTheme.moonRedShadow
        band < -1 -> if (speck(x, y) < 24) AppTheme.moonPink else AppTheme.moonRed
        band < 2 -> if (speck(x, y) < 22) AppTheme.mint else AppTheme.moonPink
        else -> if (speck(x, y) < 25) AppTheme.moonPink else AppTheme.moonRed
    }
}

private fun DrawScope.pixelDisc(pixels: List<Pixel>, center: Offset, cell: Float) {
    pixels.forEach { p ->
        drawRect(p.color, Offset(center.x + p.x * cell - cell / 2, center.y + p.y * cell - cell / 2), Size(cell, cell))
    }
}

private fun DrawScope.pixelStar(x: Float, y: Float, arm: Float, color: Color, stroke: Float) {
    drawLine(color, Offset(x - arm, y - arm), Offset(x + arm, y + arm), stroke)
    drawLine(color, Offset(x - arm, y + arm), Offset(x + arm, y - arm), stroke)
}

/** Static Canvas illustration; color/shape lists are constructed once above, outside drawing. */
@Composable
fun PixelPlanet(modifier: Modifier = Modifier) {
    val planet = remember { planetPixels }
    val moonOne = remember { yellowMoon }
    val moonTwo = remember { redMoon }
    val density = LocalDensity.current
    val orbitStroke = remember(density) { Stroke(with(density) { AppTheme.hairline.toPx() }) }
    val starStroke = remember(density) { with(density) { AppTheme.starStroke.toPx() } }
    Box(modifier.drawWithCache {
        val center = Offset(size.width * .5f, size.height * .45f)
        val radius = size.width.coerceAtMost(size.height) * .50f
        val glow = Brush.radialGradient(listOf(AppTheme.oliveGlow, AppTheme.transparent), center, radius)
        onDrawBehind { drawRect(glow) }
    }) {
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width * .5f, size.height * .45f)
        val base = size.width.coerceAtMost(size.height)
        drawCircle(AppTheme.orbit.copy(alpha = .48f), base * .43f, center, style = orbitStroke)
        drawCircle(AppTheme.orbit.copy(alpha = .7f), base * .32f, center, style = orbitStroke)
        drawCircle(AppTheme.orbit.copy(alpha = .26f), base * .55f, center, style = orbitStroke)
        pixelDisc(planet, center, base * .0175f)
        pixelDisc(moonOne, Offset(center.x - base * .36f, center.y - base * .28f), base * .0096f)
        pixelDisc(moonTwo, Offset(center.x + base * .26f, center.y - base * .20f), base * .009f)
        pixelStar(center.x + base * .10f, center.y - base * .46f, base * .017f, AppTheme.blue, starStroke)
        pixelStar(center.x - base * .28f, center.y + base * .26f, base * .013f, AppTheme.moonYellow, starStroke)
        pixelStar(center.x + base * .42f, center.y + base * .08f, base * .013f, AppTheme.moonYellow, starStroke)
    }
    }
}
