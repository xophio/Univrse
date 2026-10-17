package com.universe.remote.data.auth

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors

class TokenManagerTest {
    private class MemoryStore(var disk: AuthDisk) : AuthStorage {
        override fun read() = disk
        override fun write(value: AuthDisk) { disk = value }
    }
    @Test fun `PKCE matches RFC7636 test vector and rejects mismatched expired state`() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        val pending = PendingLogin("verifier", "expected", 100)
        assertEquals(pending, Pkce.validate(pending, "expected", 200))
        assertThrows(IllegalArgumentException::class.java) { Pkce.validate(pending, "wrong", 200) }
        assertThrows(IllegalArgumentException::class.java) { Pkce.validate(pending, "expected", 700_000) }
        assertThrows(IllegalArgumentException::class.java) { Pkce.validate(null, "expected", 200) }
    }
    @Test fun `concurrent expired requests refresh once and retain omitted refresh token`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"access_token":"new","token_type":"Bearer","expires_in":3600}"""))
            val store = MemoryStore(AuthDisk(tokens = Tokens("old", "refresh", 0)))
            val manager = manager(server, store)
            val pool = Executors.newFixedThreadPool(8)
            try {
                val values = pool.invokeAll(List(8) { java.util.concurrent.Callable { manager.accessToken() } }).map { it.get() }
                assertTrue(values.all { it == "new" })
                assertEquals(1, server.requestCount)
                assertEquals("refresh", store.disk.tokens?.refresh)
                assertTrue(server.takeRequest().body.readUtf8().contains("grant_type=refresh_token"))
                assertEquals("new", manager.accessToken(rejected = "old"))
                assertEquals(1, server.requestCount)
            } finally { pool.shutdownNow() }
        }
    }
    @Test fun `invalid grant clears credentials but server failure preserves session`() {
        MockWebServer().use { server ->
            val store = MemoryStore(AuthDisk(tokens = Tokens("old", "refresh", 0)))
            val manager = manager(server, store)
            manager.restore()
            server.enqueue(MockResponse().setResponseCode(503))
            assertThrows(java.io.IOException::class.java) { manager.accessToken() }
            assertNotNull(store.disk.tokens)
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))
            assertThrows(LoginRequired::class.java) { manager.accessToken() }
            assertNull(store.disk.tokens)
            assertFalse(manager.loggedIn.value)
        }
    }
    @Test fun `code exchange sends verifier consumes state and prevents replay`() {
        MockWebServer().use { server ->
            val pending = PendingLogin("secret-verifier", "expected", 900)
            val store = MemoryStore(AuthDisk(pending = pending))
            val manager = manager(server, store)
            server.enqueue(MockResponse().setBody("""{"access_token":"access","refresh_token":"rotated","token_type":"Bearer","expires_in":3600}"""))
            manager.complete("auth-code", "expected")
            val body = server.takeRequest().body.readUtf8()
            assertTrue(body.contains("code_verifier=secret-verifier"))
            assertTrue(body.contains("grant_type=authorization_code"))
            assertFalse(body.contains("client_secret"))
            assertNull(store.disk.pending)
            assertThrows(IllegalArgumentException::class.java) { manager.complete("auth-code", "expected") }
            assertEquals(1, server.requestCount)
        }
    }
    private fun manager(server: MockWebServer, store: AuthStorage) = TokenManager(store,
        Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create())
            .build().create(TokenApi::class.java), "client", "universe-remote://callback") { 1000L }
}
