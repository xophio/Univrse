package com.universe.remote.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.remote.data.local.PlaybackSourceTracker
import com.universe.remote.domain.PlayerController
import com.universe.remote.domain.SpotifyLibraryRepository
import com.universe.remote.domain.WebTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray

sealed interface SearchResults {
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Ready(val tracks: List<WebTrack>) : SearchResults
    data class Failed(val message: String) : SearchResults
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val library: SpotifyLibraryRepository,
    private val player: PlayerController,
    private val sources: PlaybackSourceTracker,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val prefs = context.getSharedPreferences("universe_search", Context.MODE_PRIVATE)
    private val _query = MutableStateFlow("")
    private val retryNonce = MutableStateFlow(0)
    val query = _query.asStateFlow()
    private val _results = MutableStateFlow<SearchResults>(SearchResults.Idle)
    val results = _results.asStateFlow()
    private val _recent = MutableStateFlow(readRecent())
    val recent = _recent.asStateFlow()
    val playbackError = player.error

    init {
        viewModelScope.launch {
            combine(query, retryNonce) { text, _ -> text }.collectLatest { text ->
                val q = text.trim()
                if (q.isEmpty()) { _results.value = SearchResults.Idle; return@collectLatest }
                _results.value = SearchResults.Loading
                delay(650)
                try { _results.value = SearchResults.Ready(library.searchTracks(q)) }
                catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _results.value = SearchResults.Failed(when (e) {
                        is java.net.UnknownHostException -> "No network connection."
                        is retrofit2.HttpException -> if (e.code() == 429) {
                            e.response()?.headers()?.get("Retry-After")?.let {
                                "Spotify search is cooling down. Try again in $it seconds."
                            } ?: "Spotify search is cooling down. Try again shortly."
                        } else "Spotify search failed (${e.code()})."
                        else -> "Could not search Spotify. Try again."
                    })
                }
            }
        }
    }
    fun setQuery(text: String) { _query.value = text.take(150) }
    fun retry() { retryNonce.value++ }
    fun rememberSearch() {
        val q = query.value.trim()
        if (q.isEmpty()) return
        val values = (listOf(q) + _recent.value.filterNot { it.equals(q, ignoreCase = true) }).take(10)
        _recent.value = values
        prefs.edit().putString("recent", JSONArray(values).toString()).apply()
    }
    fun selectRecent(text: String) { setQuery(text); rememberSearch() }
    fun clearRecent() {
        _recent.value = emptyList()
        prefs.edit().remove("recent").apply()
    }
    fun play(track: WebTrack) {
        val uri = track.uri ?: return
        rememberSearch()
        sources.mark("search", uri)
        player.play(uri)
    }
    fun queue(track: WebTrack) {
        val uri = track.uri ?: return
        rememberSearch()
        player.queue(uri)
    }
    private fun readRecent(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("recent", "[]"))
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
}
