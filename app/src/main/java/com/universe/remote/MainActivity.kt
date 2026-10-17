package com.universe.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.universe.remote.data.spotify.SpotifyConnectionManager
import com.universe.remote.ui.connection.ConnectionScreen
import com.universe.remote.ui.connection.ConnectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.universe.remote.ui.weblogin.WebLoginScreen
import com.universe.remote.ui.weblogin.WebLoginViewModel
import com.universe.remote.ui.home.HomeScreen
import com.universe.remote.ui.home.HomeViewModel
import com.universe.remote.ui.nowplaying.NowPlayingViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connection: SpotifyConnectionManager
    private val viewModel: ConnectionViewModel by viewModels()
    private val webViewModel: WebLoginViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val nowPlayingViewModel: NowPlayingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "home") {
                composable("home") {
                    val loggedIn by webViewModel.loggedIn.collectAsStateWithLifecycle()
                    if (loggedIn) HomeScreen(homeViewModel, nowPlayingViewModel,
                        openRemote = { navController.navigate("connection") }, logout = webViewModel::logout)
                    else WebLoginScreen(this@MainActivity, webViewModel)
                }
                composable("connection") { ConnectionScreen(viewModel) }
            }
        }
    }
    override fun onStart() { super.onStart(); connection.start() }
    override fun onStop() { connection.stop(); super.onStop() }
}
