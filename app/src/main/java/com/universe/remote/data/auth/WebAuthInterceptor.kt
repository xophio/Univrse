package com.universe.remote.data.auth

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class WebAuthInterceptor(private val tokens: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != "api.spotify.com" || !request.url.isHttps)
            throw IOException("Refusing to send Spotify credentials to an unexpected host.")
        val access = tokens.accessToken()
        val response = chain.proceed(request.newBuilder().header("Authorization", "Bearer $access").build())
        if (response.code != 401) return response
        response.close()
        val refreshed = tokens.accessToken(rejected = access)
        // One retry only. No unbounded authentication loop.
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $refreshed").build())
    }
}
