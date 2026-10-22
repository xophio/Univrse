# Univrse

An Android Spotify remote with a custom pixel-art interface, local listening insights, and synced lyrics. Spotify continues to own audio playback; Univrse provides the control surface.

<!-- Screenshot or GIF placeholder: add docs/images/univrse-home.png. -->

## Highlights

- Spotify App Remote playback controls and live player state
- Spotify Web API login with PKCE, playlists, liked songs, and search
- Custom Jetpack Compose UI: pixel planets, motion, dock, and Now Playing
- Room-backed listening history, mood tags, library filtering, and stats
- LRCLIB-powered synced lyrics cached for offline repeat playback

## Tech stack

Kotlin, Jetpack Compose, Coroutines and Flow, Hilt, Room, Retrofit/OkHttp, Coil, Palette, Spotify Android App Remote SDK, Spotify Auth, and LRCLIB.

## Prerequisites

- Android Studio Panda 4 or newer with its bundled JDK 21
- Android SDK Platform 37
- An Android device or emulator running Android 8.0 (API 26) or later
- The Spotify app installed and signed in on the test device
- A Spotify developer app configured for package `com.universe.remote`

## Setup

1. Clone the repository and open it in Android Studio.
2. Copy `spotify.properties.example` to `spotify.properties` in the repository root.
3. Set `clientId` to your Spotify dashboard client ID. Keep `redirectUri` aligned with the redirect URI registered in Spotify's dashboard.
4. Register the debug signing SHA-1 in Spotify's dashboard. Run `./gradlew.bat :app:signingReport` on Windows to obtain it.
5. Sync Gradle, connect a device with Spotify installed, and run the `app` configuration.

`spotify.properties` is ignored intentionally. Never put a Spotify client secret in the Android app.

### Configuration

| File | Key | Purpose |
| --- | --- | --- |
| `spotify.properties` | `clientId` | Spotify public client ID |
| `spotify.properties` | `redirectUri` | Registered PKCE redirect URI |

Start from [`spotify.properties.example`](spotify.properties.example). This Android project does not load a `.env` file.

## Build and verify

From PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

For a release-like performance build:

```powershell
./gradlew.bat :app:assemblePerformance
```

On macOS or Linux, make the wrapper executable once with `chmod +x gradlew`, then run `./gradlew` in the commands above.

## Project structure

```text
app/
  src/main/java/com/universe/remote/
    data/       Spotify, Room, lyrics, and network implementations
    domain/     UI-independent models and repository contracts
    di/         Hilt modules
    ui/         Compose screens, components, and theme
  src/test/     Unit tests
  schemas/      Room migration schemas
docs/
  phase-notes/  Historical implementation notes by phase
  performance.md
  validation.md
exports/        Project logo source and exported logo
licenses/       Third-party license notices
```

## Documentation

- [Phase notes](docs/phase-notes/)
- [Performance notes](docs/performance.md)
- [Validation notes](docs/validation.md)
- [Spotify setup sample](spotify.properties.example)

## What I learned / next steps

This project explores lifecycle-safe playback state, PKCE on Android, local-first listening data, Compose performance, and a custom visual system without Material components. Potential next steps include broader device testing, a screenshot or GIF in this README, and release signing for a distributable build.

## License

Licensed under the [MIT License](LICENSE). Spotify services and the bundled SDK remain subject to Spotify's terms.
