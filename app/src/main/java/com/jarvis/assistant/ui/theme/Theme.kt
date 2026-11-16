package com.jarvis.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.ThemePreset

// ==========================================
// THEME 1: WARM SUNSET & GLASSMORPHISM
// ==========================================
val SunsetBackground = Color(0xFFFFF0E9)
val SunsetBackgroundSecondary = Color(0xFFFCEAE5)
val SunsetCardBackground = Color(0xFFFFF9F6)
val SunsetTextPrimary = Color(0xFF32182B)
val SunsetTextSecondary = Color(0xFF98758D)
val SunsetCoral = Color(0xFFF16D43)
val SunsetPurple = Color(0xFFA66C91)
val SunsetBlue = Color(0xFF8295AD)
val SunsetGreen = Color(0xFF28AE86)
val SunsetBorder = Color(0xFFD9C7C4)

// Gradient stops: Peach -> Cream -> Soft Pink
val SunsetPeach = Color(0xFFFED6C4)
val SunsetCream = Color(0xFFFFF0E9)
val SunsetPink = Color(0xFFFAD2E1)
val SunsetDarkPurple = Color(0xFF261221)

// Gradient Brushes
val SunsetGradientBrush = Brush.linearGradient(
    colors = listOf(SunsetPeach, SunsetCream, SunsetPink)
)

val SunsetCoralPurpleBrush = Brush.horizontalGradient(
    colors = listOf(SunsetCoral, SunsetPurple)
)

val SunsetOrbGlowBrush = Brush.radialGradient(
    colors = listOf(SunsetCoral.copy(alpha = 0.45f), SunsetPurple.copy(alpha = 0.25f), Color.Transparent)
)

val GlassCardBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.65f),
        SunsetBorder.copy(alpha = 0.4f),
        SunsetCoral.copy(alpha = 0.2f)
    )
)

// ==========================================
// THEME 2: MIDNIGHT COMMAND (EXECUTIVE CYBER)
// ==========================================
val MidnightBackground = Color(0xFF090D16)
val MidnightBackgroundSecondary = Color(0xFF0F1524)
val MidnightCardBackground = Color(0xFF131B2E)
val MidnightTextPrimary = Color(0xFFE2E8F0)
val MidnightTextSecondary = Color(0xFF94A3B8)
val MidnightElectricBlue = Color(0xFF00E5FF)
val MidnightNeonViolet = Color(0xFF7C4DFF)
val MidnightEmerald = Color(0xFF00E676)
val MidnightBorder = Color(0xFF1E293B)
val MidnightSurfaceElevated = Color(0xFF1A243D)

val MidnightGradientBrush = Brush.linearGradient(
    colors = listOf(MidnightBackground, MidnightBackgroundSecondary, Color(0xFF0D1426))
)

val MidnightCyanVioletBrush = Brush.horizontalGradient(
    colors = listOf(MidnightElectricBlue, MidnightNeonViolet)
)

val MidnightBorderBrush = Brush.linearGradient(
    colors = listOf(
        MidnightElectricBlue.copy(alpha = 0.5f),
        MidnightBorder,
        MidnightNeonViolet.copy(alpha = 0.3f)
    )
)

// Dark Theme Fallback Palette
val DarkBackground = Color(0xFF191219)
val DarkCardBackground = Color(0xFF241A23)
val DarkTextPrimary = Color(0xFFFFF0E9)
val DarkTextSecondary = Color(0xFFC0A6B7)
val DarkBorder = Color(0xFF4A3546)

private val SunsetColorScheme = lightColorScheme(
    primary = SunsetCoral,
    onPrimary = Color.White,
    primaryContainer = SunsetPeach,
    onPrimaryContainer = SunsetTextPrimary,
    secondary = SunsetPurple,
    onSecondary = Color.White,
    secondaryContainer = SunsetBackgroundSecondary,
    onSecondaryContainer = SunsetTextPrimary,
    tertiary = SunsetBlue,
    onTertiary = Color.White,
    background = SunsetBackground,
    onBackground = SunsetTextPrimary,
    surface = SunsetCardBackground,
    onSurface = SunsetTextPrimary,
    surfaceVariant = SunsetBackgroundSecondary,
    onSurfaceVariant = SunsetTextSecondary,
    outline = SunsetBorder,
    outlineVariant = SunsetBorder.copy(alpha = 0.5f)
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightElectricBlue,
    onPrimary = Color(0xFF050B14),
    primaryContainer = MidnightSurfaceElevated,
    onPrimaryContainer = MidnightTextPrimary,
    secondary = MidnightNeonViolet,
    onSecondary = Color.White,
    secondaryContainer = MidnightCardBackground,
    onSecondaryContainer = MidnightTextPrimary,
    tertiary = MidnightEmerald,
    onTertiary = Color(0xFF050B14),
    background = MidnightBackground,
    onBackground = MidnightTextPrimary,
    surface = MidnightCardBackground,
    onSurface = MidnightTextPrimary,
    surfaceVariant = MidnightBackgroundSecondary,
    onSurfaceVariant = MidnightTextSecondary,
    outline = MidnightBorder,
    outlineVariant = MidnightBorder.copy(alpha = 0.6f)
)

val JarvisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

val JarvisShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun JarvisTheme(
    preset: ThemePreset = ThemePreset.MIDNIGHT_COMMAND,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (preset) {
        ThemePreset.MIDNIGHT_COMMAND, ThemePreset.DARK, ThemePreset.AMOLED, ThemePreset.CYBERPUNK -> MidnightColorScheme
        else -> if (darkTheme) MidnightColorScheme else SunsetColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JarvisTypography,
        shapes = JarvisShapes,
        content = content
    )
}
