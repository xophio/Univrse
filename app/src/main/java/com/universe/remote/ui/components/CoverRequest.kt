package com.universe.remote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.request.ImageRequest

@Composable
fun coverRequest(url: String?, size: Dp): ImageRequest {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    return remember(context, url, pixels) {
        ImageRequest.Builder(context).data(url).size(pixels, pixels).build()
    }
}
