package com.universe.remote.ui.connection

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.domain.ConnectionStatus
import com.universe.remote.ui.components.*
import com.universe.remote.ui.theme.AppTheme

@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel) {
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val player by viewModel.playback.collectAsStateWithLifecycle()
    val onDemand by viewModel.onDemand.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val artwork by viewModel.artwork.collectAsStateWithLifecycle()
    val bitmap = remember(artwork) { artwork?.asImageBitmap() }
    val connected = connection.status == ConnectionStatus.CONNECTED
    val context = LocalContext.current
    var linkError by remember { mutableStateOf<String?>(null) }
    fun openSpotify(install: Boolean = false) {
        val url = if (install) "https://play.google.com/store/apps/details?id=com.spotify.music"
            else player.uri.takeIf { it.startsWith("spotify:track:") }
                ?.let { "https://open.spotify.com/track/${it.substringAfterLast(':')}" }
                ?: "https://open.spotify.com/"
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); linkError = null }
        catch (_: ActivityNotFoundException) { linkError = "No app is available to open this link." }
    }
    GrainBackground(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = AppTheme.maxWidth)
            .fillMaxWidth().safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(AppTheme.page), verticalArrangement = Arrangement.spacedBy(AppTheme.large)) {
            Label("Universe", AppTheme.universeTitle)
            Label("YOUR MUSIC. YOUR ORBIT.", AppTheme.eyebrow)
            Spacer(Modifier.height(AppTheme.small))
            Label("Welcome to your\nlistening universe.", AppTheme.headline)
            Label("Phase 01 / Spotify connection", AppTheme.caption)
            Column(Modifier.fillMaxWidth().clip(AppTheme.card).background(AppTheme.surface)
                .border(AppTheme.hairline, AppTheme.border, AppTheme.card).padding(AppTheme.large),
                verticalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                Label(if (connected) "●  CONNECTED" else "○  ${connection.status.name.replace('_', ' ')}", AppTheme.eyebrow)
                Label(connection.message)
                if (connected) Label(when (onDemand) {
                    true -> "On-demand playback is available."
                    false -> "This account cannot play individual tracks on demand. Use Spotify Premium to test this track."
                    null -> "Checking account playback capabilities…"
                }, AppTheme.caption)
                if (!connected) Pill("${if (connection.status == ConnectionStatus.CONNECTING) "Connecting…" else "Retry connection"}",
                    enabled = connection.status != ConnectionStatus.CONNECTING, onClick = viewModel::retry)
                if (connection.status == ConnectionStatus.NOT_INSTALLED) Pill("Install Spotify", onClick = { openSpotify(true) })
            }
            Pill("Play test track  ↗", Modifier.fillMaxWidth(), primary = true,
                enabled = connected && onDemand == true, onClick = viewModel::playTestTrack)
            Label("Plays a fixed Spotify track. Audio stays in the Spotify app.", AppTheme.caption)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                Box(Modifier.size(AppTheme.cover).clip(AppTheme.artShape).background(AppTheme.surface), contentAlignment = Alignment.Center) {
                    if (bitmap != null) Image(bitmap, "Album artwork", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    else UniverseIcon(UniverseGlyph.MUSIC)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
                    Label(player.title)
                    Label(player.artist, AppTheme.caption)
                }
            }
            Label(if (!connected) "Player state unavailable" else if (player.paused) "PAUSED" else "PLAYING", AppTheme.eyebrow)
            Label("${formatTime(player.positionMs)} / ${formatTime(player.durationMs)} · Position at last Spotify update", AppTheme.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                Pill("Previous", Modifier.weight(1f), enabled = connected && player.canPrevious, onClick = viewModel::previous)
                Pill(if (player.paused) "Resume" else "Pause", Modifier.weight(1f), primary = true,
                    enabled = connected && player.uri.isNotBlank(), onClick = viewModel::togglePlayback)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                Pill("Next", Modifier.weight(1f), enabled = connected && player.canNext, onClick = viewModel::next)
                Pill("Seek +15s", Modifier.weight(1f), enabled = connected && player.canSeek, onClick = viewModel::seekForward)
            }
            Pill("Shuffle: ${if (player.shuffle) "on" else "off"}", Modifier.fillMaxWidth(),
                enabled = connected && player.canShuffle, onClick = viewModel::toggleShuffle)
            Pill("Repeat: ${player.repeat.name.lowercase()}", Modifier.fillMaxWidth(),
                enabled = connected && (player.canRepeatTrack || player.canRepeatContext), onClick = viewModel::cycleRepeat)
            if (error != null) Label(error!!, AppTheme.body.copy(color = AppTheme.error))
            if (linkError != null) Label(linkError!!, AppTheme.body.copy(color = AppTheme.error))
            Pill("Open in Spotify ↗", Modifier.fillMaxWidth(), onClick = { openSpotify() })
            Label("Playback provided by Spotify", AppTheme.caption)
            Spacer(Modifier.height(AppTheme.large))
        }
    }
}

internal fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
