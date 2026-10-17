package com.universe.remote.data.auth

import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WebAuthInterceptorTest {
    @Test fun `401 refreshes and retries only once even when replacement is rejected`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"access_token":"new","refresh_token":"new-refresh","token_type":"Bearer","expires_in":3600}"""))
            val store = object : AuthStorage {
                var value = AuthDisk(tokens = Tokens("old", "refresh", Long.MAX_VALUE))
                override fun read() = value
                override fun write(value: AuthDisk) { this.value = value }
            }
            val tokens = TokenManager(store, Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build().create(TokenApi::class.java), "client", "redirect")
            val seen = mutableListOf<String?>()
            val client = OkHttpClient.Builder().addInterceptor(WebAuthInterceptor(tokens)).addInterceptor { chain ->
                seen.add(chain.request().header("Authorization"))
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(401)
                    .message("Unauthorized").body("{}".toResponseBody()).build()
            }.build()
            client.newCall(Request.Builder().url("https://api.spotify.com/v1/me/playlists").build()).execute().use {
                assertEquals(401, it.code)
            }
            assertEquals(listOf("Bearer old", "Bearer new"), seen)
            assertEquals(1, server.requestCount)
            assertEquals("new-refresh", store.read().tokens?.refresh)
            assertThrows(java.io.IOException::class.java) {
                client.newCall(Request.Builder().url("https://example.com/").build()).execute()
            }
            assertEquals(2, seen.size)
        }
    }
}
