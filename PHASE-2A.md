# Phase 2a — Spotify Web API login

This phase adds Web API authentication and a temporary playlist-name screen. The existing App Remote manager, binding adapter, playback wrapper, controls and lifecycle connect/disconnect logic are unchanged. `MainActivity` only adds the new screen/ViewModel and navigation route. Android Back returns from the Phase 1 playback screen to the login screen.

## Manual setup

1. Keep your working `spotify.properties` in the project root. The same `clientId` and `redirectUri` are used for Web API login. If opening the source ZIP elsewhere, copy `spotify.properties.example` to `spotify.properties` and supply your client ID. Never add a client secret.
2. In your Spotify dashboard app settings, enable/configure Web API use and register the **exact** redirect URI from your properties (currently the example is `universe-remote://callback`). Preserve the working Android package `com.universe.remote` and signing SHA-1. For a different build machine, run `gradlew.bat :app:signingReport` and register its fingerprint.
3. If the app is in Development Mode, ensure the Spotify account used for testing is allowlisted in dashboard user access and that the app owner meets Spotify's current Premium requirement. A 403 can indicate account/app access restrictions even after successful login. Playlist item access can also be limited to playlists the user owns or collaborates on in Development Mode. See the [Spotify migration guide](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide).
4. Sync Gradle. Spotify Auth **5.0.0**, Retrofit **2.11.0**, and OkHttp **4.12.0** resolve from Maven Central; no new manual AAR download is needed. Keep the existing App Remote AAR.
5. Run the app, tap **Log in with Spotify**, and approve these scopes: `user-library-read`, `playlist-read-private`, `user-read-recently-played`. The temporary screen then loads all pages of playlist names. Recently played is authorized for later work; no recently-played screen or endpoint call is added now.

The manifest declares Spotify Auth 5's `RedirectUriReceiverActivity`. Scheme and host placeholders come from `redirectUri` at build time. The current custom-scheme redirect needs no Android App Links asset file. The filter accepts that scheme/host; the authorization request and token exchange both use the complete configured URI. Do not replace it with an HTTPS redirect without also implementing verified Android App Links.

## Authentication and storage

- Uses Spotify's `AuthorizationRequest`/`AuthorizationClient` with response type **CODE**, an explicit **S256** PKCE challenge, and a fresh cryptographically random verifier and state. This is not implicit-token login. The app exchanges the code through Retrofit's separate Accounts client.
- Expected state and verifier persist encrypted for process recreation. State is compared before exchange, expires after ten minutes, and is consumed once. Cancellation clears the pending transaction. If the library/browser cannot resume a process-death flow, retry login; an unsolicited callback cannot establish a session without a matching pending state.
- Access and refresh tokens are encrypted with **AES-256-GCM** using a non-exportable Android Keystore key. Ciphertext is written atomically under `noBackupFilesDir`, excluded from backup and transfer. Invalidated keys/corrupt storage require login again; there is no plaintext fallback. Logout deletes local Web API credentials, not Spotify app login or remote authorization.
- Refresh occurs automatically before a request when expiry is less than 60 seconds away, or once after a 401. Concurrent requests share one serialized refresh. Rotated refresh tokens replace the old token; omitted refresh tokens retain the old one. `invalid_grant` clears the session; transient network/server failures preserve credentials for retry.
- Refresh uses an independent HTTP client to avoid interceptor recursion. The Web API interceptor only sends credentials to HTTPS `api.spotify.com`, refuses other hosts, and does not follow redirects. No HTTP body/token logging interceptor is installed. A second 401 is surfaced rather than retried indefinitely.
- This is on-demand refresh when API calls are made, not a background timer waking the app every hour.

## Repository contract

`domain/SpotifyLibraryRepository.kt` exposes:

```kotlin
suspend fun getMyPlaylists(): List<WebPlaylist>
suspend fun getPlaylistTracks(id: String): List<WebTrack>
suspend fun getLikedSongs(): List<WebTrack>
```

All methods paginate. They call `GET /v1/me/playlists`, `GET /v1/playlists/{id}/items` (the current route), and `GET /v1/me/tracks`. Nullable/unavailable items are handled, episodes are excluded from track results, and local track IDs may be null. The parser accepts both the newer `item` and older `track` wrapper names. No restricted recommendations/audio-features endpoints are used. `playlist-read-collaborative` is intentionally not requested because it was not in this phase's scope list.

## Files added

```text
app/src/main/java/com/universe/remote/
  data/auth/AuthModels.kt                 PKCE generation/validation and storage models
  data/auth/KeystoreAuthStorage.kt         Android Keystore encrypted atomic storage
  data/auth/TokenManager.kt                Code exchange, expiry and single refresh
  data/auth/WebAuthInterceptor.kt          Bearer header and bounded 401 retry
  data/web/SpotifyWebApi.kt                Retrofit endpoints and DTOs
  data/web/WebLibraryRepository.kt         Pagination and domain mapping
  domain/SpotifyLibraryRepository.kt       Repository interface and models
  di/WebApiModule.kt                       Separate Accounts/Web API clients
  ui/weblogin/WebLoginViewModel.kt         Login result handling and UI state
  ui/weblogin/WebLoginScreen.kt            Temporary test UI
app/src/test/java/com/universe/remote/data/
  auth/TokenManagerTest.kt
  auth/WebAuthInterceptorTest.kt
  web/WebLibraryRepositoryTest.kt
```

Existing files changed: `app/build.gradle.kts`, `AndroidManifest.xml`, and `MainActivity.kt` for dependencies, redirect receiver and screen navigation. Documentation is also updated. No source in `data/spotify/` was changed; file hashes were compared to the working Phase 1 baseline.

## Validation and device checklist

Build, lint and six JVM tests pass. Tests cover the RFC PKCE vector, state mismatch/expiry, callback replay, verifier exchange, concurrent refresh, token rotation/retention, invalid grant versus transient failure, bounded 401 retry, host rejection, and pagination with null items. They use fake responses, not your Spotify tokens.

On your device:

1. Login and confirm the names match your Spotify playlists. An empty account should show an explicit empty state.
2. Cancel login/back out; confirm you can start again.
3. Reload playlists, close/reopen the app, and confirm the saved session works.
4. After about an hour, reload to exercise real refresh. No second consent should be required unless Spotify revoked the refresh token.
5. Disconnect networking and reload; retry after reconnecting. Check 403/429 messages if Spotify returns them.
6. Log out and verify playlist names disappear; login again.
7. Open Phase 1 playback controls and confirm App Remote still works.

The login screen and redirect receiver were verified on the connected phone. A Web API session was saved, but the live playlist request did not complete successfully during the initial device check. Clear network/timeout diagnostics are included; real playlist retrieval and a real expiry/refresh cycle still need account testing. Stop after this phase; the planet/Home UI and other Phase 2 features are not included.

References: [Spotify Auth 5.0 source](https://github.com/spotify/android-auth/tree/5.0.0), [PKCE flow](https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow), [Auth redirect receiver setup](https://github.com/spotify/android-auth#readme).

