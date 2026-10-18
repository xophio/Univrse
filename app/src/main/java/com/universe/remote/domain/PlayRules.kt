package com.universe.remote.domain

/** Pure accounting rules. Position jumps never count as listening. */
class PlayRules(
    var listenedMs: Long = 0,
    private var lastElapsedMs: Long = 0,
    private var playing: Boolean = false,
) {
    fun observe(elapsedMs: Long, nowPlaying: Boolean) {
        if (playing && lastElapsedMs > 0) {
            listenedMs += (elapsedMs - lastElapsedMs).coerceIn(0, 10_000)
        }
        lastElapsedMs = elapsedMs
        playing = nowPlaying
    }
    fun completed(durationMs: Long) = durationMs > 0 && listenedMs >= durationMs * 9 / 10
    fun skipped(durationMs: Long) = !completed(durationMs) && listenedMs < 30_000
    fun skippedOnExit(durationMs: Long, nextTrack: Boolean) = nextTrack && skipped(durationMs)

    companion object {
        fun repeated(previousPositionMs: Long, positionMs: Long, durationMs: Long,
            repeat: RepeatMode, seekRecently: Boolean = false): Boolean =
            !seekRecently && repeat == RepeatMode.TRACK && durationMs > 0 &&
                previousPositionMs >= durationMs * 9 / 10 && positionMs <= 10_000 &&
                positionMs < previousPositionMs
    }
}
