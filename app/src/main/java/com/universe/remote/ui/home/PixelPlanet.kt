package com.universe.remote.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import com.universe.remote.ui.components.motionEnabled
import com.universe.remote.ui.nowplaying.NowPlayingViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.ceil
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.universe.remote.ui.theme.AppTheme

private val spinCache = android.util.LruCache<Int, Array<ImageBitmap>>(3)
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

@Composable
fun PixelPlanet(modifier: Modifier = Modifier, player: NowPlayingViewModel? = null) {
    val enabled = motionEnabled()
    val state = player?.controls?.collectAsStateWithLifecycle()?.value
    val tone = player?.coverTone?.collectAsStateWithLifecycle()?.value ?: AppTheme.blue
    val filter = remember(tone) { androidx.compose.ui.graphics.ColorFilter.tint(tone, androidx.compose.ui.graphics.BlendMode.SrcIn) }
    var oldFilter by remember { mutableStateOf(filter) }
    var lastFilter by remember { mutableStateOf(filter) }
    val paletteFade = remember { Animatable(1f) }
    LaunchedEffect(filter, enabled) {
        oldFilter = lastFilter
        paletteFade.snapTo(if (enabled) 0f else 1f)
        lastFilter = filter
        if (enabled) paletteFade.animateTo(1f, tween(900))
    }
    val speed = remember { Animatable(0f) }
    val phase = remember { mutableFloatStateOf(0f) }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(state?.paused, enabled) {
        if (!enabled) speed.snapTo(0f)
        else speed.animateTo(if (state?.paused == false) 1f else 0f, tween(900))
    }
    if (enabled && (state?.paused == false || speed.value > 0f)) {
        val clock = rememberInfiniteTransition(label = "orbit clock")
        val tick = clock.animateFloat(0f, 1f,
            infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "orbit phase")
        LaunchedEffect(clock) {
            var previous = tick.value
            snapshotFlow { tick.value }.collect { current ->
                val elapsed = ((current - previous + 1f) % 1f) * 60f
                phase.floatValue += elapsed.coerceAtMost(.05f) * speed.value
                previous = current
            }
        }
    }
    LaunchedEffect(state?.paused, enabled) {
        if (enabled && state?.paused == false) {
            pulse.snapTo(1.035f)
            pulse.animateTo(1f, spring())
        } else pulse.snapTo(1f)
    }
    Box(modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.drawWithCache {
        val center = Offset(size.width * .5f, size.height * .45f)
        val base = size.minDimension
        val stroke = Stroke(AppTheme.hairline.toPx())
        val glow = Brush.radialGradient(listOf(AppTheme.oliveGlow, AppTheme.transparent), center, base * .5f)
        // Scale the planet to its host. The compact Now Playing header previously
        // used the full hero diameter, so most of the planet was clipped away.
        val planetDiameter = min(AppTheme.heroPlanet.toPx(), base * .55f)
        val cell = planetDiameter / 30f
        val edge = ceil(cell * 30).toInt().coerceAtLeast(1)
        // Pre-render the full chunky spin cycle. Per-frame work is one bitmap lookup/blit.
        val lookup = Array(29 * 29) { AppTheme.ink }
        planetPixels.forEach { lookup[(it.y + 14) * 29 + it.x + 14] = it.color }
        val frames = spinCache.get(cell.toBits()) ?: Array(16) { frame ->
            ImageBitmap(edge, edge).also { bitmap ->
                CanvasDrawScope().draw(this, layoutDirection, BitmapCanvas(bitmap), Size(edge.toFloat(), edge.toFloat())) {
                    var i = 0
                    while (i < planetPixels.size) {
                        val p = planetPixels[i++]
                        val shift = ((p.x + frame * 2 + 30) % 29) - 14
                        val source = lookup[(p.y + 14) * 29 + shift + 14]
                        val color = if (p.color == AppTheme.ink) p.color else source
                        drawRect(color, Offset(edge / 2f + p.x * cell - cell / 2, edge / 2f + p.y * cell - cell / 2), Size(cell, cell))
                    }
                }
            }
        }
        spinCache.put(cell.toBits(), frames)
        fun moon(pixels: List<Pixel>, cellSize: Float): ImageBitmap {
            val width = ceil(cellSize * 12).toInt().coerceAtLeast(1)
            return ImageBitmap(width, width).also { bitmap ->
                CanvasDrawScope().draw(this, layoutDirection, BitmapCanvas(bitmap), Size(width.toFloat(), width.toFloat())) {
                    pixelDisc(pixels, Offset(width / 2f, width / 2f), cellSize)
                }
            }
        }
        val moonA = moon(yellowMoon, base * .0096f)
        val moonB = moon(redMoon, base * .009f)
        val planetOrigin = Offset(center.x - edge / 2f, center.y - edge / 2f)
        val starStroke = AppTheme.starStroke.toPx()
        onDrawBehind {
            val t = phase.floatValue
            drawRect(glow)
            drawCircle(AppTheme.orbit, base * .43f, center, alpha = .48f, style = stroke)
            drawCircle(AppTheme.orbit, base * .32f, center, alpha = .7f, style = stroke)
            drawCircle(AppTheme.orbit, base * .55f, center, alpha = .26f, style = stroke)
            drawImage(frames[(t * 6).toInt().mod(16)], topLeft = planetOrigin)
            val frame = frames[(t * 6).toInt().mod(16)]
            drawImage(frame, topLeft = planetOrigin, colorFilter = oldFilter, alpha = .18f * (1f - paletteFade.value))
            drawImage(frame, topLeft = planetOrigin, colorFilter = filter, alpha = .18f * paletteFade.value)
            val a = t * .17f + 3.8f
            val b = -t * .27f - .65f
            drawImage(moonA, topLeft = Offset(center.x + cos(a) * base * .43f - moonA.width / 2,
                center.y + sin(a) * base * .43f - moonA.height / 2))
            drawImage(moonB, topLeft = Offset(center.x + cos(b) * base * .32f - moonB.width / 2,
                center.y + sin(b) * base * .32f - moonB.height / 2))
            pixelStar(center.x + base * .10f, center.y - base * .46f, base * .017f,
                AppTheme.blue.copy(alpha = .65f + .35f * sin(t * 2 + 1.7f)), starStroke)
            pixelStar(center.x - base * .28f, center.y + base * .26f, base * .013f,
                AppTheme.moonYellow.copy(alpha = .65f + .35f * sin(t * 1.6f + 4.2f)), starStroke)
            pixelStar(center.x + base * .42f, center.y + base * .08f, base * .013f,
                AppTheme.moonYellow.copy(alpha = .65f + .35f * sin(t * 2.3f + .3f)), starStroke)
        }
    })
}
