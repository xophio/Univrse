# Universe — Phase 2b

Start with [PHASE-2B.md](PHASE-2B.md) for the current Home screen setup and test instructions. The Phase 2a login notes are in [PHASE-2A.md](PHASE-2A.md); the original Phase 1 setup below is retained for reference.

# Universe — Phase 1

Native Kotlin + Jetpack Compose Spotify remote. Audio is played by the installed Spotify app. This phase contains project setup, design tokens, a real App Remote connection, one fixed test-track action, live player state, album artwork, and playback controls. No Material 3 components are used (the XML launch theme is a platform window theme).

## Open and configure

1. Open this `Universe` folder in Android Studio with support for AGP 9.2.1 (Panda 4 or newer). Use the bundled JDK 21 / Gradle JDK; Java source and bytecode target 17. Install Android SDK Platform 37 through SDK Manager. Minimum device Android version is 8.0 / API 26. Gradle 9.4.1 wrapper is included. AGP 9 uses built-in Kotlin; do not add `org.jetbrains.kotlin.android`.
2. Copy `spotify.properties.example` to **`spotify.properties` in this root folder**. Paste your Spotify dashboard client ID into `clientId`. `redirectUri=universe-remote://callback` is an example placeholder URI; register that exact value or replace it with your own and use the same value in the dashboard. Never put a client secret in the app. Rebuild after changing properties. Client IDs are public identifiers.
3. In the [Spotify developer dashboard](https://developer.spotify.com/dashboard), create/configure an app for the Android SDK. Register package **`com.universe.remote`**, your signing certificate SHA-1, and the redirect URI. Run `gradlew.bat :app:signingReport` to find the debug SHA-1. Register the release certificate separately when applicable. Add your test account under dashboard user access if required for your app mode.
4. Install Spotify on the test device, open it and sign in. Use an account with on-demand playback for the hardcoded-track test. The app checks SDK user capabilities and disables that action if unsupported.
5. Sync Gradle and run `app`. Accept Spotify's remote-control authorization. Tap **Play test track**. Connection does not start or resume music automatically. Change `ConnectionViewModel.TEST_TRACK` to another track URI if it is unavailable in your market.

Windows build:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew.bat :app:assembleDebug :app:lintDebug
```

Use Android Studio to create `local.properties` with your SDK path, or set `ANDROID_HOME`. On macOS/Linux, run `chmod +x gradlew` then `./gradlew :app:assembleDebug`.

## SDK, manifest and authentication setup

- **AAR already included:** `app/libs/spotify-app-remote-release-0.8.0.aar`, from the [official SDK repository](https://github.com/spotify/android-sdk/tree/master/app-remote-lib). `app/build.gradle.kts` references this exact filename and adds Gson explicitly. If restoring it manually, download that version from Spotify's repository/releases and place it at this path. Do not add a second imported SDK module.
- `AndroidManifest.xml` declares Internet permission, Spotify package visibility, the Hilt application and the exported launcher activity. No playback service, audio focus management, or notification permission is needed in this remote UI.
- Phase 1 uses App Remote's built-in `showAuthView(true)` consent. Its callback belongs to Spotify's flow; this phase does not implement an OAuth redirect receiver or Web API token storage. [Spotify's getting-started guide](https://developer.spotify.com/documentation/android/tutorials/getting-started) documents this flow.
- **PKCE, Retrofit and Web API login are Phase 2.** App Remote consent does not supply a Web API access token. Phase 2 will add its separate redirect receiver, verifier/state checks, token exchange and refresh. No client secret will be used. No restricted audio-features or recommendations endpoints are called.
- Silkscreen is included under the SIL Open Font License; see `licenses/Silkscreen-OFL.txt`. Spotify's SDK is governed by its own developer terms; see the official repository and `licenses/Spotify-SDK-LICENSE.txt`.

## Source map

```text
app/src/main/java/com/universe/remote/
  UniverseApplication.kt                 Hilt application
  MainActivity.kt                        onStart/onStop ownership, one NavHost
  data/spotify/
    SpotifyConnectionManager.kt          Singleton SDK owner; state, art, capability flows
    SpotifyPlayerController.kt           Playback wrapper and domain-state mapping
    SpotifyBindingContext.kt             Android 14+ consent opt-in and pending-bind cleanup
  domain/
    PlayerController.kt                  UI-independent contract and models
  di/
    PlaybackModule.kt                    Singleton player binding
  ui/
    theme/AppTheme.kt                    Colors, fonts, dimensions and shapes
    components/Controls.kt               Foundation buttons and text
    components/GrainBackground.kt        Cached, deterministic film grain
    connection/ConnectionViewModel.kt    Screen actions, test-track URI
    connection/ConnectionScreen.kt       Phase 1 connection/playback test surface
```

`SpotifyConnectionManager.playerState` is a `StateFlow<PlayerState?>` (and therefore a Flow). Null means disconnected/no state yet. The wrapper exposes immutable `PlaybackState` to the UI. It supports play, pause, resume, next, previous, seek, shuffle, repeat and respects Spotify's current playback restrictions. Repeat values match the SDK: off 0, track 1, context 2.

The manager keeps only application context, cancels its subscription and disconnects in `onStop`, and invalidates stale callbacks using a session generation. The main thread serializes lifecycle and SDK callback handling. A new `onStart` reconnects. Returning from Spotify authorization may require a retry if Spotify rejected the prior connection. Disconnection never pauses Spotify audio. Failures provide recovery instructions; unsupported on-demand playback is a capability result, not a guessed connection error.

Album artwork is fetched with the App Remote Images API and displayed with `ContentScale.Fit`; there is no crop, tint or stretch. Coil will handle Web API image URLs in Phase 2. Displayed position is the last SDK event position, labelled accordingly, not a fabricated continuously ticking clock.

## Test this phase on your device

1. Before entering credentials: setup-required screen; playback disabled; no crash.
2. With credentials but Spotify missing: install guidance and Play Store link.
3. With Spotify signed out: sign-in/recovery message or Spotify login UI; return and retry.
4. Authorize: connected state, account capability result, current track and metadata.
5. Play test track: Spotify audio and state updates. Pause/resume, previous/next, seek +15s, shuffle and repeat work when enabled. Restrictions disable unsupported controls; failures stay visible.
6. Open in Spotify: current track opens; return to reconnect. Unavailable tracks report failure without claiming playback succeeded.
7. Home/background, return, rotate repeatedly: no stale metadata after disconnect, no duplicate active subscription; audio remains owned by Spotify.
8. Deny authorization, turn off networking or force-stop Spotify while connected: a recoverable error appears; retry after fixing the cause.
9. With a non-on-demand account: test-track action stays disabled and explains the requirement. Do not infer account tier from a generic network error.
10. Large system font and small screen: all controls remain reachable by scrolling; buttons have at least 48dp targets. Album art preserves aspect ratio.

## Phase boundaries

Stop here for testing. Phase 2 adds PKCE/Web API, Coil, static Canvas planet, avatar/notification header, playlist carousel, track rows and floating Home/Stats/Library navigation. Phase 3 adds the mini player and Now Playing. Phase 4 adds Room logs/tags. Phase 5 adds search/library/stats. Phase 6 adds animations and Palette transitions, with a reduce-animations setting. Phase 7 adds smart playlists, sleep timer and row gestures.

There are no running animations in Phase 1. StateFlows, isolated components and cached grain drawing support later animation work; no random generation or collection allocation occurs in the grain draw callback. Room, Retrofit, Coil and Palette dependencies will be added with the phase that uses them.

## Connection fix

On Android 14+, Spotify App Remote 0.8.0 needs the BIND_ALLOW_ACTIVITY_STARTS service-binding flag to display its built-in authorization screen. SpotifyBindingContext applies this only to the explicit Spotify protocol service, and closes pending bindings on timeout or onStop. The SDK calls getApplicationContext internally, so this application-context adapter returns itself. No Activity reference is retained. Verified on the connected device: connected, on-demand capable, track state PLAYING.


