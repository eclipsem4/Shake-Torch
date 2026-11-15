package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeMode

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val backdropBase: Color,
    val auroraOrbPrimary: Color,
    val auroraOrbSecondary: Color,
    val auroraOrbTertiary: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderHighlight: Color,
    val glassBorderTrailing: Color,
    val glassInnerGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentCool: Color,
    val accentTorch: Color,
    val accentSuccess: Color,
    val accentWarning: Color,
    val accentDanger: Color,
    val navBarFill: Color
)

private val DarkGlassPalette = GlassPalette(
    isDark = true,
    backdropBase = Color(0xFF040711),
    auroraOrbPrimary = Color(0xFF1E40AF).copy(alpha = 0.36f),
    auroraOrbSecondary = Color(0xFF4F46E5).copy(alpha = 0.28f),
    auroraOrbTertiary = Color(0xFF0E7490).copy(alpha = 0.30f),
    glassFillTop = Color.White.copy(alpha = 0.15f),
    glassFillBottom = Color.White.copy(alpha = 0.05f),
    glassBorderHighlight = Color.White.copy(alpha = 0.52f),
    glassBorderTrailing = Color.White.copy(alpha = 0.12f),
    glassInnerGlow = Color(0xFF60A5FA).copy(alpha = 0.16f),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    accentCool = IceBlueAccent,
    accentTorch = TorchAmber,
    accentSuccess = StatusActiveGreen,
    accentWarning = StatusWarningAmber,
    accentDanger = StatusDangerCoral,
    navBarFill = Color(0xFF0B1429).copy(alpha = 0.62f)
)

private val LightGlassPalette = GlassPalette(
    isDark = false,
    backdropBase = Color(0xFFF4F8FF),
    auroraOrbPrimary = Color(0xFF93C5FD).copy(alpha = 0.52f),
    auroraOrbSecondary = Color(0xFFC4B5FD).copy(alpha = 0.40f),
    auroraOrbTertiary = Color(0xFF99F6E4).copy(alpha = 0.44f),
    glassFillTop = Color.White.copy(alpha = 0.82f),
    glassFillBottom = Color.White.copy(alpha = 0.50f),
    glassBorderHighlight = Color.White.copy(alpha = 0.96f),
    glassBorderTrailing = Color(0xFF94A3B8).copy(alpha = 0.28f),
    glassInnerGlow = Color(0xFF38BDF8).copy(alpha = 0.18f),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    accentCool = Color(0xFF0284C7),
    accentTorch = TorchWarmDeep,
    accentSuccess = Color(0xFF059669),
    accentWarning = Color(0xFFD97706),
    accentDanger = Color(0xFFE11D48),
    navBarFill = Color.White.copy(alpha = 0.78f)
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }

private val DarkColorScheme = darkColorScheme(
    primary = IceBlueAccent,
    onPrimary = Color(0xFF041326),
    secondary = TorchAmber,
    onSecondary = Color(0xFF281800),
    tertiary = StatusActiveGreen,
    background = Color(0xFF040711),
    onBackground = Color(0xFFF8FAFC),
    surface = MidnightElevated,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF16223F),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = StatusDangerCoral
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = TorchWarmDeep,
    onSecondary = Color.White,
    tertiary = Color(0xFF059669),
    background = Color(0xFFF4F8FF),
    onBackground = Color(0xFF0F172A),
    surface = GlacialElevated,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFE11D48)
)

private val ShakeLightShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ShakeLightTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = themeMode == AppThemeMode.DARK
    val target = if (useDark) DarkGlassPalette else LightGlassPalette
    val animSpec = tween<Color>(durationMillis = 360, easing = FastOutSlowInEasing)

    val backdropBase by animateColorAsState(target.backdropBase, animSpec, label = "backdrop")
    val orbPrimary by animateColorAsState(target.auroraOrbPrimary, animSpec, label = "orb1")
    val orbSecondary by animateColorAsState(target.auroraOrbSecondary, animSpec, label = "orb2")
    val orbTertiary by animateColorAsState(target.auroraOrbTertiary, animSpec, label = "orb3")
    val fillTop by animateColorAsState(target.glassFillTop, animSpec, label = "fillTop")
    val fillBottom by animateColorAsState(target.glassFillBottom, animSpec, label = "fillBottom")
    val borderHi by animateColorAsState(target.glassBorderHighlight, animSpec, label = "borderHi")
    val borderLo by animateColorAsState(target.glassBorderTrailing, animSpec, label = "borderLo")
    val textPrimary by animateColorAsState(target.textPrimary, animSpec, label = "textPrimary")
    val textSecondary by animateColorAsState(target.textSecondary, animSpec, label = "textSecondary")
    val navBarFill by animateColorAsState(target.navBarFill, animSpec, label = "navBarFill")

    val animatedPalette = target.copy(
        backdropBase = backdropBase,
        auroraOrbPrimary = orbPrimary,
        auroraOrbSecondary = orbSecondary,
        auroraOrbTertiary = orbTertiary,
        glassFillTop = fillTop,
        glassFillBottom = fillBottom,
        glassBorderHighlight = borderHi,
        glassBorderTrailing = borderLo,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        navBarFill = navBarFill
    )

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalGlassPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ShakeLightTypography,
            shapes = ShakeLightShapes,
            content = content
        )
    }
}
