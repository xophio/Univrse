package com.universe.remote.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.remote.data.local.PlaybackSourceTracker
import com.universe.remote.domain.ListeningRepository
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.SpotifyLibraryRepository
import com.universe.remote.domain.WebPlaylist
import com.universe.remote.domain.WebTrack
import com.universe.remote.ui.home.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibrarySort(val label: String) {
    RECENT("Recent"), TITLE("Title"), ARTIST("Artist"), MOST_PLAYED("Most played")
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel @Inject constructor(
    private val web: SpotifyLibraryRepository,
    private val local: ListeningRepository,
    private val player: PlayerController,
    private val sources: PlaybackSourceTracker,
) : ViewModel() {
    private val _songs = MutableStateFlow<List<WebTrack>>(emptyList())
    private val _sort = MutableStateFlow(LibrarySort.RECENT)
    val sort = _sort.asStateFlow()
    private val _tagIds = MutableStateFlow<Set<Long>>(emptySet())
    val tagIds = _tagIds.asStateFlow()
    private val _selectedPlaylist = MutableStateFlow<WebPlaylist?>(null)
    val selectedPlaylist = _selectedPlaylist.asStateFlow()
    private val _detail = MutableStateFlow<Section<List<WebTrack>>>(Section.Loading)
    val detail = _detail.asStateFlow()
    val playbackError = player.error
    val tags = local.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val counts = local.playCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val matchingIds = tagIds.flatMapLatest { local.tracksWithAllTags(it.toList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val visibleSongs = combine(_songs, sort, tagIds, matchingIds, counts) {
            songs, order, selectedTags, matched, plays ->
        LibraryOrdering.apply(songs, order, selectedTags.isNotEmpty(), matched, plays)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val visibleDetail = combine(detail, sort, tagIds, matchingIds, counts) {
            section, order, selectedTags, matched, plays ->
        if (section !is Section.Ready) section
        else {
            Section.Ready(LibraryOrdering.apply(section.value, order,
                selectedTags.isNotEmpty(), matched, plays))
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Section.Loading)
    private var detailJob: Job? = null

    fun setSongs(songs: List<WebTrack>) { _songs.value = songs }
    fun setSort(value: LibrarySort) { _sort.value = value }
    fun toggleTag(id: Long) { _tagIds.value = _tagIds.value.toMutableSet().also {
        if (!it.add(id)) it.remove(id)
    } }
    fun clearTags() { _tagIds.value = emptySet() }
    fun openPlaylist(playlist: WebPlaylist) {
        _selectedPlaylist.value = playlist
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detail.value = Section.Loading
            try { _detail.value = Section.Ready(web.getPlaylistTracks(playlist.id)) }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                _detail.value = Section.Failed("Could not load playlist tracks. Tap retry.")
            }
        }
    }
    fun retryDetail() { selectedPlaylist.value?.let(::openPlaylist) }
    fun closePlaylist() { detailJob?.cancel(); _selectedPlaylist.value = null }
    fun playPlaylist(playlist: WebPlaylist, shuffle: Boolean) {
        sources.mark("playlist")
        player.shuffle(shuffle)
        player.play("spotify:playlist:" + playlist.id)
    }
    fun playTrack(track: WebTrack, fromPlaylist: Boolean) {
        val uri = track.uri ?: return
        val songs = if (fromPlaylist) (visibleDetail.value as? Section.Ready)?.value.orEmpty()
            else visibleSongs.value
        val index = songs.indexOfFirst { it.uri == uri }
        val sequence = if (index >= 0) songs.drop(index).mapNotNull(WebTrack::uri) else listOf(uri)
        sources.mark(if (fromPlaylist) "playlist" else "liked", uri)
        player.playOrdered(sequence)
    }
    fun queue(track: WebTrack) { track.uri?.let(player::queue) }
}
