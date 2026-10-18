package com.universe.remote.data.local

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/** Prevents an in-app seek back to the beginning from looking like a repeat wrap. */
@Singleton
class SeekIntentTracker @Inject constructor() {
    @Volatile private var lastSeekElapsedMs = Long.MIN_VALUE
    fun mark() { lastSeekElapsedMs = SystemClock.elapsedRealtime() }
    fun recent(nowElapsedMs: Long) = nowElapsedMs - lastSeekElapsedMs in 0..3_000
}
