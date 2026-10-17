package com.universe.remote.ui.weblogin

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotify.sdk.android.auth.AuthorizationClient
import com.universe.remote.ui.components.*
import com.universe.remote.ui.theme.AppTheme

@Composable
fun WebLoginScreen(activity: Activity, viewModel: WebLoginViewModel) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.result(AuthorizationClient.getResponse(result.resultCode, result.data), result.resultCode == Activity.RESULT_CANCELED)
    }
    GrainBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(AppTheme.page),
            verticalArrangement = Arrangement.spacedBy(AppTheme.large)) {
            Label("Universe", AppTheme.title)
            Spacer(Modifier.height(AppTheme.section))
            Label("Your music universe", AppTheme.headline)
            Label("Connect your Spotify library to see your playlists and liked songs.", AppTheme.body)
            Pill(if (state.busy) "Please wait…" else "Log in with Spotify", primary = true,
                enabled = !state.busy, onClick = { viewModel.login { request ->
                    launcher.launch(AuthorizationClient.createLoginActivityIntent(activity, request))
                } })
            state.message?.let { Label(it, AppTheme.caption.copy(color = AppTheme.error)) }
        }
    }
}
