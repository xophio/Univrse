package com.universe.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import com.universe.remote.ui.theme.AppTheme

@Composable
fun Label(text: String, style: TextStyle = AppTheme.body, modifier: Modifier = Modifier) {
    BasicText(text, modifier = modifier, style = style)
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.alpha(if (enabled) 1f else AppTheme.disabledAlpha)
        .heightIn(min = AppTheme.touch).clip(AppTheme.pill)
        .background(if (primary) AppTheme.lime else AppTheme.surface)
        .border(AppTheme.hairline, if (primary) Color.Transparent else AppTheme.border, AppTheme.pill)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = AppTheme.large, vertical = AppTheme.medium), contentAlignment = Alignment.Center) {
        Label(text, AppTheme.body.copy(color = if (primary) AppTheme.ink else AppTheme.foreground))
    }
}
