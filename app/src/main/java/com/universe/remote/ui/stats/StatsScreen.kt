package com.universe.remote.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.domain.TimeBucket
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.components.liquidGlass
import com.universe.remote.ui.mood.MoodViewModel
import com.universe.remote.ui.mood.PlayHistoryScreen
import com.universe.remote.ui.theme.AppTheme

@Composable
fun StatsScreen(model: StatsViewModel, mood: MoodViewModel, listState: LazyListState,
    dockHasPlayer: Boolean) {
    val range by model.range.collectAsStateWithLifecycle()
    val stats by model.snapshot.collectAsStateWithLifecycle()
    val totals by model.totals.collectAsStateWithLifecycle()
    var debug by remember { mutableStateOf(false) }
    BackHandler(enabled = debug) { debug = false }
    if (debug) {
        Box(Modifier.fillMaxSize()) {
            PlayHistoryScreen(mood)
            Box(Modifier.align(Alignment.TopEnd).safeDrawingPadding()
                .padding(AppTheme.page).clickable(role = Role.Button, onClickLabel = "Close play history") {
                    debug = false
                }, contentAlignment = Alignment.Center) { UniverseIcon(UniverseGlyph.CLOSE) }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), state = listState,
        contentPadding = PaddingValues(start = AppTheme.page, end = AppTheme.page,
            top = AppTheme.large, bottom = if (dockHasPlayer)
                AppTheme.dockBottomPaddingWithPlayer else AppTheme.dockBottomPaddingWithoutPlayer),
        verticalArrangement = Arrangement.spacedBy(AppTheme.large)) {
        item {
            Column(Modifier.combinedClickable(onClick = {}, onLongClick = { debug = true })) {
                Label("UNIVERSE / STATS", AppTheme.eyebrow)
                Label("Your listening", AppTheme.headline)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                StatsRange.entries.forEach { option ->
                    Label(option.label, AppTheme.caption.copy(
                        color = if (option == range) AppTheme.ink else AppTheme.foreground),
                        Modifier.weight(1f).liquidGlass(AppTheme.pill,
                            if (option == range) AppTheme.lime else AppTheme.liquidGlass)
                            .clickable(role = Role.Button) { model.select(option) }
                            .padding(horizontal = AppTheme.small, vertical = AppTheme.medium))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                listOf("7 DAYS", "30 DAYS", "ALL TIME").forEachIndexed { index, label ->
                    Column(Modifier.weight(1f).heightIn(min = AppTheme.statCardHeight)
                        .liquidGlass(AppTheme.card)
                        .padding(AppTheme.small)) {
                        Label(label, AppTheme.time)
                        Label(duration(totals.getOrElse(index) { 0 }), AppTheme.body)
                    }
                }
            }
        }
        if (stats.tracks.isEmpty()) {
            item { Label("No listening logs in this range yet. Play music with Universe open to fill this universe.",
                AppTheme.caption) }
        } else {
            item { Label("BY HOUR", AppTheme.eyebrow) }
            item { PixelBarChart(stats.hours, 24, Modifier.fillMaxWidth().height(AppTheme.chartHeight)) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("00", "06", "12", "18", "24").forEach { Label(it, AppTheme.time) }
                }
            }
            item { Label("BY DAY", AppTheme.eyebrow) }
            item { PixelBarChart(stats.days, 7, Modifier.fillMaxWidth().height(AppTheme.chartShortHeight)) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach {
                        Label(it, AppTheme.time)
                    }
                }
            }
            item { Label("TOP TRACKS", AppTheme.eyebrow) }
            itemsIndexed(stats.tracks, key = { _, track -> "track:${track.trackId}" }) { index, track ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                    Label((index + 1).toString().padStart(2, '0'), AppTheme.eyebrow)
                    Column(Modifier.weight(1f)) {
                        Label(track.title, AppTheme.body)
                        Label(track.artist + " · " + duration(track.listenedMs), AppTheme.caption)
                    }
                }
            }
            item { Label("TOP ARTISTS", AppTheme.eyebrow) }
            itemsIndexed(stats.artists, key = { _, artist -> "artist:${artist.artist}" }) { index, artist ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                    Label((index + 1).toString().padStart(2, '0'), AppTheme.eyebrow)
                    Label(artist.artist + " · " + duration(artist.listenedMs), AppTheme.body)
                }
            }
            item { Label("MOOD BREAKDOWN", AppTheme.eyebrow) }
            if (stats.moods.isEmpty()) item { Label("Tag tracks to see your moods here.", AppTheme.caption) }
            itemsIndexed(stats.moods, key = { _, mood -> "mood:${mood.name}" }) { _, moodStat ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Label(moodStat.name, AppTheme.body)
                    Label(duration(moodStat.listenedMs), AppTheme.caption)
                }
            }
        }
    }
}

@Composable
private fun PixelBarChart(buckets: List<TimeBucket>, count: Int, modifier: Modifier = Modifier) {
    val values = remember(buckets, count) {
        LongArray(count).also { array ->
            buckets.forEach { if (it.bucket in 0 until count) array[it.bucket] = it.listenedMs }
        }
    }
    Box(modifier.liquidGlass(AppTheme.card).padding(AppTheme.medium)
        .drawWithCache {
            val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
            val gap = AppTheme.tiny.toPx()
            val barWidth = (size.width / count - gap).coerceAtLeast(1f)
            val bars = values.mapIndexed { index, value ->
                val height = (size.height * value / max).coerceAtLeast(if (value > 0) 2f else 0f)
                Offset(index * size.width / count, size.height - height) to Size(barWidth, height)
            }
            val baseline = Offset(0f, size.height) to Offset(size.width, size.height)
            onDrawBehind {
                drawLine(AppTheme.border, baseline.first, baseline.second, AppTheme.hairline.toPx())
                var index = 0
                while (index < bars.size) {
                    val (top, dimensions) = bars[index++]
                    if (dimensions.height > 0) drawRect(AppTheme.lime, top, dimensions)
                }
            }
        })
}

private fun duration(ms: Long): String {
    if (ms < 60_000) return (ms.coerceAtLeast(0) / 1_000).toString() + "s"
    val minutes = (ms / 60_000).coerceAtLeast(0)
    return if (minutes >= 60) (minutes / 60).toString() + "h " + (minutes % 60) + "m"
        else minutes.toString() + "m"
}

