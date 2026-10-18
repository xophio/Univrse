package com.universe.remote.ui.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.theme.AppTheme

@Composable
fun PlayPauseIcon(paused: Boolean, color: Color, iconSize: Dp, modifier: Modifier = Modifier) =
    UniverseIcon(if (paused) UniverseGlyph.PLAY else UniverseGlyph.PAUSE, modifier, iconSize, color)

@Composable
fun SkipIcon(next: Boolean, color: Color, iconSize: Dp, modifier: Modifier = Modifier) =
    UniverseIcon(if (next) UniverseGlyph.NEXT else UniverseGlyph.PREVIOUS, modifier, iconSize, color)

@Composable
fun RepeatIcon(track: Boolean, color: Color, modifier: Modifier = Modifier) =
    UniverseIcon(UniverseGlyph.REPEAT, modifier, AppTheme.shuffleGlyph, color, repeatOne = track)

@Composable
fun DownChevronIcon(modifier: Modifier = Modifier) =
    UniverseIcon(UniverseGlyph.COLLAPSE, modifier)
