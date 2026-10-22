# Phase 3 — mini player and Now Playing

This phase adds a floating mini player above the Home, Stats, and Library navigation whenever App Remote reports a loaded track. Tapping the bar opens Now Playing. Swipe downward or tap the down chevron to close it.

Now Playing shows App Remote artwork without cropping, metadata, an adjustable progress scrubber, shuffle, previous, play/pause, next, repeat, library heart, a small static planet, and an Open in Spotify link. Its subtle background tone is calculated from the cover using Palette. Position is extrapolated from App Remote state while playing; only the mini progress and Now Playing scrubber collect the 250 ms ticker. Spotify remains responsible for audio.

## Manual setup

1. Open this project in Android Studio and sync Gradle. The new dependency is `androidx.palette:palette:1.0.0` in `app/build.gradle.kts`.
2. Keep the existing `spotify.properties`, Spotify dashboard redirect, Spotify app installation, and bundled App Remote AAR as set up in Phases 1–2b. No new Spotify scopes or reauthorization are required. Like/unlike uses the App Remote 0.8.0 User API.
3. Run the `app` configuration or install `app/build/outputs/apk/debug/app-debug.apk`. The debug APK in the parent `outputs` folder is a copy for direct installation.

## Test on a phone

1. Start or resume a track through Spotify or the Home playlist controls. Check that the mini player appears on Home, Stats, and Library and that play/pause works. With no loaded track, the mini player should be absent.
2. Tap the mini player. Check artwork aspect ratio, title, artist, dark cover tone, progress, and Open in Spotify. Swipe down or tap the chevron to return.
3. Drag or tap the progress line to seek. Test shuffle, skip, play/pause, and repeat. Spotify can disable controls according to the current content and account.
4. Tap the heart, then tap again to restore its previous state. It is lime for a liked track. If the App Remote library query fails or the action is unsupported, the control stays disabled or an error appears.
5. Change tracks in Spotify while Now Playing is open. Metadata, artwork, colors, progress, and heart state should follow the new track. Disconnect or background/return to check recovery.

Build checks: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.

Only Phase 3 is included. The earlier auth implementation, connection lifecycle, and Home data fetching remain unchanged.
