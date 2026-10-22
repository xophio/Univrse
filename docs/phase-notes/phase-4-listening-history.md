# Phase 4 — local listening history and mood tags

This phase adds Room tables for tracks, play logs, tags, track-tag links, and notes. The database is version 1, with its exported schema committed under `app/schemas`. Room is configured with KSP and no destructive migration fallback. Version 1 is the migration baseline; a future schema change must add an explicit migration and keep the old JSON schema.

## Manual setup

1. Sync Gradle. This phase adds `androidx.room:room-runtime:2.8.5`, `room-ktx`, and the KSP compiler. No new Spotify scope, dashboard change, or AAR is needed.
2. Install the new debug APK or run `app` from Android Studio. The database is created inside app-private storage as `universe.db`. Existing login and Spotify connection setup remain as before.
3. Open **Stats** for the temporary latest-50 play list and tag counts. Open Now Playing to add tags or a note. Long-press a liked-song row on Home or Library, or use its overflow menu, to edit tags. In the tag dialog, tap a color dot to cycle colors, EDIT to rename, or × to delete.

The six initial tags are gym, focus, chill, late night, sad, and hype. They are seeded once, so renamed or deleted tags stay changed after restart.

## Logging rules and limits

A row starts when a Spotify track is observed playing. The observer uses App Remote's player-state Flow, runs in an application-scoped coroutine, and writes about every five seconds plus on pause, track change, and disconnect. It counts elapsed observed playback time, not jumps in Spotify's position: seeking forward does not credit skipped audio. A row is completed after at least 90% of the track duration has been observed as listened; switching to another track under 30 seconds and below completion marks a skip. A short play closed by disconnect remains partial. Repeat-track wraps near the end create a new row. Recent unfinished rows can be resumed after a quick process restart; stale rows are closed at startup.

**Screen-off limitation:** the existing Phase 1 activity disconnects App Remote in `onStop`. The logger itself remains alive while the process lives, but it receives no player updates after that disconnect. It closes the observed play and never estimates the unobserved interval. Android can also kill the process; up to roughly five seconds of unsaved listening may be lost, and playback while the app is dead cannot be reconstructed. Continuous screen-off logging would require changing the existing connection lifecycle and adding an appropriate foreground service, which is outside this phase's rule to leave App Remote connection code untouched.

The source is `playlist` or `liked` when Universe initiated that play. Plays started elsewhere appear as `spotify`; Spotify's App Remote state does not identify the original source for every later track.

## Test

1. Play a track for under 30 seconds, then skip: the Stats row should say **skipped**.
2. Listen through at least 90%: the row should say **completed**.
3. Pause/resume and seek forward: listened duration should grow only while playing, not by the seek jump.
4. Turn on track repeat and let a track loop: the next loop should create a new row.
5. Add, rename, recolor, and delete tags; add a note; restart and check they persist. Home and Library long-press and overflow should open the same tag editor.
6. Background or screen-off the app, return, and check that no unobserved time was invented.

Validation command: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
