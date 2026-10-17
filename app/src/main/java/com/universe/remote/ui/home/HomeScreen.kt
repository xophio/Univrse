package com.universe.remote.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.universe.remote.domain.WebPlaylist
import com.universe.remote.domain.WebTrack
import com.universe.remote.ui.components.GrainBackground
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.Pill
import com.universe.remote.ui.theme.AppTheme
import androidx.compose.ui.window.Popup

private enum class Tab(val title: String) {
    HOME("Home"), STATS("Stats"), LIBRARY("Library")
}

@Composable
fun HomeScreen(viewModel: HomeViewModel, openRemote: () -> Unit, logout: () -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val songs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var notice by remember { mutableStateOf<String?>(null) }
    var avatarMenu by remember { mutableStateOf(false) }

    GrainBackground(Modifier.fillMaxSize()) {
        if (tab == Tab.HOME) {
            LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(),
                contentPadding = PaddingValues(bottom = AppTheme.navInset),
                verticalArrangement = Arrangement.spacedBy(0.dp)) {
                item { HomeTopBar(onNotification = { notice = "No notifications yet." },
                    onAvatar = { avatarMenu = !avatarMenu }, avatarMenu = avatarMenu,
                    closeAvatar = { avatarMenu = false }, openRemote = openRemote, logout = logout) }
                item { HeroCarousel(playlists, viewModel::playPlaylist, viewModel::refreshPlaylists) }
                item {
                    if (playbackError != null) Label(playbackError!!, AppTheme.caption.copy(color = AppTheme.error),
                        Modifier.padding(horizontal = AppTheme.page, vertical = AppTheme.small))
                    if (notice != null) Label(notice!!, AppTheme.caption,
                        Modifier.padding(horizontal = AppTheme.page, vertical = AppTheme.small))
                }
                item { Label("Liked songs", AppTheme.eyebrow,
                    Modifier.padding(start = AppTheme.page, top = AppTheme.large, bottom = AppTheme.small)) }
                when (val value = songs) {
                    Section.Loading -> item { StateMessage("Loading your liked songs…") }
                    is Section.Failed -> item { StateMessage(value.message, viewModel::refreshLikedSongs) }
                    is Section.Ready -> if (value.value.isEmpty()) item { StateMessage("No liked songs yet.") }
                        else itemsIndexed(value.value, key = { index, track -> "${track.id ?: "unknown"}:$index" }) { _, track ->
                            LikedTrackRow(track, onPlay = { viewModel.playTrack(track) })
                        }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(AppTheme.page),
                verticalArrangement = Arrangement.spacedBy(AppTheme.large)) {
                PixelTitle(Modifier.width(AppTheme.titleWidth))
                Spacer(Modifier.height(AppTheme.section))
                Label(tab.title, AppTheme.headline)
                Label("This screen arrives in Phase 5.", AppTheme.caption)
            }
        }
        FloatingNav(tab, onTab = { tab = it }, Modifier.align(Alignment.BottomCenter).safeDrawingPadding()
            .padding(bottom = AppTheme.navBottom))
        HomeGrainOverlay(Modifier.matchParentSize())
    }
}

@Composable
private fun HomeTopBar(onNotification: () -> Unit, onAvatar: () -> Unit, avatarMenu: Boolean,
    closeAvatar: () -> Unit, openRemote: () -> Unit, logout: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page, vertical = AppTheme.large),
        verticalAlignment = Alignment.CenterVertically) {
        PixelTitle(Modifier.weight(1f))
        RoundIcon("Notifications", onNotification) { BellIcon() }
        Spacer(Modifier.width(AppTheme.small))
        Box {
            Box(Modifier.size(AppTheme.avatar).clip(CircleShape).background(AppTheme.lime)
                .clickable(role = Role.Button, onClickLabel = "Account") { onAvatar() }, contentAlignment = Alignment.Center) {
                Label("U", AppTheme.eyebrow.copy(color = AppTheme.ink))
            }
            if (avatarMenu) Popup(alignment = Alignment.TopEnd, onDismissRequest = closeAvatar) {
                Column(Modifier.width(AppTheme.menuWidth).clip(AppTheme.card).background(AppTheme.surface)
                    .border(AppTheme.hairline, AppTheme.border, AppTheme.card).padding(AppTheme.small)) {
                    MenuAction("Playback controls") { closeAvatar(); openRemote() }
                    MenuAction("Log out") { closeAvatar(); logout() }
                }
            }
        }
    }
}

@Composable
private fun MenuAction(text: String, action: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = AppTheme.touch).clip(AppTheme.artShape)
        .clickable(role = Role.Button, onClick = action).padding(horizontal = AppTheme.medium), contentAlignment = Alignment.CenterStart) {
        Label(text, AppTheme.body)
    }
}

@Composable
private fun RoundIcon(description: String, action: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(AppTheme.iconButton).clip(CircleShape).background(AppTheme.surface)
        .border(AppTheme.hairline, AppTheme.border, CircleShape)
        .clickable(role = Role.Button, onClickLabel = description, onClick = action), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun HeroCarousel(playlists: Section<List<WebPlaylist>>, onPlay: (WebPlaylist, Boolean) -> Unit,
    retry: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(AppTheme.heroHeight)) {
        PixelPlanet(Modifier.fillMaxWidth().height(AppTheme.heroHeight - AppTheme.carouselHeight / 3))
        Box(Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
            when (playlists) {
                Section.Loading -> StateMessage("Loading your playlists…")
                is Section.Failed -> StateMessage(playlists.message, retry)
                is Section.Ready -> if (playlists.value.isEmpty()) StateMessage("No playlists yet.")
                    else BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val width = (maxWidth - AppTheme.page - AppTheme.carouselPeek).coerceAtMost(AppTheme.carouselWidth)
                        LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = AppTheme.page),
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                            itemsIndexed(playlists.value, key = { index, p -> "${p.id}:$index" }) { _, playlist ->
                                PlaylistCard(playlist, width, onPlay)
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: WebPlaylist, width: androidx.compose.ui.unit.Dp,
    onPlay: (WebPlaylist, Boolean) -> Unit) {
    Row(Modifier.width(width).height(AppTheme.carouselHeight).clip(AppTheme.card)
        .background(AppTheme.glass).border(AppTheme.hairline, AppTheme.glassBorder, AppTheme.card)
        .padding(horizontal = AppTheme.medium), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
            BasicText(playlist.name, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Label(playlist.trackCount?.let { "$it Tracks" } ?: "Playlist", AppTheme.caption)
        }
        Box(Modifier.height(AppTheme.iconButton).clip(AppTheme.pill).background(AppTheme.lime)
            .clickable(role = Role.Button, onClickLabel = "Play ${playlist.name}") { onPlay(playlist, false) }
            .padding(horizontal = AppTheme.medium), contentAlignment = Alignment.Center) {
            Label("▷ Play", AppTheme.body.copy(color = AppTheme.ink))
        }
        Box(Modifier.size(AppTheme.iconButton).clip(CircleShape).background(AppTheme.shuffleSurface)
            .clickable(role = Role.Button, onClickLabel = "Shuffle ${playlist.name}") { onPlay(playlist, true) },
            contentAlignment = Alignment.Center) { ShuffleIcon() }
    }
}

@Composable
private fun LikedTrackRow(track: WebTrack, onPlay: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = AppTheme.rowMinHeight).padding(horizontal = AppTheme.page, vertical = AppTheme.small),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
        Box(Modifier.size(AppTheme.rowCover).clip(AppTheme.artShape).background(AppTheme.surface),
            contentAlignment = Alignment.Center) {
            if (track.coverUrl == null) Label("♫", AppTheme.headline)
            else AsyncImage(model = track.coverUrl, contentDescription = "Cover for ${track.name}",
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
            BasicText(track.name, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(track.artists.joinToString(", ").ifBlank { "Unknown artist" }, style = AppTheme.caption,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            Box(Modifier.size(AppTheme.rowAction).clip(CircleShape).clickable(role = Role.Button,
                onClickLabel = "More options for ${track.name}") { menu = true }, contentAlignment = Alignment.Center) {
                Label("•••", AppTheme.body)
            }
            if (menu) Popup(alignment = Alignment.TopEnd, onDismissRequest = { menu = false }) {
                Column(Modifier.width(AppTheme.menuWidth).clip(AppTheme.card).background(AppTheme.surface)
                    .border(AppTheme.hairline, AppTheme.border, AppTheme.card).padding(AppTheme.small)) {
                    MenuAction("Play") { menu = false; onPlay() }
                    MenuAction("Open in Spotify") {
                        menu = false
                        track.uri?.takeIf { it.startsWith("spotify:track:") }?.substringAfterLast(':')?.let { id ->
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/track/$id"))) }
                            catch (_: ActivityNotFoundException) { }
                        }
                    }
                }
            }
        }
        HeartIcon()
    }
}

@Composable
private fun StateMessage(message: String, retry: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page, vertical = AppTheme.medium),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        Label(message, AppTheme.caption, Modifier.weight(1f))
        if (retry != null) Pill("Retry", onClick = retry)
    }
}

@Composable
private fun FloatingNav(selected: Tab, onTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(AppTheme.navShape).background(AppTheme.navSolid)
        .border(AppTheme.hairline, AppTheme.border, AppTheme.navShape).padding(AppTheme.tiny),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
        Tab.entries.forEach { tab ->
            Row(Modifier.height(AppTheme.navHeight - AppTheme.small).clip(AppTheme.pill)
                .background(if (tab == selected) AppTheme.navSelected else AppTheme.transparent)
                .clickable(role = Role.Tab, onClickLabel = tab.title) { onTab(tab) }
                .padding(end = if (tab == selected) AppTheme.medium else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                Box(Modifier.size(AppTheme.navHeight - AppTheme.small).clip(CircleShape)
                    .background(if (tab == selected) AppTheme.lime else AppTheme.surface), contentAlignment = Alignment.Center) {
                    when (tab) {
                        Tab.HOME -> Label("⌂", AppTheme.headline.copy(color = if (tab == selected) AppTheme.ink else AppTheme.foreground))
                        Tab.STATS -> RainbowIcon()
                        Tab.LIBRARY -> StackedCardsIcon()
                    }
                }
                if (tab == selected) Label(tab.title, AppTheme.body)
            }
        }
    }
}
