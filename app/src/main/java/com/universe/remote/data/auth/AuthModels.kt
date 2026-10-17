package com.universe.remote.data.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.io.IOException

data class Tokens(val access: String, val refresh: String, val expiresAt: Long)
data class PendingLogin(val verifier: String, val state: String, val createdAt: Long)
data class AuthDisk(val tokens: Tokens? = null, val pending: PendingLogin? = null)
interface AuthStorage {
    fun read(): AuthDisk
    fun write(value: AuthDisk)
}
class LoginRequired : IOException("Please log in to Spotify Web API again.")
object Pkce {
    fun random(): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
    fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    fun validate(pending: PendingLogin?, state: String?, now: Long): PendingLogin {
        require(pending != null && state != null &&
            MessageDigest.isEqual(pending.state.toByteArray(), state.toByteArray()) &&
            now - pending.createdAt in 0..600_000) { "Login expired or could not be verified. Please try again." }
        return pending
    }
}
