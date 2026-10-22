# Phase 6 animation validation

The performance build adds a cached 16-frame stepped planet cycle, independent moon orbits, phased star twinkle, eased playback speed, palette tint transition and playback pulse. Home uses spring layout transitions in the nav, like changes bounce, rows enter with fade/translation, and Now Playing enters/exits with scale/fade. No new Spotify scopes or dependencies.

System animator scale is checked while resumed; disabling animations or leaving the foreground stops the planet clock. Normal controls remain usable with motion disabled. Follow-up: the player container now uses Compose sharedBounds between the actual floating bar and the full-screen destination, reversing on close. Contents cross-fade within the shared container; artwork is not independently stretched. Duration and fade timing are in AppTheme. Planet band artwork cross-fades between cached album-derived color filters, preserving black gaps and the base blue/green texture. The orbital clock uses rememberInfiniteTransition and stops after the pause easing finishes.

Build: `./gradlew.bat :app:assemblePerformance :app:testDebugUnitTest :app:lintDebug`.

Frame measurements below are device samples, not a guarantee across screens or phones.


First device sample (RMX5085, optimized build, system animator scale 1): steady playing Home, 732 frames / roughly 12 seconds, p50 9 ms, p95 12 ms, p99 13 ms; current jank 0%, legacy jank 37.70%. Initial mixed tab/Now Playing test: 223 frames, p50 22 ms, p95 31 ms, p99 97 ms; current jank 3.14%. This prompted a bounded spin-frame cache and removal of nav measurement feedback. Reduced motion: 16 frames in four seconds (progress updates), zero current deadline misses; the original system scale was restored. These counters use different deadline definitions; the legacy counter is retained rather than hidden.

Final optimized build sample: Home 489 frames over approximately 8.1 seconds, p50 13 ms, p90 15 ms, p95 16 ms, p99 17 ms; current deadline jank 0%, legacy 85.48%. Mixed tab/Now Playing sequence 214 frames, p50 22 ms, p95 32 ms, p99 69 ms; current jank 2.34%, legacy 89.72%. Sustained Home is approximately 60 fps, but transitions are NOT consistently within 16.67 ms. This pass does not claim that the full 60 fps requirement is achieved. Phone remained on the final performance build. Build, unit tests and lint pass. No new manual setup.


Player expansion follow-up: performance build and lint passed; installed successfully. Device interaction test was stopped when the user switched to another app. Opening/closing visual verification is pending.
