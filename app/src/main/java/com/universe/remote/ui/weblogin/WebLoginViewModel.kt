package com.universe.remote.ui.weblogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.PKCEInformation
import com.universe.remote.BuildConfig
import com.universe.remote.data.auth.*
import com.universe.remote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException

data class WebLoginUiState(val busy: Boolean = false, val message: String? = null,
    val playlists: List<WebPlaylist> = emptyList(), val loaded: Boolean = false)

@HiltViewModel
class WebLoginViewModel @Inject constructor(private val tokens: TokenManager,
    private val repository: SpotifyLibraryRepository) : ViewModel() {
    val loggedIn = tokens.loggedIn
    private val _ui = MutableStateFlow(WebLoginUiState())
    val ui = _ui.asStateFlow()
    private var task: Job? = null
    init { task = viewModelScope.launch {
        try { withContext(Dispatchers.IO) { tokens.restore() }; if (loggedIn.value) load() }
        catch (e: Exception) { failure(e) }
    } }
    fun login(launch: (AuthorizationRequest) -> Unit) {
        if (_ui.value.busy) return
        task = viewModelScope.launch {
            _ui.value = WebLoginUiState(busy = true)
            try {
                val pending = withContext(Dispatchers.IO) { tokens.begin() }
                val request = AuthorizationRequest.Builder(BuildConfig.SPOTIFY_CLIENT_ID,
                    AuthorizationResponse.Type.CODE, BuildConfig.SPOTIFY_REDIRECT_URI)
                    .setScopes(arrayOf("user-library-read", "playlist-read-private", "user-read-recently-played"))
                    .setState(pending.state)
                    .setPkceInformation(PKCEInformation.sha256(pending.verifier, Pkce.challenge(pending.verifier)))
                    .build()
                launch(request)
            } catch (e: Exception) { withContext(Dispatchers.IO) { tokens.cancel() }; failure(e) }
        }
    }
    fun result(response: AuthorizationResponse?, cancelled: Boolean) {
        task = viewModelScope.launch {
            _ui.value = WebLoginUiState(busy = true)
            try {
                if (cancelled || response?.type == AuthorizationResponse.Type.CANCELLED) {
                    withContext(Dispatchers.IO) { tokens.cancel() }
                    _ui.value = WebLoginUiState(message = "Login cancelled. You can try again.")
                } else if (response?.type == AuthorizationResponse.Type.CODE) {
                    withContext(Dispatchers.IO) { tokens.complete(response.code.orEmpty(), response.state) }
                    load()
                } else {
                    withContext(Dispatchers.IO) { tokens.cancel() }
                    _ui.value = WebLoginUiState(message = "Spotify login was denied or failed. Check your dashboard redirect URI, package and signing SHA-1, then retry.")
                }
            } catch (e: Exception) { failure(e) }
        }
    }
    fun reload() { if (!_ui.value.busy) task = viewModelScope.launch { try { load() } catch (e: Exception) { failure(e) } } }
    private suspend fun load() {
        _ui.value = WebLoginUiState(busy = true)
        val playlists = repository.getMyPlaylists()
        _ui.value = WebLoginUiState(playlists = playlists, loaded = true)
    }
    fun logout() {
        task?.cancel()
        task = viewModelScope.launch {
            withContext(Dispatchers.IO) { tokens.logout() }
            _ui.value = WebLoginUiState(message = "Web API credentials removed from this device.")
        }
    }
    private fun failure(e: Exception) {
        if (e is CancellationException) throw e
        // Class name only: never log HTTP bodies, codes, tokens or callback URIs.
        if (BuildConfig.DEBUG) android.util.Log.w("UniverseWebApi", "Request failed: ${e.javaClass.name}")
        val message = when (e) {
            is HttpException -> when (e.code()) {
                401 -> "Spotify rejected this session. Log out and log in again."
                403 -> "Spotify denied access. Check scopes, dashboard test-user access and development-mode eligibility."
                429 -> "Spotify rate limit reached. Retry after ${e.response()?.headers()?.get("Retry-After") ?: "a few"} seconds."
                else -> "Spotify request failed (${e.code()}). Retry later."
            }
            is LoginRequired -> e.message!!
            is java.net.UnknownHostException -> "Cannot reach Spotify's server. Check internet, VPN and Private DNS, then reload playlists."
            is java.io.InterruptedIOException -> "Spotify's request timed out. Check your connection, then reload playlists."
            is javax.net.ssl.SSLException -> "A secure connection to Spotify failed. Check the device clock, VPN or network filtering."
            is IllegalArgumentException -> e.message ?: "Login could not be verified."
            else -> "Could not load Spotify data or securely save the session (${e.javaClass.simpleName}). Try again."
        }
        _ui.value = _ui.value.copy(busy = false, message = message)
    }
}
