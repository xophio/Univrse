package com.universe.remote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.universe.remote.data.local.PlayLoggingCoordinator
import javax.inject.Inject

@HiltAndroidApp
class UniverseApplication : Application() {
    @Inject lateinit var playLogger: PlayLoggingCoordinator
    override fun onCreate() {
        super.onCreate()
        playLogger.start()
    }
}
