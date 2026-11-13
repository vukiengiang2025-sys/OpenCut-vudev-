package com.example.opencut.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opencut.R

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val RobotoMonoFontFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono, FontWeight.Medium),
    Font(R.font.roboto_mono, FontWeight.SemiBold)
)

@Immutable
data class OpenCutColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,
    val popover: Color,
    val popoverForeground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val accentForeground: Color,
    val destructive: Color,
    val border: Color,
    val input: Color,
    val ring: Color,
    val sidebar: Color,
    val sidebarForeground: Color,
    val sidebarPrimary: Color,
    val sidebarBorder: Color,
    val trackMain: Color,
    val trackOverlay: Color,
    val trackAudio: Color,
    val playhead: Color,
    val isDark: Boolean
)

private val LightOpenCutColors = OpenCutColors(
    background = Color(0xFFFFFFFF),
    foreground = Color(0xFF0A0A0A),
    card = Color(0xFFFFFFFF),
    cardForeground = Color(0xFF0A0A0A),
    popover = Color(0xFFFFFFFF),
    popoverForeground = Color(0xFF0A0A0A),
    primary = Color(0xFF171717),
    primaryForeground = Color(0xFFFAFAFA),
    secondary = Color(0xFFF5F5F5),
    secondaryForeground = Color(0xFF171717),
    muted = Color(0xFFF5F5F5),
    mutedForeground = Color(0xFF737373),
    accent = Color(0xFFF5F5F5),
    accentForeground = Color(0xFF171717),
    destructive = Color(0xFFDC2626),
    border = Color(0xFFE5E5E5),
    input = Color(0xFFE5E5E5),
    ring = Color(0xFFA3A3A3),
    sidebar = Color(0xFFFAFAFA),
    sidebarForeground = Color(0xFF0A0A0A),
    sidebarPrimary = Color(0xFF3B82F6),
    sidebarBorder = Color(0xFFE5E5E5),
    trackMain = Color(0xFF2563EB),
    trackOverlay = Color(0xFF7C3AED),
    trackAudio = Color(0xFF059669),
    playhead = Color(0xFFEF4444),
    isDark = false
)

private val DarkOpenCutColors = OpenCutColors(
    background = Color(0xFF0A0A0A),
    foreground = Color(0xFFFAFAFA),
    card = Color(0xFF171717),
    cardForeground = Color(0xFFFAFAFA),
    popover = Color(0xFF171717),
    popoverForeground = Color(0xFFFAFAFA),
    primary = Color(0xFFEDEDED),
    primaryForeground = Color(0xFF171717),
    secondary = Color(0xFF262626),
    secondaryForeground = Color(0xFFFAFAFA),
    muted = Color(0xFF262626),
    mutedForeground = Color(0xFFA1A1A1),
    accent = Color(0xFF262626),
    accentForeground = Color(0xFFFAFAFA),
    destructive = Color(0xFFEF4444),
    border = Color(0x1AFFFFFF),
    input = Color(0x26FFFFFF),
    ring = Color(0xFF737373),
    sidebar = Color(0xFF141414),
    sidebarForeground = Color(0xFFFAFAFA),
    sidebarPrimary = Color(0xFF5B6CFA),
    sidebarBorder = Color(0x1AFFFFFF),
    trackMain = Color(0xFF3B82F6),
    trackOverlay = Color(0xFF8B5CF6),
    trackAudio = Color(0xFF10B981),
    playhead = Color(0xFFF43F5E),
    isDark = true
)

val LocalOpenCutColors = staticCompositionLocalOf { DarkOpenCutColors }

private val LightMaterialColors = lightColorScheme(
    primary = LightOpenCutColors.primary,
    onPrimary = LightOpenCutColors.primaryForeground,
    secondary = LightOpenCutColors.secondary,
    onSecondary = LightOpenCutColors.secondaryForeground,
    tertiary = LightOpenCutColors.sidebarPrimary,
    background = LightOpenCutColors.background,
    onBackground = LightOpenCutColors.foreground,
    surface = LightOpenCutColors.card,
    onSurface = LightOpenCutColors.cardForeground,
    surfaceVariant = LightOpenCutColors.muted,
    onSurfaceVariant = LightOpenCutColors.mutedForeground,
    outline = LightOpenCutColors.border,
    outlineVariant = LightOpenCutColors.border,
    error = LightOpenCutColors.destructive
)

private val DarkMaterialColors = darkColorScheme(
    primary = DarkOpenCutColors.primary,
    onPrimary = DarkOpenCutColors.primaryForeground,
    secondary = DarkOpenCutColors.secondary,
    onSecondary = DarkOpenCutColors.secondaryForeground,
    tertiary = DarkOpenCutColors.sidebarPrimary,
    background = DarkOpenCutColors.background,
    onBackground = DarkOpenCutColors.foreground,
    surface = DarkOpenCutColors.card,
    onSurface = DarkOpenCutColors.cardForeground,
    surfaceVariant = DarkOpenCutColors.muted,
    onSurfaceVariant = DarkOpenCutColors.mutedForeground,
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF262626),
    error = DarkOpenCutColors.destructive
)

val OpenCutTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val OpenCutShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

object OpenCutTheme {
    val colors: OpenCutColors
        @Composable get() = LocalOpenCutColors.current
}

@Composable
fun OpenCutTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val openCutColors = if (darkTheme) DarkOpenCutColors else LightOpenCutColors
    val materialColors = if (darkTheme) DarkMaterialColors else LightMaterialColors

    CompositionLocalProvider(LocalOpenCutColors provides openCutColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = OpenCutTypography,
            shapes = OpenCutShapes,
            content = content
        )
    }
}
