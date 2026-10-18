package com.universe.remote.data.local

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/** A short-lived hint for a play initiated inside Universe. External Spotify plays remain "spotify". */
@Singleton
class PlaybackSourceTracker @Inject constructor() {
    private data class Pending(val source: String, val expectedUri: String?, val until: Long)
    private var pending: Pending? = null
    @Synchronized fun mark(source: String, expectedUri: String? = null) {
        pending = Pending(source, expectedUri, SystemClock.elapsedRealtime() + 15_000)
    }
    @Synchronized fun consume(trackUri: String): String {
        val hint = pending
        pending = null
        return if (hint != null && SystemClock.elapsedRealtime() <= hint.until &&
            (hint.expectedUri == null || hint.expectedUri == trackUri)) hint.source else "spotify"
    }
}
