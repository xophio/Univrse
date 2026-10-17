# Phase 2b verification

- Final `assembleDebug`, `testDebugUnitTest`, and `lintDebug` passed on October 17, 2026. Six JVM tests passed; lint reported no errors.
- Installed the final APK on the connected Android phone and inspected a screenshot of the Home screen. The saved session opened Home and displayed real playlist names/counts, liked songs, and artwork. The static planet, next-card peek, bell, lime hearts, and floating navigation rendered.
- The three Phase 1 App Remote implementation files match their saved SHA-256 hashes; no code in `data/auth/` was edited for Phase 2b.
- Playlist Play and shuffle taps, offline/error states, logout/relogin, and a fresh Web API login were not exercised during this verification; use the [Phase 2b checklist](PHASE-2B.md) on the phone.
- No auth tokens, `spotify.properties`, or `local.properties` are included in the Phase 2b source ZIP. The debug APK uses the existing locally configured public client ID and redirect URI.

## Earlier Phase 2a verification

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`: passed; lint has 0 errors (13 warnings, including dependency update notices and existing style/configuration suggestions).
- Six JVM tests passed: PKCE RFC vector/state expiry and mismatch; serialized refresh; invalid grant vs transient error; code exchange and replay; bounded 401 retry/host checks; playlist pagination/null items.
- Working Phase 1 `data/spotify/` source hashes are unchanged. MainActivity's onStart/onStop playback lifecycle is unchanged.
- Installed on the connected physical phone. Verified the temporary login UI and resolution of universe-remote://callback to Spotify's RedirectUriReceiverActivity.
- Phone reported a saved Web API session; initial playlist fetch failed. The latest build distinguishes DNS/network, timeout and TLS errors. A successful live playlist fetch and real expiry refresh remain unverified.
- No auth tokens or private properties are included in the source ZIP. The debug APK uses the existing locally configured public client ID/redirect URI.

See PHASE-2A.md for the earlier login setup and testing.
