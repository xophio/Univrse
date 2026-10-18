package com.universe.remote.ui.home

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.universe.remote.domain.WebPlaylist
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PlanetPalette(
    val dark: Color,
    val mid: Color,
    val light: Color,
    val accent: Color,
)

@Composable
fun PlaylistPlanetItem(
    playlist: WebPlaylist,
    selected: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.width(AppTheme.playlistItemWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            PlaylistPlanet(
                playlist.coverUrl,
                Modifier.size(AppTheme.playlistPlanet)
                    .border(if (selected) AppTheme.playlistRing else AppTheme.hairline,
                        if (selected) AppTheme.lime else AppTheme.dockBorder, CircleShape)
                    .combinedClickable(
                        role = Role.Button,
                        onClickLabel = "Play ${playlist.name}",
                        onLongClickLabel = "Playlist options for ${playlist.name}",
                        onClick = onPlay,
                        onLongClick = onLongPress,
                    ),
            )
            if (selected) {
                Box(Modifier.size(AppTheme.playlistBadge).background(AppTheme.lime, CircleShape),
                    contentAlignment = Alignment.Center) {
                    UniverseIcon(UniverseGlyph.PLAY, size = AppTheme.navGlyph, color = AppTheme.ink)
                }
            }
        }
        BasicText(playlist.name, style = AppTheme.body.copy(textAlign = TextAlign.Center),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(AppTheme.playlistItemWidth))
        BasicText("${playlist.trackCount ?: 0} trk",
            style = AppTheme.time.copy(textAlign = TextAlign.Center), maxLines = 1,
            modifier = Modifier.width(AppTheme.playlistItemWidth))
    }
}

@Composable
private fun PlaylistPlanet(coverUrl: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { AppTheme.playlistPlanet.roundToPx() }
    val fallback = remember {
        PlanetPalette(AppTheme.blueShadow, AppTheme.blueDeep, AppTheme.blue, AppTheme.mint)
    }
    val palette = produceState(fallback, coverUrl, pixels) {
        if (coverUrl.isNullOrBlank()) return@produceState
        val result = context.imageLoader.execute(ImageRequest.Builder(context)
            .data(coverUrl).size(pixels).allowHardware(false).build()) as? SuccessResult
            ?: return@produceState
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val colors = Palette.from(bitmap).maximumColorCount(10).generate()
            val dominant = Color(colors.dominantSwatch?.rgb ?: AppTheme.blue.value.toLong().toInt())
            val vibrant = Color(colors.vibrantSwatch?.rgb ?: colors.mutedSwatch?.rgb
                ?: AppTheme.mint.value.toLong().toInt())
            val light = Color(colors.lightVibrantSwatch?.rgb ?: colors.lightMutedSwatch?.rgb
                ?: vibrant.value.toLong().toInt())
            PlanetPalette(
                lerp(AppTheme.background, dominant, .58f),
                lerp(AppTheme.background, dominant, .82f),
                lerp(dominant, light, .62f),
                lerp(vibrant, AppTheme.foreground, .18f),
            )
        }
    }
    Box(modifier.drawWithCache {
        val bitmapSize = size.minDimension.toInt().coerceAtLeast(1)
        val bitmap = ImageBitmap(bitmapSize, bitmapSize)
        val cell = AppTheme.playlistPlanetCell.toPx().coerceAtLeast(1f)
        val columns = (bitmapSize / cell).toInt().coerceAtLeast(1)
        val radius = bitmapSize * .46f
        val center = bitmapSize / 2f
        val bandColors = arrayOf(palette.value.dark, palette.value.mid, palette.value.light,
            palette.value.accent, palette.value.mid, palette.value.light)
        CanvasDrawScope().draw(this, layoutDirection, BitmapCanvas(bitmap),
            Size(bitmapSize.toFloat(), bitmapSize.toFloat())) {
            var row = 0
            while (row <= columns) {
                var column = 0
                while (column <= columns) {
                    val cx = column * cell + cell / 2f
                    val cy = row * cell + cell / 2f
                    val dx = cx - center
                    val dy = cy - center
                    val rough = if ((row * 17 + column * 31) % 11 == 0) cell * .65f else 0f
                    if (dx * dx + dy * dy <= (radius - rough) * (radius - rough)) {
                        val wave = if (column % 5 < 2) 1 else 0
                        val band = ((row + wave) / 2).mod(bandColors.size)
                        val noisy = (row * 23 + column * 47) % 13 == 0
                        drawRect(if (noisy) lerp(bandColors[band], AppTheme.background, .22f)
                            else bandColors[band], Offset(column * cell, row * cell), Size(cell, cell))
                    }
                    column++
                }
                row++
            }
        }
        onDrawBehind { drawImage(bitmap) }
    })
}
