package com.universe.remote.data.spotify

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.universe.remote.BuildConfig
import com.universe.remote.domain.ConnectionState
import com.universe.remote.domain.ConnectionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** All SDK ownership lives here. Generation invalidates callbacks from stopped/replaced sessions. */
@Singleton
class SpotifyConnectionManager @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var started = false
    internal var remote: SpotifyAppRemote? = null
        private set
    private var subscription: Subscription<PlayerState>? = null
    private val _connection = MutableStateFlow(ConnectionState())
    val connection = _connection.asStateFlow()
    private val _playerState = MutableStateFlow<PlayerState?>(null)
    val playerState = _playerState.asStateFlow()
    private val _onDemand = MutableStateFlow<Boolean?>(null)
    val onDemand = _onDemand.asStateFlow()
    private val _artwork = MutableStateFlow<Bitmap?>(null)
    val artwork = _artwork.asStateFlow()
    private var artUri: String? = null
    private var connectionTimeout: Runnable? = null
    private var bindingContext: SpotifyBindingContext? = null

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post { block() }
    }

    fun start() = onMain {
        started = true
        connect()
    }

    fun retry() = onMain { if (started) { clearRemote(); connect() } }

    private fun connect() {
        if (remote != null || _connection.value.status == ConnectionStatus.CONNECTING) return
        if (!BuildConfig.SPOTIFY_CLIENT_ID.matches(Regex("[a-fA-F0-9]{32}"))) {
            _connection.value = ConnectionState(ConnectionStatus.SETUP_REQUIRED, "Add your client ID to spotify.properties, then rebuild.")
            return
        }
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo("com.spotify.music", 0)
        } catch (_: PackageManager.NameNotFoundException) {
            _connection.value = ConnectionState(ConnectionStatus.NOT_INSTALLED, "Install Spotify, sign in, then return here.")
            return
        }
        val ticket = ++generation
        _connection.value = ConnectionState(ConnectionStatus.CONNECTING, "Connecting to Spotify…")
        connectionTimeout = Runnable {
            if (ticket == generation && remote == null) {
                clearRemote()
                _connection.value = ConnectionState(ConnectionStatus.ERROR, "Spotify did not respond. Open Spotify and retry.")
            }
        }.also { main.postDelayed(it, 30_000) }
        val params = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(BuildConfig.SPOTIFY_REDIRECT_URI).showAuthView(true).build()
        val sessionContext = SpotifyBindingContext(context)
        bindingContext = sessionContext
        try {
            SpotifyAppRemote.connect(sessionContext, params, object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) = onMain {
                    if (!started || ticket != generation) {
                        SpotifyAppRemote.disconnect(appRemote)
                        return@onMain
                    }
                    remote = appRemote
                    connectionTimeout?.let(main::removeCallbacks)
                    connectionTimeout = null
                    _connection.value = ConnectionState(ConnectionStatus.CONNECTED, "Connected to Spotify")
                    subscription = appRemote.playerApi.subscribeToPlayerState().also { sub ->
                        sub.setEventCallback { state -> onMain {
                            if (ticket == generation) {
                                _playerState.value = state
                                val uri = state.track?.imageUri
                                if (uri?.raw != artUri) {
                                    artUri = uri?.raw
                                    _artwork.value = null
                                    if (uri != null) appRemote.imagesApi.getImage(uri)
                                        .setResultCallback { bitmap -> onMain {
                                            if (ticket == generation && artUri == uri.raw) _artwork.value = bitmap
                                        } }.setErrorCallback { /* Artwork is optional; retain placeholder. */ }
                                }
                            }
                        } }
                        sub.setErrorCallback { failure -> onMain { if (ticket == generation) fail(failure) } }
                    }
                    appRemote.userApi.getCapabilities().setResultCallback { capabilities -> onMain {
                        if (ticket == generation) _onDemand.value = capabilities.canPlayOnDemand
                    } }.setErrorCallback { failure -> onMain {
                        if (ticket == generation) fail(failure)
                    } }
                }
                override fun onFailure(throwable: Throwable) = onMain { if (ticket == generation) fail(throwable) }
            })
        } catch (failure: Exception) { fail(failure) }
    }

    private fun fail(failure: Throwable) {
        clearRemote()
        val names = generateSequence(failure) { it.cause }.take(8).map { it.javaClass.simpleName }.toSet()
        val (status, message) = when {
            "CouldNotFindSpotifyApp" in names -> ConnectionStatus.NOT_INSTALLED to "Install Spotify and return here."
            "NotLoggedInException" in names -> ConnectionStatus.LOGIN_REQUIRED to "Open Spotify and sign in, then retry."
            "UserNotAuthorizedException" in names -> ConnectionStatus.AUTH_REQUIRED to "Approve Spotify remote access when you retry."
            "AuthenticationFailedException" in names -> ConnectionStatus.AUTH_REQUIRED to "Check the client ID, redirect URI and dashboard package fingerprint."
            "OfflineModeException" in names -> ConnectionStatus.ERROR to "Disable offline mode in Spotify, then retry."
            "UnsupportedFeatureVersionException" in names -> ConnectionStatus.ERROR to "Update the Spotify app, then retry."
            else -> ConnectionStatus.ERROR to "Spotify connection lost or unavailable. Open Spotify, then retry. (${failure.javaClass.simpleName})"
        }
        _connection.value = ConnectionState(status, message)
    }

    private fun clearRemote() {
        generation++
        connectionTimeout?.let(main::removeCallbacks)
        connectionTimeout = null
        subscription?.cancel()
        subscription = null
        remote?.let { SpotifyAppRemote.disconnect(it) }
        bindingContext?.close()
        bindingContext = null
        remote = null
        _playerState.value = null
        _onDemand.value = null
        _artwork.value = null
        artUri = null
        _connection.value = ConnectionState()
    }

    fun stop() = onMain { started = false; clearRemote() }
}
