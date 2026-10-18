package com.universe.remote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.universe.remote.ui.theme.AppTheme

enum class UniverseGlyph {
    HOME, STATS, LIBRARY, SEARCH, PLAY, PAUSE, PREVIOUS, NEXT, SHUFFLE, REPEAT,
    VOLUME, QUEUE, HEART, MORE, ADD, DOWNLOAD, SHARE, ALERTS, PROFILE, SETTINGS,
    COLLAPSE, CLOSE, CHEVRON_RIGHT, MUSIC
}

/** The reference sheet's rounded, 24-unit icon language, drawn as native Compose paths. */
@Composable
fun UniverseIcon(
    glyph: UniverseGlyph,
    modifier: Modifier = Modifier,
    size: Dp = AppTheme.navGlyph,
    color: Color = AppTheme.foreground,
    filled: Boolean = false,
    repeatOne: Boolean = false,
    strokeWidth: Dp = AppTheme.navStroke,
) {
    Box(modifier.size(size).drawWithCache {
        val sx = this.size.width / 24f
        val sy = this.size.height / 24f
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy
        fun p(block: Path.() -> Unit) = Path().apply(block)
        fun Path.m(a: Float, b: Float) = moveTo(x(a), y(b))
        fun Path.l(a: Float, b: Float) = lineTo(x(a), y(b))
        fun Path.c(a: Float, b: Float, d: Float, e: Float, f: Float, g: Float) =
            cubicTo(x(a), y(b), x(d), y(e), x(f), y(g))
        fun Path.oval(a: Float, b: Float, c: Float, d: Float) =
            addOval(Rect(x(a), y(b), x(c), y(d)))
        fun Path.round(a: Float, b: Float, c: Float, d: Float, r: Float) =
            addRoundRect(RoundRect(Rect(x(a), y(b), x(c), y(d)), CornerRadius(x(r), y(r))))
        val strokes = mutableListOf<Path>()
        val fills = mutableListOf<Path>()
        fun s(block: Path.() -> Unit) { strokes += p(block) }
        fun f(block: Path.() -> Unit) { fills += p(block) }
        when (glyph) {
            UniverseGlyph.HOME -> {
                s { m(3.5f,10.5f); l(3.5f,19f); c(3.5f,20.1f,4.4f,21f,5.5f,21f); l(18.5f,21f); c(19.6f,21f,20.5f,20.1f,20.5f,19f); l(20.5f,10.5f); c(20.5f,9.9f,20.2f,9.3f,19.8f,8.9f); l(13.3f,3.5f); c(12.5f,2.8f,11.5f,2.8f,10.7f,3.5f); l(4.2f,8.9f); c(3.8f,9.3f,3.5f,9.9f,3.5f,10.5f) }
                s { m(10f,20.8f); l(10f,16.8f); c(10f,14.1f,14f,14.1f,14f,16.8f); l(14f,20.8f) }
            }
            UniverseGlyph.STATS -> {
                s { m(2f,17.5f); c(2f,4.5f,22f,4.5f,22f,17.5f) }
                s { m(6f,17.5f); c(6f,9.5f,18f,9.5f,18f,17.5f) }
                s { m(10f,17.5f); c(10f,15f,14f,15f,14f,17.5f) }
            }
            UniverseGlyph.LIBRARY -> {
                s { m(7f,6f); l(17.5f,6f); c(19.5f,6f,20.5f,7f,20.5f,9f); l(20.5f,17f) }
                s { round(3.5f,9f,16.5f,21f,2.3f) }
            }
            UniverseGlyph.SEARCH -> {
                s { oval(3.2f,3.2f,16.8f,16.8f) }
                s { m(15.3f,15.3f); l(21f,21f) }
            }
            UniverseGlyph.PLAY -> f {
                m(7f,3.6f); c(7f,2.6f,8.2f,2.1f,9.1f,2.7f); l(20.1f,10.4f)
                c(21.3f,11.2f,21.3f,12.8f,20.1f,13.6f); l(9.1f,21.3f)
                c(8.2f,21.9f,7f,21.4f,7f,20.4f); close()
            }
            UniverseGlyph.PAUSE -> {
                f { round(4.8f,3f,10.5f,21f,2.5f) }
                f { round(13.5f,3f,19.2f,21f,2.5f) }
            }
            UniverseGlyph.PREVIOUS, UniverseGlyph.NEXT -> {
                if (glyph == UniverseGlyph.PREVIOUS) {
                    s { m(4.5f,4f); l(4.5f,20f) }
                    f { m(18.8f,4f); c(18.8f,3f,17.7f,2.5f,16.8f,3.1f); l(6.8f,10.5f); c(5.7f,11.3f,5.7f,12.7f,6.8f,13.5f); l(16.8f,20.9f); c(17.7f,21.5f,18.8f,21f,18.8f,20f); close() }
                } else {
                    f { m(5.2f,4f); c(5.2f,3f,6.3f,2.5f,7.2f,3.1f); l(17.2f,10.5f); c(18.3f,11.3f,18.3f,12.7f,17.2f,13.5f); l(7.2f,20.9f); c(6.3f,21.5f,5.2f,21f,5.2f,20f); close() }
                    s { m(19.5f,4f); l(19.5f,20f) }
                }
            }
            UniverseGlyph.SHUFFLE -> {
                s { m(2f,6f); l(5.5f,6f); c(10f,6f,11f,18f,17.5f,18f); l(21f,18f) }
                s { m(18f,15f); l(21f,18f); l(18f,21f) }
                s { m(2f,18f); l(5.5f,18f); c(8f,18f,9.6f,15.5f,11f,13f) }
                s { m(13.2f,9f); c(14.5f,7f,15.8f,6f,17.5f,6f); l(21f,6f) }
                s { m(18f,3f); l(21f,6f); l(18f,9f) }
            }
            UniverseGlyph.REPEAT -> {
                s { m(3f,10f); l(3f,8f); c(3f,6.3f,4.3f,5f,6f,5f); l(20f,5f) }
                s { m(17f,2f); l(20f,5f); l(17f,8f) }
                s { m(21f,14f); l(21f,16f); c(21f,17.7f,19.7f,19f,18f,19f); l(4f,19f) }
                s { m(7f,16f); l(4f,19f); l(7f,22f) }
                if (repeatOne) f { oval(10.8f,10.8f,13.2f,13.2f) }
            }
            UniverseGlyph.VOLUME -> {
                s { m(3f,9f); l(7f,9f); l(13f,4f); l(13f,20f); l(7f,15f); l(3f,15f); close() }
                s { m(17f,8f); c(19.5f,10f,19.5f,14f,17f,16f) }
            }
            UniverseGlyph.QUEUE -> {
                s { m(3f,6f); l(19f,6f); m(3f,11f); l(16f,11f); m(3f,16f); l(12f,16f) }
                s { oval(16f,15f,21f,20f) }
            }
            UniverseGlyph.HEART -> {
                val heart: Path.() -> Unit = { m(12f,21f); c(8f,17.8f,2f,13.2f,2f,8.3f); c(2f,2f,9.2f,1.5f,12f,6.2f); c(14.8f,1.5f,22f,2f,22f,8.3f); c(22f,13.2f,16f,17.8f,12f,21f); close() }
                if (filled) f(heart) else s(heart)
            }
            UniverseGlyph.MORE -> f { oval(2.4f,9.9f,6.6f,14.1f); oval(9.9f,9.9f,14.1f,14.1f); oval(17.4f,9.9f,21.6f,14.1f) }
            UniverseGlyph.ADD -> s { m(12f,3f); l(12f,21f); m(3f,12f); l(21f,12f) }
            UniverseGlyph.DOWNLOAD -> {
                s { m(12f,3f); l(12f,16f); m(7f,11f); l(12f,16f); l(17f,11f); m(4f,21f); l(20f,21f) }
            }
            UniverseGlyph.SHARE -> {
                s { m(6.5f,12f); l(17f,5.5f); m(6.5f,12f); l(17f,18.5f) }
                s { oval(2f,9f,8f,15f); oval(16f,2f,22f,8f); oval(16f,16f,22f,22f) }
            }
            UniverseGlyph.ALERTS -> {
                s { m(3f,18f); l(5f,16f); l(5f,10f); c(5f,1.5f,19f,1.5f,19f,10f); l(19f,16f); l(21f,18f); close() }
                s { m(9.5f,21f); c(10.5f,22.5f,13.5f,22.5f,14.5f,21f) }
            }
            UniverseGlyph.PROFILE -> {
                s { oval(8f,2.5f,16f,10.5f) }
                s { m(3f,21f); c(3f,10.5f,21f,10.5f,21f,21f) }
            }
            UniverseGlyph.SETTINGS -> {
                s { m(3f,7f); l(21f,7f); m(3f,17f); l(21f,17f) }
                f { oval(12.3f,4.3f,17.7f,9.7f); oval(6.3f,14.3f,11.7f,19.7f) }
            }
            UniverseGlyph.COLLAPSE -> s { m(4f,8f); l(12f,16f); l(20f,8f) }
            UniverseGlyph.CLOSE -> s { m(4f,4f); l(20f,20f); m(20f,4f); l(4f,20f) }
            UniverseGlyph.CHEVRON_RIGHT -> s { m(9f,4f); l(17f,12f); l(9f,20f) }
            UniverseGlyph.MUSIC -> {
                s { m(10f,17f); l(10f,5f); l(20f,3f); l(20f,15f) }
                f { oval(3f,15f,10f,21f); oval(13f,13f,20f,19f) }
            }
        }
        val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        onDrawBehind {
            for (path in strokes) drawPath(path, color, style = stroke)
            for (path in fills) drawPath(path, color)
        }
    })
}
