package com.universe.remote.di

import com.universe.remote.BuildConfig
import com.universe.remote.data.auth.*
import com.universe.remote.data.web.*
import com.universe.remote.domain.SpotifyLibraryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object WebApiModule {
    @Provides @Singleton fun tokens(store: KeystoreAuthStorage): TokenManager {
        // Separate client: refresh cannot recursively invoke the bearer interceptor.
        val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).build()
        val api = Retrofit.Builder().baseUrl("https://accounts.spotify.com/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(TokenApi::class.java)
        return TokenManager(store, api, BuildConfig.SPOTIFY_CLIENT_ID, BuildConfig.SPOTIFY_REDIRECT_URI)
    }
    @Provides @Singleton fun api(tokens: TokenManager): SpotifyWebApi {
        val client = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).addInterceptor(WebAuthInterceptor(tokens)).build()
        return Retrofit.Builder().baseUrl("https://api.spotify.com/v1/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(SpotifyWebApi::class.java)
    }
    @Provides @Singleton fun repository(api: SpotifyWebApi): SpotifyLibraryRepository = WebLibraryRepository(api)
}
