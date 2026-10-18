package com.universe.remote.ui.home

import android.content.ActivityNotFoundException
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.universe.remote.ui.components.motionEnabled
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.ui.components.firstAppearance
import coil.compose.AsyncImage
import com.universe.remote.ui.components.coverRequest
import com.universe.remote.domain.WebPlaylist
import com.universe.remote.domain.WebTrack
import com.universe.remote.domain.LocalTrack
import com.universe.remote.ui.components.GrainBackground
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.Pill
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.components.liquidGlass
import com.universe.remote.ui.components.LocalLiquidGlassHaze
import com.universe.remote.ui.theme.AppTheme
import com.universe.remote.ui.nowplaying.NowPlayingScreen
import com.universe.remote.ui.nowplaying.NowPlayingViewModel
import com.universe.remote.ui.mood.MoodViewModel
import com.universe.remote.ui.mood.PlayHistoryScreen
import com.universe.remote.ui.mood.TagDialog
import com.universe.remote.ui.search.SearchViewModel
import com.universe.remote.ui.search.SearchScreen
import com.universe.remote.ui.search.SearchGlyph
import com.universe.remote.ui.library.LibraryViewModel
import com.universe.remote.ui.library.LibraryScreen
import com.universe.remote.ui.stats.StatsViewModel
import com.universe.remote.ui.stats.StatsScreen
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun HomeScreen(viewModel: HomeViewModel, nowPlaying: NowPlayingViewModel, mood: MoodViewModel,
    search: SearchViewModel, library: LibraryViewModel, stats: StatsViewModel,
    openRemote: () -> Unit, logout: () -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val songs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
    val hasTrack by nowPlaying.hasTrack.collectAsStateWithLifecycle()
    val motion = motionEnabled()
    var tab by remember { mutableStateOf(HomeTab.HOME) }
    var notice by remember { mutableStateOf<String?>(null) }
    var avatarMenu by remember { mutableStateOf(false) }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    val homeList = rememberLazyListState()
    val statsList = rememberLazyListState()
    val libraryList = rememberLazyListState()
    val scrolling by remember(tab) { derivedStateOf {
        when (tab) {
            HomeTab.HOME -> homeList.isScrollInProgress
            HomeTab.STATS -> statsList.isScrollInProgress
            HomeTab.LIBRARY -> libraryList.isScrollInProgress
        }
    } }
    var searchFromBottom by remember { mutableStateOf(true) }
    fun openSearch(fromBottom: Boolean) {
        searchFromBottom = fromBottom
        searchOpen = true
    }
    val searchPivot = TransformOrigin(.88f, if (searchFromBottom) .94f else .10f)
    val searchDirection = if (searchFromBottom) 1 else -1
    val glassState = rememberHazeState(blurEnabled = true)

    CompositionLocalProvider(LocalLiquidGlassHaze provides glassState) {
    SharedTransitionLayout(Modifier.fillMaxSize()) {
    GrainBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(glassState, zIndex = 0f)) {
        if (tab == HomeTab.HOME) {
            LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), state = homeList,
                contentPadding = PaddingValues(bottom = if (hasTrack)
                    AppTheme.dockBottomPaddingWithPlayer else AppTheme.dockBottomPaddingWithoutPlayer),
                verticalArrangement = Arrangement.spacedBy(0.dp)) {
                item { HomeTopBar(onNotification = { notice = "No notifications yet." },
                    onAvatar = { avatarMenu = !avatarMenu }, avatarMenu = avatarMenu,
                    closeAvatar = { avatarMenu = false }, openRemote = openRemote, logout = logout) }
                item { HomeHero(nowPlaying) }
                item {
                    PlaylistSection(playlists, selectedPlaylistId,
                        onPlay = { playlist, shuffle ->
                            selectedPlaylistId = playlist.id
                            viewModel.playPlaylist(playlist, shuffle)
                        }, retry = viewModel::refreshPlaylists)
                }
                item {
                    if (playbackError != null) Label(playbackError!!, AppTheme.caption.copy(color = AppTheme.error),
                        Modifier.padding(horizontal = AppTheme.page, vertical = AppTheme.small))
                    if (notice != null) Label(notice!!, AppTheme.caption,
                        Modifier.padding(horizontal = AppTheme.page, vertical = AppTheme.small))
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(start = AppTheme.page, end = AppTheme.page,
                        top = AppTheme.large, bottom = AppTheme.small),
                        verticalAlignment = Alignment.CenterVertically) {
                        Label("Liked songs", AppTheme.eyebrow, Modifier.weight(1f))
                        Box(Modifier.size(AppTheme.touch).clickable(role = Role.Button,
                            onClickLabel = "Search Spotify") { openSearch(false) },
                            contentAlignment = Alignment.Center) { SearchGlyph() }
                    }
                }
                when (val value = songs) {
                    Section.Loading -> item { StateMessage("Loading your liked songs…") }
                    is Section.Failed -> item { StateMessage(value.message, viewModel::refreshLikedSongs) }
                    is Section.Ready -> if (value.value.isEmpty()) item { StateMessage("No liked songs yet.") }
                        else itemsIndexed(value.value, key = { index, track -> track.id ?: track.uri ?: "unknown:$index" }) { _, track ->
                            LikedTrackRow(track, mood, onPlay = { viewModel.playTrack(track) })
                        }
                }
            }
        } else if (tab == HomeTab.STATS) {
            StatsScreen(stats, mood, listState = statsList, dockHasPlayer = hasTrack)
        } else if (tab == HomeTab.LIBRARY) {
            LibraryScreen(playlists, songs, library, mood, listState = libraryList,
                dockHasPlayer = hasTrack,
                retryPlaylists = viewModel::refreshPlaylists, retrySongs = viewModel::refreshLikedSongs,
                onSearch = { openSearch(false) })
        }
        }
        AnimatedVisibility(visible = !scrolling && !nowPlayingOpen,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(horizontal = AppTheme.page, vertical = AppTheme.dockBottom),
            enter = if (motion) slideInVertically(tween(AppTheme.dockSlideMs)) { it } + fadeIn()
                else EnterTransition.None,
            exit = if (motion) slideOutVertically(tween(AppTheme.dockSlideMs)) { it } + fadeOut()
                else ExitTransition.None) {
            val base = Modifier.fillMaxWidth()
            val dockModifier = if (hasTrack) base.sharedBounds(
                rememberSharedContentState("player-container"), this,
                boundsTransform = { _, _ -> tween(if (motion) AppTheme.playerExpandMs else 0,
                    easing = FastOutSlowInEasing) },
                enter = fadeIn(tween(if (motion) AppTheme.playerFadeMs else 0)),
                exit = fadeOut(tween(if (motion) AppTheme.playerFadeMs else 0)),
                clipInOverlayDuringTransition = OverlayClip(AppTheme.dockShape)) else base
            PlayerDock(tab, hasTrack, nowPlaying, onTab = { tab = it },
                onSearch = { openSearch(true) }, searchActive = searchOpen,
                onOpenPlayer = { nowPlayingOpen = true },
                modifier = dockModifier)
        }
        HomeGrainOverlay(Modifier.matchParentSize())
        AnimatedVisibility(nowPlayingOpen, modifier = Modifier.fillMaxSize(),
            enter = EnterTransition.None, exit = ExitTransition.None) {
            NowPlayingScreen(nowPlaying, onClose = { nowPlayingOpen = false },
                modifier = Modifier.sharedBounds(
                    rememberSharedContentState("player-container"), this,
                    boundsTransform = { _, _ -> tween(if (motion) AppTheme.playerExpandMs else 0, easing = FastOutSlowInEasing) },
                    enter = fadeIn(tween(if (motion) AppTheme.playerFadeMs else 0)),
                    exit = fadeOut(tween(if (motion) AppTheme.playerFadeMs else 0)),
                    clipInOverlayDuringTransition = OverlayClip(AppTheme.card)))
        }
        AnimatedVisibility(searchOpen, modifier = Modifier.fillMaxSize(),
            enter = if (motion) fadeIn(tween(AppTheme.searchOpenMs)) +
                scaleIn(tween(AppTheme.searchOpenMs, easing = FastOutSlowInEasing),
                    initialScale = .92f, transformOrigin = searchPivot) +
                slideInVertically(tween(AppTheme.searchOpenMs, easing = FastOutSlowInEasing)) {
                    it / 10 * searchDirection
                }
                else EnterTransition.None,
            exit = if (motion) fadeOut(tween(AppTheme.searchCloseMs)) +
                scaleOut(tween(AppTheme.searchCloseMs), targetScale = .94f,
                    transformOrigin = searchPivot) +
                slideOutVertically(tween(AppTheme.searchCloseMs)) { it / 10 * searchDirection }
                else ExitTransition.None) {
            SearchScreen(search, mood, onClose = { searchOpen = false })
        }
    }
    }
    }
}

@Composable
private fun HomeTopBar(onNotification: () -> Unit, onAvatar: () -> Unit, avatarMenu: Boolean,
    closeAvatar: () -> Unit, openRemote: () -> Unit, logout: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page, vertical = AppTheme.large),
        verticalAlignment = Alignment.CenterVertically) {
        PixelTitle(Modifier.weight(1f).padding(end = AppTheme.titleBellGap))
        RoundIcon("Notifications", onNotification) { BellIcon() }
        Spacer(Modifier.width(AppTheme.small))
        Box {
            Box(Modifier.size(AppTheme.avatar).clip(CircleShape).background(AppTheme.lime)
                .clickable(role = Role.Button, onClickLabel = "Account") { onAvatar() }, contentAlignment = Alignment.Center) {
                UniverseIcon(UniverseGlyph.PROFILE, color = AppTheme.ink)
            }
            if (avatarMenu) Popup(alignment = Alignment.TopEnd, onDismissRequest = closeAvatar) {
                Column(Modifier.width(AppTheme.menuWidth).liquidGlass(AppTheme.card)
                    .padding(AppTheme.small)) {
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
    Box(Modifier.size(AppTheme.iconButton).liquidGlass(CircleShape)
        .clickable(role = Role.Button, onClickLabel = description, onClick = action), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun HomeHero(player: NowPlayingViewModel) {
    Box(Modifier.fillMaxWidth().height(AppTheme.heroHeight), contentAlignment = Alignment.Center) {
        PixelPlanet(Modifier.fillMaxSize(), player)
    }
}

@Composable
private fun PlaylistSection(playlists: Section<List<WebPlaylist>>, selectedId: String?,
    onPlay: (WebPlaylist, Boolean) -> Unit, retry: () -> Unit) {
    var menu by remember { mutableStateOf<WebPlaylist?>(null) }
    Column(Modifier.fillMaxWidth()) {
        Label("Playlists", AppTheme.eyebrow,
            Modifier.padding(horizontal = AppTheme.page, vertical = AppTheme.small))
        when (playlists) {
            Section.Loading -> StateMessage("Loading your playlists…")
            is Section.Failed -> StateMessage(playlists.message, retry)
            is Section.Ready -> if (playlists.value.isEmpty()) StateMessage("No playlists yet.")
            else LazyRow(Modifier.fillMaxWidth().height(AppTheme.playlistItemHeight),
                contentPadding = PaddingValues(horizontal = AppTheme.page),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                itemsIndexed(playlists.value, key = { _, playlist -> playlist.id }) { _, playlist ->
                    PlaylistPlanetItem(playlist, selected = playlist.id == selectedId,
                        onPlay = { onPlay(playlist, false) }, onLongPress = { menu = playlist })
                }
            }
        }
    }
    menu?.let { playlist ->
        PlaylistActionsSheet(playlist, onDismiss = { menu = null },
            onShuffle = { menu = null; onPlay(playlist, true) })
    }
}

@Composable
private fun PlaylistActionsSheet(playlist: WebPlaylist, onDismiss: () -> Unit,
    onShuffle: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().navigationBarsPadding().padding(AppTheme.page),
            contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().liquidGlass(AppTheme.dockShape)
                .padding(AppTheme.large), verticalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                BasicText(playlist.name, style = AppTheme.headline, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                MenuAction("Shuffle play") { onShuffle() }
                MenuAction("Open in Spotify") {
                    onDismiss()
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://open.spotify.com/playlist/${playlist.id}")))
                    } catch (_: ActivityNotFoundException) { }
                }
            }
        }
    }
}

@Composable
private fun LikedTrackRow(track: WebTrack, mood: MoodViewModel, onPlay: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var tagSheet by remember { mutableStateOf(false) }
    val id = track.id ?: track.uri?.takeIf { it.startsWith("spotify:track:") }?.substringAfterLast(':')
    val artist = remember(track.artists) { track.artists.joinToString(", ") }
    val localTrack = remember(track) { id?.let { LocalTrack(it, track.name, artist,
        "", track.coverUrl, 0) } }
    Row(Modifier.fillMaxWidth().firstAppearance(track.uri ?: track.name).heightIn(min = AppTheme.rowMinHeight)
        .combinedClickable(onClick = onPlay, onLongClick = { if (localTrack != null) tagSheet = true })
        .padding(horizontal = AppTheme.page, vertical = AppTheme.small),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
        Box(Modifier.size(AppTheme.rowCover).clip(AppTheme.artShape).background(AppTheme.surface),
            contentAlignment = Alignment.Center) {
            if (track.coverUrl == null) UniverseIcon(UniverseGlyph.MUSIC)
            else AsyncImage(model = coverRequest(track.coverUrl, AppTheme.rowCover), contentDescription = "Cover for ${track.name}",
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
            BasicText(track.name, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(artist.ifBlank { "Unknown artist" }, style = AppTheme.caption,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            Box(Modifier.size(AppTheme.rowAction).clip(CircleShape).clickable(role = Role.Button,
                onClickLabel = "More options for ${track.name}") { menu = true }, contentAlignment = Alignment.Center) {
                UniverseIcon(UniverseGlyph.MORE)
            }
            if (menu) Popup(alignment = Alignment.TopEnd, onDismissRequest = { menu = false }) {
                Column(Modifier.width(AppTheme.menuWidth).liquidGlass(AppTheme.card)
                    .padding(AppTheme.small)) {
                    MenuAction("Play") { menu = false; onPlay() }
                    MenuAction("Mood tags") { menu = false; if (localTrack != null) tagSheet = true }
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
    if (tagSheet && localTrack != null) TagDialog(localTrack, mood, onDismiss = { tagSheet = false })
}

@Composable
private fun StateMessage(message: String, retry: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page, vertical = AppTheme.medium),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        Label(message, AppTheme.caption, Modifier.weight(1f))
        if (retry != null) Pill("Retry", onClick = retry)
    }
}

