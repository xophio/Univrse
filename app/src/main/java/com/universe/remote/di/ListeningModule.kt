package com.universe.remote.di

import android.content.Context
import androidx.room.Room
import com.universe.remote.data.local.HistoryDao
import com.universe.remote.data.local.RoomListeningRepository
import com.universe.remote.data.local.TagDao
import com.universe.remote.data.local.StatsDao
import com.universe.remote.data.local.UniverseDatabase
import com.universe.remote.domain.ListeningRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): UniverseDatabase =
        Room.databaseBuilder(context, UniverseDatabase::class.java, "universe.db").build()
    @Provides fun history(db: UniverseDatabase): HistoryDao = db.historyDao()
    @Provides fun tags(db: UniverseDatabase): TagDao = db.tagDao()
    @Provides fun stats(db: UniverseDatabase): StatsDao = db.statsDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ListeningModule {
    @Binds @Singleton abstract fun repository(impl: RoomListeningRepository): ListeningRepository
}
