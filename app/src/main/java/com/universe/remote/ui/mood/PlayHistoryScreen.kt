package com.universe.remote.ui.mood

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.theme.AppTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun PlayHistoryScreen(model: MoodViewModel, modifier: Modifier = Modifier) {
    val plays by model.plays.collectAsStateWithLifecycle()
    val counts by model.counts.collectAsStateWithLifecycle()
    LazyColumn(modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(start = AppTheme.page, end = AppTheme.page,
            top = AppTheme.large, bottom = AppTheme.navInset + AppTheme.miniHeight + AppTheme.miniGap),
        verticalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
        item { Label("PLAY HISTORY", AppTheme.eyebrow) }
        item { Label("Latest 50 plays", AppTheme.headline) }
        if (plays.isEmpty()) item { Label("No plays logged yet. Play a track while Universe is open.", AppTheme.caption) }
        items(plays, key = { it.id }) { play ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
                Label(play.title + " · " + play.artist, AppTheme.body)
                val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(play.startedAt))
                val listened = play.listenedMs / 1_000
                val status = when {
                    play.completed -> "completed"
                    play.skipped -> "skipped"
                    else -> "partial"
                }
                Label("$time · ${listened / 60}m ${listened % 60}s · $status · ${play.source}",
                    AppTheme.caption)
            }
        }
        item { Spacer(Modifier.height(AppTheme.medium)); Label("TAG COUNTS", AppTheme.eyebrow) }
        if (counts.isEmpty()) item { Label("No tags yet.", AppTheme.caption) }
        items(counts, key = { it.name }) { tag ->
            Label(tag.name + ": " + tag.count, AppTheme.body)
        }
    }
}
