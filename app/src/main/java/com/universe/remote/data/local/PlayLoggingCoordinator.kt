package com.universe.remote.data.local

import android.os.SystemClock
import android.util.Log
import android.content.Context
import com.universe.remote.domain.ListeningRepository
import com.universe.remote.domain.LocalTrack
import com.universe.remote.domain.PlayRules
import com.universe.remote.domain.PlaybackState
import com.universe.remote.domain.PlayerController
import com.universe.remote.ui.theme.AppTheme
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Application-scoped observer, independent of Compose screens.
 * The existing Activity disconnects App Remote in onStop; a foreground service would be needed
 * to keep observing Spotify while the screen is off. This class never guesses unobserved time.
 */
@Singleton
class PlayLoggingCoordinator @Inject constructor(
    private val player: PlayerController,
    private val repository: ListeningRepository,
    private val sources: PlaybackSourceTracker,
    private val seeks: SeekIntentTracker,
    @param:ApplicationContext private val context: Context,
) {
    private sealed interface Event {
        data class State(val value: PlaybackState) : Event
        data object Tick : Event
    }
    private data class Session(
        val id: Long, val uri: String, val startedAt: Long, val rules: PlayRules,
        var durationMs: Long, var lastPositionMs: Long, var playing: Boolean,
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private var session: Session? = null
    private var started = false

    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch { player.playback.collect { events.send(Event.State(it)) } }
        scope.launch { while (true) { delay(5_000); events.send(Event.Tick) } }
        scope.launch {
            val prefs = context.getSharedPreferences("universe_local", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("mood_tags_seeded", false)) {
                AppTheme.defaultMoodTags.forEach { (name, color) ->
                    repository.addTag(name, color.value.toLong())
                }
                prefs.edit().putBoolean("mood_tags_seeded", true).apply()
            }
            repository.closeStalePlays(System.currentTimeMillis() - 30_000)
            for (event in events) {
                try { handle(event) }
                catch (e: Exception) { Log.w("UniversePlayLog", "Could not persist play observation", e) }
            }
        }
    }

    private suspend fun handle(event: Event) {
        val elapsed = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()
        val current = session
        if (event == Event.Tick) {
            if (current != null) {
                current.rules.observe(elapsed, current.playing)
                persist(current, wall, close = false)
            }
            return
        }
        val state = (event as Event.State).value
        val trackId = state.uri.takeIf { it.startsWith("spotify:track:") }?.substringAfterLast(':')
        val repeated = current != null && current.uri == state.uri &&
            PlayRules.repeated(current.lastPositionMs, state.positionMs, current.durationMs,
                state.repeat, seeks.recent(elapsed))
        if (current != null && (trackId == null || current.uri != state.uri || repeated)) {
            current.rules.observe(elapsed, current.playing)
            persist(current, wall, close = true, skipEligible = trackId != null)
            session = null
        }
        if (trackId == null) return
        if (session == null && !state.paused) {
            repository.saveTrack(LocalTrack(trackId, state.title, state.artist, state.album,
                state.coverUrl, state.durationMs))
            val resume = repository.recentOpenPlay(trackId, wall - 30_000)
            val id = resume?.id ?: repository.startPlay(trackId, wall, sources.consume(state.uri))
            session = Session(id, state.uri, resume?.startedAt ?: wall,
                PlayRules(listenedMs = resume?.listenedMs ?: 0), state.durationMs,
                state.positionMs, !state.paused)
            session!!.rules.observe(elapsed, !state.paused)
            return
        }
        session?.let {
            it.rules.observe(elapsed, !state.paused)
            it.durationMs = state.durationMs
            it.lastPositionMs = state.positionMs
            it.playing = !state.paused
            if (state.paused) persist(it, wall, close = false)
        }
    }

    private suspend fun persist(value: Session, wall: Long, close: Boolean,
        skipEligible: Boolean = false) {
        repository.updatePlay(value.id, value.rules.listenedMs,
            value.rules.completed(value.durationMs),
            close && value.rules.skippedOnExit(value.durationMs, skipEligible),
            wall, if (close) wall else null)
    }
}
