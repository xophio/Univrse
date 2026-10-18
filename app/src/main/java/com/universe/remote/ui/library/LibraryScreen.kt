package com.universe.remote.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.universe.remote.ui.components.coverRequest
import com.universe.remote.domain.WebPlaylist
import com.universe.remote.domain.WebTrack
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.home.Section
import com.universe.remote.ui.mood.MoodViewModel
import com.universe.remote.ui.search.CatalogTrackRow
import com.universe.remote.ui.search.SearchGlyph
import com.universe.remote.ui.theme.AppTheme

private enum class LibraryTab(val label: String) { PLAYLISTS("Playlists"), LIKED("Liked songs") }

@Composable
fun LibraryScreen(playlists: Section<List<WebPlaylist>>, songs: Section<List<WebTrack>>,
    model: LibraryViewModel, mood: MoodViewModel, listState: LazyListState,
    dockHasPlayer: Boolean,
    retryPlaylists: () -> Unit, retrySongs: () -> Unit, onSearch: () -> Unit) {
    var tab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    val sort by model.sort.collectAsStateWithLifecycle()
    val selectedTags by model.tagIds.collectAsStateWithLifecycle()
    val tags by model.tags.collectAsStateWithLifecycle()
    val selectedPlaylist by model.selectedPlaylist.collectAsStateWithLifecycle()
    val detail by model.visibleDetail.collectAsStateWithLifecycle()
    val liked by model.visibleSongs.collectAsStateWithLifecycle()
    val playbackError by model.playbackError.collectAsStateWithLifecycle()
    LaunchedEffect(songs) { if (songs is Section.Ready) model.setSongs(songs.value) }
    BackHandler(enabled = selectedPlaylist != null) { model.closePlaylist() }

    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), state = listState,
        contentPadding = PaddingValues(bottom = if (dockHasPlayer)
            AppTheme.dockBottomPaddingWithPlayer else AppTheme.dockBottomPaddingWithoutPlayer),
        verticalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page, vertical = AppTheme.large),
                verticalAlignment = Alignment.CenterVertically) {
                Label("LIBRARY", AppTheme.eyebrow, Modifier.weight(1f))
                Box(Modifier.size(AppTheme.touch).clickable(role = Role.Button,
                    onClickLabel = "Search Spotify", onClick = onSearch),
                    contentAlignment = Alignment.Center) { SearchGlyph() }
            }
        }
        if (selectedPlaylist == null) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                    LibraryTab.entries.forEach { option ->
                        Label(option.label, AppTheme.body.copy(
                            color = if (tab == option) AppTheme.ink else AppTheme.foreground),
                            Modifier.background(if (tab == option) AppTheme.lime else AppTheme.surface,
                                AppTheme.pill).clickable(role = Role.Tab) {
                                    tab = option
                                    if (option == LibraryTab.PLAYLISTS && sort !in listOf(
                                            LibrarySort.RECENT, LibrarySort.TITLE)) model.setSort(LibrarySort.RECENT)
                                }
                                .padding(horizontal = AppTheme.medium, vertical = AppTheme.small))
                    }
                }
            }
        } else {
            item {
                Label("‹  " + selectedPlaylist!!.name, AppTheme.headline,
                    Modifier.padding(horizontal = AppTheme.page).clickable(role = Role.Button,
                        onClickLabel = "Back to playlists") { model.closePlaylist() })
            }
            item {
                Row(Modifier.padding(horizontal = AppTheme.page),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                    ActionPill("Play", UniverseGlyph.PLAY) { model.playPlaylist(selectedPlaylist!!, false) }
                    ActionPill("Shuffle", UniverseGlyph.SHUFFLE) { model.playPlaylist(selectedPlaylist!!, true) }
                }
            }
        }
        if (playbackError != null) item {
            Label(playbackError!!, AppTheme.caption.copy(color = AppTheme.error),
                Modifier.padding(horizontal = AppTheme.page))
        }
        if (tab == LibraryTab.LIKED || selectedPlaylist != null) {
            item { SortChoices(sort, model, LibrarySort.entries) }
            item { MoodFilters(tags, selectedTags, model) }
        } else {
            item { SortChoices(sort, model, listOf(LibrarySort.RECENT, LibrarySort.TITLE)) }
        }
        if (selectedPlaylist != null) {
            when (val value = detail) {
                Section.Loading -> item { StateLine("Loading playlist tracks…") }
                is Section.Failed -> item { StateLine(value.message, model::retryDetail) }
                is Section.Ready -> if (value.value.isEmpty()) item { StateLine("No tracks match this filter.") }
                    else items(value.value, key = { it.uri ?: it.name }) { track ->
                        Box(Modifier.padding(horizontal = AppTheme.page)) {
                            CatalogTrackRow(track, mood,
                                onPlay = { model.playTrack(track, fromPlaylist = true) },
                                onQueue = { model.queue(track) })
                        }
                    }
            }
        } else if (tab == LibraryTab.PLAYLISTS) {
            when (playlists) {
                Section.Loading -> item { StateLine("Loading playlists…") }
                is Section.Failed -> item { StateLine(playlists.message, retryPlaylists) }
                is Section.Ready -> {
                    val rows = if (sort == LibrarySort.TITLE) playlists.value.sortedBy { it.name.lowercase() }
                        else playlists.value
                    if (rows.isEmpty()) item { StateLine("No playlists yet.") }
                    else items(rows, key = { it.id }) { playlist ->
                        PlaylistRow(playlist) { model.openPlaylist(playlist) }
                    }
                }
            }
        } else {
            when (songs) {
                Section.Loading -> item { StateLine("Loading liked songs…") }
                is Section.Failed -> item { StateLine(songs.message, retrySongs) }
                is Section.Ready -> if (liked.isEmpty()) item {
                    StateLine(if (selectedTags.isEmpty()) "No liked songs yet." else "No songs match these moods.")
                } else items(liked, key = { it.uri ?: it.name }) { track ->
                    Box(Modifier.padding(horizontal = AppTheme.page)) {
                        CatalogTrackRow(track, mood,
                            onPlay = { model.playTrack(track, fromPlaylist = false) },
                            onQueue = { model.queue(track) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: WebPlaylist, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = AppTheme.cover)
        .clickable(role = Role.Button, onClickLabel = "Open " + playlist.name, onClick = onOpen)
        .padding(horizontal = AppTheme.page, vertical = AppTheme.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
        AsyncImage(coverRequest(playlist.coverUrl, AppTheme.rowCover), contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.size(AppTheme.rowCover).clip(AppTheme.artShape)
                .background(AppTheme.surface))
        Column(Modifier.weight(1f)) {
            Label(playlist.name, AppTheme.body)
            Label((playlist.trackCount ?: 0).toString() + " tracks", AppTheme.caption)
        }
        UniverseIcon(UniverseGlyph.CHEVRON_RIGHT)
    }
}

@Composable
private fun SortChoices(current: LibrarySort, model: LibraryViewModel, options: List<LibrarySort>) {
    LazyRow(contentPadding = PaddingValues(horizontal = AppTheme.page),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        items(options, key = { it.name }) { option ->
            Label(option.label, AppTheme.caption.copy(
                color = if (current == option) AppTheme.ink else AppTheme.foreground),
                Modifier.background(if (current == option) AppTheme.lime else AppTheme.surface,
                    AppTheme.pill).clickable(role = Role.Button) { model.setSort(option) }
                    .padding(horizontal = AppTheme.medium, vertical = AppTheme.small))
        }
    }
}

@Composable
private fun MoodFilters(tags: List<com.universe.remote.domain.LocalTag>,
    selected: Set<Long>, model: LibraryViewModel) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.small),
        verticalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        tags.forEach { tag ->
            val active = tag.id in selected
            Label(tag.name, AppTheme.caption.copy(color = if (active) AppTheme.ink else AppTheme.foreground),
                Modifier.background(if (active) Color(tag.color.toULong()) else AppTheme.surface,
                    AppTheme.pill).clickable(role = Role.Button) { model.toggleTag(tag.id) }
                    .padding(horizontal = AppTheme.medium, vertical = AppTheme.small))
        }
        if (selected.isNotEmpty()) Label("Clear", AppTheme.caption,
            Modifier.clickable(role = Role.Button, onClick = model::clearTags)
                .padding(AppTheme.small))
    }
}

@Composable
private fun ActionPill(text: String, glyph: UniverseGlyph, action: () -> Unit) {
    Row(Modifier.background(AppTheme.lime, AppTheme.pill)
        .clickable(role = Role.Button, onClick = action)
        .padding(horizontal = AppTheme.medium, vertical = AppTheme.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
        UniverseIcon(glyph, size = AppTheme.navGlyph, color = AppTheme.ink)
        Label(text, AppTheme.body.copy(color = AppTheme.ink))
    }
}

@Composable
private fun StateLine(message: String, retry: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(AppTheme.page),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Label(message, AppTheme.caption, Modifier.weight(1f))
        if (retry != null) Label("RETRY", AppTheme.eyebrow,
            Modifier.clickable(role = Role.Button, onClick = retry))
    }
}


