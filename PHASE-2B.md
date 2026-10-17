# Phase 2b — Home

The app now opens the Web API login screen only when the saved session is absent. After login it shows Home. The temporary playlist-name test screen has been replaced. Phase 1 playback controls remain reachable from the avatar menu; the App Remote connection and auth implementation are unchanged. Stats and Library are visible placeholders.

## Manual setup

Keep the Phase 2a `spotify.properties` and [Spotify dashboard configuration](PHASE-2A.md). If you open the source ZIP on a different machine, copy `spotify.properties.example` to `spotify.properties` and paste your client ID and exact registered redirect URI. Register that machine's signing SHA-1 if needed. Install the Spotify app and sign in on the phone.

Gradle adds Coil 2.7.0 `coil-compose`; sync Gradle with network access. This release is compatible with the project's existing Hilt/Kotlin toolchain. Coil downloads album covers from HTTPS Spotify image URLs. There is no new font download: `app/src/main/res/font/silkscreen.ttf` is already bundled with its SIL Open Font License under `licenses/`.

## What to test

1. Open the app with a saved Web API session: Home should open, with the pixel planet, playlist carousel, and liked-song rows. With no session, complete login first.
2. Swipe the playlist carousel. The next card should peek in from the right. Track counts appear when the Web API supplies them; otherwise the card says “Playlist.”
3. Tap **Play** and the shuffle control on a playlist. Both target its Spotify URI through the existing App Remote player. Audio remains in Spotify. The app shows the wrapper's error if playback is unavailable.
4. Check artwork aspect ratio, names and artists in liked songs. The lime heart denotes a saved track; no write scope or unlike action is requested. The overflow menu offers Play and Open in Spotify.
5. Check loading, empty, error and retry states independently for playlists and liked songs. Try offline mode and reconnecting.
6. Tap Home, Stats and Library in the bottom pill. Stats and Library are placeholders. Tap the avatar for playback controls and Log out.
7. Rotate the phone and use a smaller screen or larger system font. The Home list scrolls behind a floating nav.

The planet is a static Canvas grid of blue, green and black squares with fixed orbit rings, moons and stars. There are no animation loops in this phase. Global appearance tokens live in `ui/theme/AppTheme.kt`; the Home UI uses Compose Foundation and Coil, with no Material 3 components.

## Code map

```text
ui/home/PixelPlanet.kt       Static pixel art on Canvas
ui/home/HomeViewModel.kt     Independent playlist/liked-song state and playback actions
ui/home/HomeScreen.kt        Header, carousel, track rows, placeholder tabs and floating nav
```

The Web API DTO/domain mapping adds optional playlist counts and cover URLs while keeping the existing repository method signatures. The app takes the 300px-nearest HTTPS album image and uses `ContentScale.Fit`, so covers are never stretched. Existing tokens, App Remote manager and player wrapper were not edited.

Stop after testing this phase. Now Playing, Room, Search/Stats/Library content, animation and extras come later.
