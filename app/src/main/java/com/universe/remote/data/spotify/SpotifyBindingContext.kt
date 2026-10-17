package com.universe.remote.data.spotify

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build

/** Per-attempt adapter for App Remote 0.8.0's legacy bindService call.
 * Only Spotify's protocol service gets permission to show consent while our app is visible.
 * Closing also releases pending binds, for which the SDK exposes no cancellation handle.
 */
internal class SpotifyBindingContext(context: Context) : ContextWrapper(context) {
    private val connections = mutableSetOf<ServiceConnection>()
    private var closed = false

    // The SDK normalizes its input context before binding and unbinding.
    // Our base is already the application context, so no Activity is retained.
    override fun getApplicationContext(): Context = this

    @Synchronized
    override fun bindService(service: Intent, connection: ServiceConnection, flags: Int): Boolean {
        if (closed) return false
        val isSpotify = (service.`package` ?: service.component?.packageName) == "com.spotify.music" &&
            service.action == "com.spotify.mobile.appprotocol.action.BIND_PROTOCOL_SERVICE"
        val effectiveFlags = if (isSpotify && Build.VERSION.SDK_INT >= 34)
            flags or Context.BIND_ALLOW_ACTIVITY_STARTS else flags
        return super.bindService(service, connection, effectiveFlags).also {
            // Android requires unbinding even when bindService returns false.
            connections.add(connection)
        }
    }

    @Synchronized
    override fun unbindService(connection: ServiceConnection) {
        if (connections.remove(connection)) super.unbindService(connection)
    }

    @Synchronized
    fun close() {
        closed = true
        connections.forEach { connection ->
            try { super.unbindService(connection) } catch (_: IllegalArgumentException) {
                // Already removed by the framework following a failed bind.
            }
        }
        connections.clear()
    }
}
