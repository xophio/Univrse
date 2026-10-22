# Universe

A native Android Spotify remote built with Kotlin and Jetpack Compose. Audio plays through the installed Spotify app, and Universe is a custom, animated interface on top of it.

![Universe screenshot](docs/images/your-screenshot.png)

## Features
- Live player state with album artwork and playback controls
- Mini player and full Now Playing screen
- Synced lyrics with highlighting
- Search, Library and Stats
- Local play history with mood tags and notes
- Animated UI with a liquid glass dock

## Tech Stack
- Kotlin
- Jetpack Compose
- Spotify App Remote SDK
- Gradle (Kotlin DSL)

## Getting Started

**Prerequisites**
- Android Studio with AGP 9.2.1 support (Panda 4 or newer)
- JDK 21 (the bundled Gradle JDK works)
- Android SDK Platform 37 (install via SDK Manager)
- Android device or emulator on Android 8.0 / API 26 or higher, with the Spotify app installed and signed in
- A Spotify Developer dashboard app

**Setup**
1. Clone the repo and open the folder in Android Studio.
```
   git clone https://github.com/xophio/Univrse.git
```
2. Copy `spotify.properties.example` to `spotify.properties` in the root folder.
3. Paste your Spotify client ID into `clientId`. Keep `redirectUri=universe-remote://callback` or use your own, as long as it matches the dashboard. Never put a client secret in the app.
4. In the [Spotify developer dashboard](https://developer.spotify.com/dashboard), create an app for the Android SDK and register:
   - Package name: `com.universe.remote`
   - Your debug signing SHA-1 (run `gradlew.bat :app:signingReport` to get it)
   - The redirect URI from step 3
   - Your test account under User Management, if your app is in development mode
5. Sync Gradle and run the `app` configuration, then accept Spotify's remote-control authorization.

**Build from the command line (Windows)**
```
gradlew.bat assembleDebug
```

## Project Structure
```
app/         Android app source
docs/        Screenshots and extra notes
licenses/    Third-party licenses
```

## What I Learned
- Working with the Spotify App Remote SDK and its authorization flow
- Building custom animated UI in Jetpack Compose
- Managing secrets and config with a properties file kept out of git

## License
MIT
