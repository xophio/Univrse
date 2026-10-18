# Home performance pass

## Changes

- Planet and moons are rasterized once per draw cache lifetime. Scrolling and player updates draw one bitmap; a layout size/density change rebuilds it. The palette remains the existing static AppTheme palette. Extra transparent padding preserves the original overflowing orbit rings. The olive glow is unchanged and its Brush is cached.
- Both grain layers now tile a 256 x 256 texture with the same seeds, opacity, one-pixel grain size, and point density. The noise pattern is tiled; layout, colors and artwork geometry are unchanged.
- Home observes only a distinct hasTrack boolean. MiniPlayer and Now Playing observe playback metadata/controls without position. Position remains in the dedicated progress/scrubber flow. Mini progress reads position during drawing, avoiding composition and layout every 250 ms.
- Existing lazy lists are retained. Home uses Spotify IDs instead of index-dependent keys; search history, sort chips, and stats rows have explicit keys. Coil cover requests explicitly use the displayed pixel size and are remembered by URL/size. Artist text, local tag-sheet track data and mini-player bitmap wrappers are remembered.
- Pixel title geometry, icon Strokes, paths, bitmap and shader setup are cached. Chart draw loops use index access instead of allocating iterators.
- Immutable primitive-only playback and playlist models declare their contract. Collection-bearing models are not falsely annotated immutable. Strong skipping allows unchanged collection/model instances to skip; compiler reports can verify this.

## Release-like test build

Select `performance` in Android Studio's Build Variants, or run:

```powershell
.\gradlew.bat :app:assemblePerformance
```

This variant is non-debuggable, R8 optimized, resource-shrunk and shell-profileable. It uses the existing debug signing key and application ID so it can update the phone's current app while retaining login and Room data. It is a local test build, not a production signing setup. No new Spotify scopes or dashboard changes.

Output: `app/build/outputs/apk/performance/app-performance.apk`.

The first build downloads Kotlin's Compose mapping tool. R8 rules retain reflective Gson/Spotify models. Missing optional Jackson adapter types are narrowly suppressed; the bundled SDK's ConnectionParams selects Gson by default.

## Stability and validation

Compiler reports are in `app/build/compose-reports`. To regenerate reports specifically for the app (unit-test compilation uses the same output directory), run:

```powershell
.\gradlew.bat :app:compilePerformanceKotlin --rerun
```

Compare scrolling on the same phone, with the same loaded library and playback state, using the performance variant. Debug mode adds overhead. No percentage improvement is claimed without a controlled device trace. Likely contributors were repeated planet/grain drawing, broad playback-state collection and debug-build overhead. The existing 250 ms interpolated position flow was already separate, but mini progress still caused layout on each tick before this pass.

Validation: debug and performance APK builds, unit tests and lint completed successfully. Compiler reports confirm HomeScreen, PixelPlanet, PlaylistCard, LikedTrackRow and MiniPlayer are skippable. PlaybackState and WebPlaylist are stable; WebTrack remains collection-unstable but its rows are skippable through strong skipping. Device frame-time comparison was not completed: the phone was in use and its existing gfxinfo dump failed. No on-device speedup percentage or visual comparison is claimed.
