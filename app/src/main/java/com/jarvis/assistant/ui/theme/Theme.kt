package com.jarvis.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

// ==========================================
// JARVIS WARM SUNSET & GLASSMORPHISM PALETTE
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

// Dark Theme Palette
val DarkBackground = Color(0xFF191219)
val DarkCardBackground = Color(0xFF241A23)
val DarkTextPrimary = Color(0xFFFFF0E9)
val DarkTextSecondary = Color(0xFFC0A6B7)
val DarkBorder = Color(0xFF4A3546)

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
    primary = SunsetCoral,
    onPrimary = Color.White,
    primaryContainer = SunsetPurple,
    onPrimaryContainer = Color.White,
    secondary = SunsetPurple,
    onSecondary = Color.White,
    secondaryContainer = DarkCardBackground,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = SunsetBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCardBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBackground,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.5f)
)

// Rounded friendly typography hierarchy
val JarvisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        color = SunsetTextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = SunsetTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = SunsetTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = SunsetTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        color = SunsetTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = SunsetTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = SunsetTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = SunsetTextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = SunsetCoral
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = SunsetTextSecondary
    )
)

// Card Design with 26dp - 32dp corner radii as specified
val JarvisShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun JarvisTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JarvisTypography,
        shapes = JarvisShapes,
        content = content
    )
}
