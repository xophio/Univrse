package com.universe.remote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universe.remote.R

/** Single source of visual tokens. Components use Foundation, not Material components. */
object AppTheme {
    val background = Color(0xFF080908)
    val surface = Color(0xFF20221E)
    val border = Color(0xFF45483D)
    val lime = Color(0xFFDDF77F)
    val foreground = Color(0xFFF5F5EF)
    val muted = Color(0xFFA6AA9C)
    val error = Color(0xFFFFB5A1)
    val ink = Color(0xFF171D0B)
    val grain = Color(0xFFBBC5A2)
    val glass = Color(0xE6262926)
    val glassBorder = Color(0xFF53564E)
    val liquidGlass = Color(0xB8323431)
    val liquidGlassTop = Color(0x7AFFFFFF)
    val liquidGlassEdge = Color(0xA8E8ECE4)
    val liquidGlassShadow = Color(0xB8000000)
    val liquidGlassReflection = Color(0x503F7250)
    val navSurface = Color(0xEE222321)
    val navSolid = Color(0xFF222321)
    val navSelected = Color(0xFF41433C)
    val dock = Color(0xFF1F1F1D)
    val dockBorder = Color(0xFF3A3A36)
    val dockSegmentEmpty = Color(0xFF3A3A36)
    val playerDockGlassTint = Color(0xB6151714)
    val playerDockScrim = Color(0xB8080908)
    val shuffleSurface = Color(0xFF32352F)
    val oliveGlow = Color(0x663A4928)
    val sky = Color(0xFFB8E8F4)
    val blue = Color(0xFF7BA9FA)
    val blueDeep = Color(0xFF3669D2)
    val mint = Color(0xFFD5FBC0)
    val orbit = Color(0xFF41483B)
    val moonYellow = Color(0xFFE5F67A)
    val moonRed = Color(0xFFEB806F)
    val moonRedShadow = Color(0xFFA54D61)
    val moonPink = Color(0xFFFFB0A1)
    val blueShadow = Color(0xFF476995)
    val mintShadow = Color(0xFF8FB69B)
    val moonCrater = Color(0xFF6E833B)
    val moodColors = listOf(lime, blue, moonRed, moonYellow, mint, moonPink)
    val defaultMoodTags = listOf(
        "gym" to lime, "focus" to blue, "chill" to mint,
        "late night" to moonYellow, "sad" to moonPink, "hype" to moonRed,
    )
    val transparent = Color.Transparent
    val pixelFont = FontFamily(Font(R.font.silkscreen))
    val universeFont = FontFamily(Font(R.font.nt_brick_sans, weight = FontWeight.Normal))
    val universeTitle = TextStyle(fontFamily = universeFont, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, color = foreground)
    val bodyFont = FontFamily.SansSerif
    val title = TextStyle(fontFamily = pixelFont, fontSize = 30.sp, color = foreground)
    val headline = TextStyle(fontFamily = bodyFont, fontSize = 23.sp, color = foreground)
    val body = TextStyle(fontFamily = bodyFont, fontSize = 15.sp, lineHeight = 22.sp, color = foreground)
    val caption = TextStyle(fontFamily = bodyFont, fontSize = 12.sp, lineHeight = 18.sp, color = muted)
    val eyebrow = TextStyle(fontFamily = pixelFont, fontSize = 14.sp, color = lime)
    val nowTitle = TextStyle(fontFamily = bodyFont, fontSize = 25.sp, color = foreground)
    val time = TextStyle(fontFamily = pixelFont, fontSize = 10.sp, color = muted)
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 20.dp
    // Shared bezel inset for every screen and the floating player dock.
    val page = 12.dp
    val section = 32.dp
    val touch = 48.dp
    val cover = 72.dp
    val rowCover = 50.dp
    val rowMinHeight = 66.dp
    val rowAction = 40.dp
    val heartWidth = 24.dp
    val bellSize = 24.dp
    val shuffleSize = 20.dp
    val titlePixel = 3.5.dp
    val titleWidth = 165.dp
    val titleHeight = 34.dp
    // Reference: 145 px gap on a 754 px-wide crop, scaled to this phone.
    val titleBellGap = 65.dp
    val shuffleGlyph = 26.dp
    val navGlyph = 22.dp
    val heroHeight = 152.dp
    val heroPlanet = 84.dp
    val playlistPlanet = 60.dp
    val playlistPlanetCell = 4.dp
    val playlistItemWidth = 92.dp
    val playlistItemHeight = 112.dp
    val playlistBadge = 24.dp
    val playlistRing = 2.dp
    val carouselHeight = 90.dp
    val carouselWidth = 308.dp
    val carouselPeek = 40.dp
    val navHeight = 60.dp
    val navBottom = 18.dp
    val navInset = 108.dp
    val miniHeight = 70.dp
    val miniCover = 48.dp
    val miniGap = 12.dp
    val miniProgress = 2.dp
    val dockCover = 48.dp
    val dockPlayerHeight = 62.dp
    val dockNavHeight = 56.dp
    val dockDivider = 1.dp
    val dockProgressHeight = 6.dp
    val dockProgressGap = 3.dp
    val dockHorizontalPadding = 12.dp
    val dockVerticalPadding = 10.dp
    val dockBottom = 10.dp
    val dockBottomPaddingWithPlayer = 164.dp
    val dockBottomPaddingWithoutPlayer = 86.dp
    val nowCoverMax = 272.dp
    val nowControl = 48.dp
    val nowPlayControl = 64.dp
    val nowHeart = 32.dp
    val nowArtRadius = 18.dp
    val scrubberHeight = 32.dp
    val scrubberTrack = 4.dp
    val scrubberThumb = 7.dp
    val nowPlanet = 58.dp
    val lyricsSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val lyricsHandleWidth = 38.dp
    val lyricsHandleHeight = 4.dp
    val lyricsLineSpacing = 18.dp
    val lyricsCreditBottom = 10.dp
    val lyricsCurrent = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, color = lime)
    val lyricsLine = TextStyle(fontFamily = bodyFont, fontSize = 17.sp,
        lineHeight = 24.sp, color = muted)
    val tagSwatch = 24.dp
    val tagChipHeight = 38.dp
    val tagAddIcon = 15.dp
    val tagAddStroke = 1.dp
    val noteHeight = 72.dp
    val searchHeight = 48.dp
    val searchEdgePadding = 8.dp
    val searchHeaderHeight = 48.dp
    val searchHistoryHeaderHeight = 40.dp
    val searchEmptyHeight = 48.dp
    val searchSectionTopPadding = 8.dp
    val libraryHeaderHeight = 48.dp
    val libraryControlHeight = 40.dp
    val chartHeight = 130.dp
    val chartShortHeight = 86.dp
    val statCardHeight = 74.dp
    val nowDismissDistance = 90.dp
    val iconButton = 48.dp
    val avatar = 46.dp
    val menuWidth = 172.dp
    val menuOpenSlide = 6.dp
    val maxWidth = 520.dp
    val hairline = 1.dp
    val glassEdgeWidth = 1.dp
    val glassShadowElevation = 14.dp
    val glassBlurRadius = 44.dp
    val iconStroke = 1.7.dp
    val shuffleStroke = 2.2.dp
    val navStroke = 2.dp
    val starStroke = 1.5.dp
    val card = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(50)
    val artShape = RoundedCornerShape(8.dp)
    val navShape = RoundedCornerShape(34.dp)
    val dockShape = RoundedCornerShape(26.dp)
    val circle = CircleShape
    const val disabledAlpha = 0.4f
    const val grainAlpha = 0.035f
    const val homeGrainAlpha = 0.025f
    const val playerExpandMs = 420
    const val playerFadeMs = 180
    const val dockProgressSegments = 20
    const val dockSlideMs = 240
    const val searchOpenMs = 320
    const val searchCloseMs = 220
    const val searchFocusMs = 180
    const val searchRowMs = 260
    const val searchRowStaggerMs = 32L
    const val searchSweepMs = 900
    const val menuOpenMs = 190
    const val menuInitialScale = 0.84f
    const val lyricsPanelFraction = 0.68f
    const val lyricsSheetMs = 300
    const val lyricsLineTransitionMs = 360
    const val lyricsSyncLeadMs = 90L
    const val playerPositionTickMs = 80L
    val searchEntranceOffset = 14.dp
    const val coverToneBlend = 0.35f
}
