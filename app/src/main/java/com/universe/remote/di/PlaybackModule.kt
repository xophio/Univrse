package com.universe.remote.di

import com.universe.remote.data.spotify.SpotifyPlayerController
import com.universe.remote.domain.PlayerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds @Singleton abstract fun player(implementation: SpotifyPlayerController): PlayerController
}
