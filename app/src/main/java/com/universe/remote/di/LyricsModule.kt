package com.universe.remote.di

import com.universe.remote.data.local.LyricsDao
import com.universe.remote.data.lyrics.LrclibApi
import com.universe.remote.data.lyrics.LrclibLyricsRepository
import com.universe.remote.domain.LyricsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object LyricsModule {
    @Provides
    @Singleton
    fun lrclibApi(): LrclibApi {
        val client = OkHttpClient.Builder()
            .callTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .header("User-Agent", "Universe/0.1.0 (https://github.com/xophio/Univrse)")
                    .build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrclibApi::class.java)
    }

    @Provides
    @Singleton
    fun lyricsRepository(api: LrclibApi, dao: LyricsDao): LyricsRepository =
        LrclibLyricsRepository(api, dao)
}
