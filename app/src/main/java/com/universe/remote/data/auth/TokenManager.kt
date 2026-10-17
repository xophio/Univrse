package com.universe.remote.data.auth

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.io.IOException

data class TokenResponse(
    @SerializedName("access_token") val access: String?,
    @SerializedName("refresh_token") val refresh: String?,
    @SerializedName("expires_in") val expires: Long,
    @SerializedName("token_type") val type: String?,
)
interface TokenApi {
    @FormUrlEncoded @POST("api/token")
    fun exchange(@Field("client_id") client: String, @Field("grant_type") grant: String,
        @Field("code") code: String? = null, @Field("redirect_uri") redirect: String? = null,
        @Field("code_verifier") verifier: String? = null, @Field("refresh_token") refresh: String? = null): Call<TokenResponse>
}

/** Synchronous methods run on IO/OkHttp threads. A single lock coalesces concurrent refreshes. */
class TokenManager(private val storage: AuthStorage, private val api: TokenApi,
    private val clientId: String, private val redirect: String, private val now: () -> Long = System::currentTimeMillis) {
    private val _loggedIn = MutableStateFlow(false)
    val loggedIn = _loggedIn.asStateFlow()
    @Synchronized fun restore() { _loggedIn.value = storage.read().tokens != null }
    @Synchronized fun begin(): PendingLogin {
        require(clientId.matches(Regex("[a-fA-F0-9]{32}"))) { "Configure your client ID in spotify.properties and rebuild." }
        val pending = PendingLogin(Pkce.random(), Pkce.random(), now())
        storage.write(storage.read().copy(pending = pending))
        return pending
    }
    @Synchronized fun cancel() { storage.write(storage.read().copy(pending = null)) }
    @Synchronized fun complete(code: String, state: String?) {
        val disk = storage.read()
        // Consume before exchanging: replayed callbacks cannot reuse this transaction.
        storage.write(disk.copy(pending = null))
        val pending = Pkce.validate(disk.pending, state, now())
        require(code.isNotBlank()) { "Spotify returned no authorization code." }
        val response = api.exchange(clientId, "authorization_code", code, redirect, pending.verifier).execute()
        if (!response.isSuccessful) { response.errorBody()?.close(); throw IOException("Spotify token exchange failed (${response.code()}). Please log in again.") }
        save(response.body(), null)
    }
    @Synchronized fun accessToken(rejected: String? = null): String {
        val current = storage.read().tokens ?: throw LoginRequired()
        // Another request already refreshed the token rejected by this request.
        if (rejected != null && rejected != current.access) return current.access
        if (rejected == null && current.expiresAt - now() > 60_000) return current.access
        val response = api.exchange(clientId, "refresh_token", refresh = current.refresh).execute()
        if (!response.isSuccessful) {
            val invalidGrant = response.code() == 400 && response.errorBody()?.string()?.contains("invalid_grant") == true
            response.errorBody()?.close()
            if (invalidGrant || response.code() == 401) { logout(); throw LoginRequired() }
            throw IOException("Spotify refresh unavailable (${response.code()}). Try again later.")
        }
        return save(response.body(), current.refresh).access
    }
    private fun save(body: TokenResponse?, previousRefresh: String?): Tokens {
        val access = body?.access?.takeIf { it.isNotBlank() } ?: throw IOException("Spotify returned no access token.")
        val refresh = body.refresh?.takeIf { it.isNotBlank() } ?: previousRefresh ?: throw IOException("Spotify returned no refresh token. Please log in again.")
        if (!body.type.equals("Bearer", true) || body.expires <= 0) throw IOException("Invalid Spotify token response.")
        val tokens = Tokens(access, refresh, now() + body.expires.coerceAtMost(86_400) * 1000)
        storage.write(AuthDisk(tokens = tokens))
        _loggedIn.value = true
        return tokens
    }
    @Synchronized fun logout() { storage.write(AuthDisk()); _loggedIn.value = false }
}
