package com.universe.remote.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun motionEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var enabled by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (true) {
                    enabled = Settings.Global.getFloat(context.contentResolver,
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
                    delay(500)
                }
            } finally { enabled = false }
        }
    }
    return enabled
}

@Composable
fun Modifier.firstAppearance(key: String): Modifier {
    val enabled = motionEnabled()
    var seen by androidx.compose.runtime.saveable.rememberSaveable(key) { mutableStateOf(false) }
    val progress = remember(key) { androidx.compose.animation.core.Animatable(if (seen) 1f else 0f) }
    LaunchedEffect(enabled, key) {
        if (!seen && enabled) progress.animateTo(1f, androidx.compose.animation.core.tween(250))
        else progress.snapTo(1f)
        seen = true
    }
    return this.then(Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12f
    })
}


