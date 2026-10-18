package com.universe.remote.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.ui.components.firstAppearance
import coil.compose.AsyncImage
import com.universe.remote.ui.components.coverRequest
import com.universe.remote.domain.LocalTrack
import com.universe.remote.domain.WebTrack
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.AnimatedPopup
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.components.liquidGlass
import com.universe.remote.ui.components.motionEnabled
import com.universe.remote.ui.home.HomeGrainOverlay
import com.universe.remote.ui.mood.MoodViewModel
import com.universe.remote.ui.mood.TagDialog
import com.universe.remote.ui.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(model: SearchViewModel, mood: MoodViewModel, onClose: () -> Unit) {
    val query by model.query.collectAsStateWithLifecycle()
    val results by model.results.collectAsStateWithLifecycle()
    val recent by model.recent.collectAsStateWithLifecycle()
    val playbackError by model.playbackError.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val motion = motionEnabled()
    val searchInteractions = remember { MutableInteractionSource() }
    val focused by searchInteractions.collectIsFocusedAsState()
    val focusBorder by animateColorAsState(if (focused) AppTheme.lime else AppTheme.border,
        tween(if (motion) AppTheme.searchFocusMs else 0), label = "search focus border")
    val iconScale by animateFloatAsState(if (focused) 1.12f else 1f,
        tween(if (motion) AppTheme.searchFocusMs else 0), label = "search focus icon")
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(AppTheme.background)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(
                horizontal = AppTheme.searchEdgePadding,
                vertical = AppTheme.page,
            ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
                Box(Modifier.size(AppTheme.touch)
                    .clickable(role = Role.Button, onClickLabel = "Close Search", onClick = onClose),
                    contentAlignment = Alignment.Center) { UniverseIcon(UniverseGlyph.CLOSE) }
                Label("SEARCH", AppTheme.eyebrow)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = AppTheme.searchEdgePadding)
                .clip(AppTheme.pill).background(AppTheme.surface)
                .border(AppTheme.hairline, focusBorder, AppTheme.pill)
                .padding(horizontal = AppTheme.medium).heightIn(min = AppTheme.searchHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                SearchGlyph(Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Label("Search tracks", AppTheme.caption)
                    BasicTextField(query, onValueChange = model::setQuery, singleLine = true,
                        textStyle = AppTheme.body, modifier = Modifier.fillMaxWidth(),
                        interactionSource = searchInteractions,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            model.rememberSearch(); keyboard?.hide()
                        }))
                }
                AnimatedVisibility(query.isNotEmpty(),
                    enter = if (motion) fadeIn(tween(AppTheme.searchFocusMs)) +
                        expandHorizontally(tween(AppTheme.searchFocusMs)) else androidx.compose.animation.EnterTransition.None,
                    exit = if (motion) fadeOut(tween(AppTheme.searchFocusMs)) +
                        shrinkHorizontally(tween(AppTheme.searchFocusMs)) else androidx.compose.animation.ExitTransition.None) {
                    Box(Modifier.size(AppTheme.touch)
                        .clickable(role = Role.Button, onClickLabel = "Clear search") { model.setQuery("") },
                        contentAlignment = Alignment.Center) { UniverseIcon(UniverseGlyph.CLOSE) }
                }
            }
            AnimatedVisibility(results is SearchResults.Loading,
                enter = if (motion) fadeIn(tween(AppTheme.searchFocusMs)) +
                    expandVertically(tween(AppTheme.searchFocusMs)) else androidx.compose.animation.EnterTransition.None,
                exit = if (motion) fadeOut(tween(AppTheme.searchFocusMs)) +
                    shrinkVertically(tween(AppTheme.searchFocusMs)) else androidx.compose.animation.ExitTransition.None) {
                SearchLoadingLine(motion)
            }
            LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = AppTheme.searchEdgePadding,
                    end = AppTheme.searchEdgePadding,
                    top = AppTheme.large, bottom = AppTheme.navInset),
                verticalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                if (playbackError != null) item { Label(playbackError!!, AppTheme.caption.copy(color = AppTheme.error)) }
                when (val value = results) {
                    SearchResults.Idle -> {
                        item {
                            RecentSearchSection(
                                recent = recent,
                                onSelect = model::selectRecent,
                                onClear = model::clearRecent,
                            )
                        }
                    }
                    SearchResults.Loading -> item { Label("Searching Spotify…", AppTheme.caption) }
                    is SearchResults.Failed -> item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Label(value.message, AppTheme.caption.copy(color = AppTheme.error),
                                Modifier.weight(1f))
                            Label("RETRY", AppTheme.eyebrow,
                                Modifier.clickable(role = Role.Button, onClick = model::retry))
                        }
                    }
                    is SearchResults.Ready -> {
                        if (value.tracks.isEmpty()) item { Label("No tracks found.", AppTheme.caption) }
                        itemsIndexed(value.tracks, key = { _, track -> track.uri ?: track.name }) { index, track ->
                            CatalogTrackRow(track, mood, onPlay = { model.play(track) },
                                onQueue = { model.queue(track) }, animationIndex = index,
                                animationEnabled = motion)
                        }
                    }
                }
            }
        }
        HomeGrainOverlay(Modifier.matchParentSize())
    }
}

@Composable
private fun RecentSearchSection(
    recent: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.small),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = AppTheme.touch),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label("RECENT SEARCHES", AppTheme.eyebrow, Modifier.weight(1f))
            if (recent.isNotEmpty()) {
                Box(
                    Modifier.heightIn(min = AppTheme.touch)
                        .clickable(role = Role.Button, onClickLabel = "Clear recent searches",
                            onClick = onClear)
                        .padding(horizontal = AppTheme.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Label("CLEAR", AppTheme.caption)
                }
            }
        }
        if (recent.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(AppTheme.pill)
                    .background(AppTheme.surface)
                    .border(AppTheme.hairline, AppTheme.border, AppTheme.pill)
                    .padding(horizontal = AppTheme.medium, vertical = AppTheme.large),
            ) {
                Label("Search Spotify tracks to begin.", AppTheme.caption)
            }
        } else {
            recent.forEach { text ->
                Row(
                    Modifier.fillMaxWidth().firstAppearance("recent:$text")
                        .clip(AppTheme.pill)
                        .background(AppTheme.surface)
                        .border(AppTheme.hairline, AppTheme.border, AppTheme.pill)
                        .clickable(role = Role.Button,
                            onClickLabel = "Search again for $text") { onSelect(text) }
                        .heightIn(min = AppTheme.searchHeight)
                        .padding(horizontal = AppTheme.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.medium),
                ) {
                    UniverseIcon(UniverseGlyph.SEARCH, modifier = Modifier.size(AppTheme.navGlyph),
                        color = AppTheme.muted)
                    BasicText(text, style = AppTheme.body, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CatalogTrackRow(track: WebTrack, mood: MoodViewModel, onPlay: () -> Unit, onQueue: () -> Unit,
    animationIndex: Int? = null, animationEnabled: Boolean = false) {
    var menu by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf(false) }
    val id = track.id ?: track.uri?.substringAfterLast(':')
    val local = id?.let { LocalTrack(it, track.name, track.artists.joinToString(", "),
        track.album, track.coverUrl, track.durationMs) }
    val entrance = if (animationIndex == null) Modifier.firstAppearance(track.uri ?: track.name)
        else Modifier.searchAppearance(track.uri ?: track.name, animationIndex, animationEnabled)
    Row(Modifier.fillMaxWidth().then(entrance).heightIn(min = AppTheme.rowMinHeight)
        .clickable(role = Role.Button, onClickLabel = "Play " + track.name, onClick = onPlay),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
        AsyncImage(coverRequest(track.coverUrl, AppTheme.rowCover), contentDescription = "Cover for " + track.name,
            contentScale = ContentScale.Fit, modifier = Modifier.size(AppTheme.rowCover)
                .clip(AppTheme.artShape).background(AppTheme.surface))
        Column(Modifier.weight(1f)) {
            BasicText(track.name, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(track.artists.joinToString(", "), style = AppTheme.caption,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            Box(Modifier.size(AppTheme.touch)
                .clickable(role = Role.Button, onClickLabel = "More options") { menu = true },
                contentAlignment = Alignment.Center) { UniverseIcon(UniverseGlyph.MORE) }
            AnimatedPopup(visible = menu, onDismissRequest = { menu = false }) {
                Column(Modifier.width(AppTheme.menuWidth).liquidGlass(AppTheme.card)
                    .padding(AppTheme.small)) {
                    listOf("Play" to onPlay, "Add to queue" to onQueue,
                        "Mood tags" to { if (local != null) tags = true }).forEach { (label, action) ->
                        Label(label, AppTheme.body, Modifier.fillMaxWidth().heightIn(min = AppTheme.touch)
                            .clickable(role = Role.Button) { menu = false; action() }
                            .padding(AppTheme.small))
                    }
                }
            }
        }
    }
    if (tags && local != null) TagDialog(local, mood, onDismiss = { tags = false })
}

@Composable
fun SearchGlyph(modifier: Modifier = Modifier) = UniverseIcon(UniverseGlyph.SEARCH, modifier)

@Composable
private fun SearchLoadingLine(motion: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = AppTheme.page,
        vertical = AppTheme.small).height(AppTheme.hairline)) {
        val segment = maxWidth * .28f
        val travel = with(LocalDensity.current) { (maxWidth - segment).toPx() }
        val progress = if (motion) {
            val sweep = rememberInfiniteTransition(label = "search sweep")
            sweep.animateFloat(0f, 1f,
                infiniteRepeatable(tween(AppTheme.searchSweepMs), RepeatMode.Reverse),
                label = "search sweep position").value
        } else 0f
        Box(Modifier.width(segment).fillMaxHeight().graphicsLayer {
            translationX = travel * progress
        }.background(AppTheme.lime))
    }
}

@Composable
private fun Modifier.searchAppearance(key: String, index: Int, motion: Boolean): Modifier {
    val progress = remember(key) { androidx.compose.animation.core.Animatable(0f) }
    val distance = with(LocalDensity.current) { AppTheme.searchEntranceOffset.toPx() }
    LaunchedEffect(key, motion) {
        if (motion) {
            delay(index.coerceAtMost(8) * AppTheme.searchRowStaggerMs)
            progress.animateTo(1f, tween(AppTheme.searchRowMs))
        } else progress.snapTo(1f)
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * distance
    }
}




