# Phase 5 — Search, Library, Stats

## Manual setup

Sync Gradle and install the new debug APK, or run the app from Android Studio. Keep the Phase 1–4 Spotify dashboard, credentials, and App Remote AAR setup. Search uses the existing authenticated Web API client and needs no new scope. Queue uses the bundled App Remote Player API. No Room schema migration is needed: this phase adds queries to the existing version 1 database and does not change its tables.

Spotify's current Search endpoint allows up to 10 results per request for this app mode, so this screen shows the first 10 tracks. Search is available from the magnifying glass beside Liked songs on Home and in the Library header. It debounces input by 400 ms; pressing the keyboard Search action or selecting a result saves that query in the on-device recent-search list.

Library has Playlists and Liked songs tabs. Liked songs and opened playlist tracks support recent, title, artist, and most-played sorting. Recent uses Spotify's `added_at` timestamp. Mood chips combine as an **all selected tags** filter. Playlist cards support Spotify's order and title sorting; Spotify's playlist list response has no added timestamp or artist, and playlist mood filtering applies to the tracks after opening a playlist. Tap a playlist card to load its tracks and use Play or Shuffle.

Stats shows 7-day, 30-day, and all-time totals, with a range selector for the charts and rankings. The hour and weekday charts, top tracks, top artists, and mood totals use Room aggregate queries. Listening time is assigned to the hour and day when a play log began; the Phase 4 schema does not store per-minute segments, so a play that crosses an hour/day boundary cannot be split exactly. Long-press the Stats title to open the Phase 4 debug history; use Back or × to close it.

## Test on your phone

1. Open Search and type a track name. Check loading, results, no-results, recent searches, play, queue, and the tag option in the overflow menu. Turn off network briefly for the error state.
2. Open Library. Switch tabs, sort liked songs in all four ways, select multiple mood chips, and open a playlist. Use Play/Shuffle and verify the playlist's tracks.
3. Play a few songs while Universe is open, then open Stats. Check the three totals, range selector, charts, rankings, and mood breakdown. Long-press the heading for the old history screen.
4. With no logs in a chosen range, confirm Stats shows its empty message.

Validation: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
