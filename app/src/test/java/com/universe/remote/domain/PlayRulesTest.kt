package com.universe.remote.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRulesTest {
    @Test fun shortPlayIsSkipped() {
        val rules = PlayRules()
        rules.observe(1_000, true)
        rules.observe(21_000, false)
        assertTrue(rules.skipped(180_000))
        assertTrue(rules.skippedOnExit(180_000, nextTrack = true))
        assertFalse(rules.skippedOnExit(180_000, nextTrack = false))
        assertFalse(rules.completed(180_000))
    }

    @Test fun ninetyPercentListenedIsCompleted() {
        val rules = PlayRules()
        rules.observe(1_000, true)
        repeat(18) { rules.observe(11_000L + it * 10_000, true) }
        rules.observe(191_000, false)
        assertTrue(rules.completed(200_000))
        assertFalse(rules.skipped(200_000))
    }

    @Test fun repeatWrapStartsANewPlayButSeekDoesNot() {
        assertTrue(PlayRules.repeated(195_000, 1_000, 200_000, RepeatMode.TRACK))
        assertFalse(PlayRules.repeated(195_000, 1_000, 200_000, RepeatMode.TRACK, seekRecently = true))
        assertFalse(PlayRules.repeated(195_000, 1_000, 200_000, RepeatMode.OFF))
    }

    @Test fun seekingAheadDoesNotAddPositionJumpToListenedTime() {
        val rules = PlayRules()
        rules.observe(1_000, true)
        rules.observe(6_000, true) // Spotify's position can jump from 5s to 170s here.
        rules.observe(11_000, false)
        assertTrue(rules.listenedMs == 10_000L)
        assertFalse(rules.completed(200_000))
    }
}
