package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
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
    backdropBase = MidnightDeep,
    auroraOrbPrimary = SapphireGlow.copy(alpha = 0.55f),
    auroraOrbSecondary = IndigoAura.copy(alpha = 0.50f),
    auroraOrbTertiary = CyanTealAura.copy(alpha = 0.45f),
    glassFillTop = Color.White.copy(alpha = 0.14f),
    glassFillBottom = Color.White.copy(alpha = 0.06f),
    glassBorderHighlight = Color.White.copy(alpha = 0.42f),
    glassBorderTrailing = Color.White.copy(alpha = 0.10f),
    glassInnerGlow = Color(0xFF93C5FD).copy(alpha = 0.14f),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    accentCool = IceBlueAccent,
    accentTorch = TorchAmber,
    accentSuccess = StatusActiveGreen,
    accentWarning = StatusWarningAmber,
    accentDanger = StatusDangerCoral,
    navBarFill = Color(0xFF0B1429).copy(alpha = 0.68f)
)

private val LightGlassPalette = GlassPalette(
    isDark = false,
    backdropBase = GlacialMist,
    auroraOrbPrimary = SkyLightAura.copy(alpha = 0.85f),
    auroraOrbSecondary = LavenderLightAura.copy(alpha = 0.80f),
    auroraOrbTertiary = MintLightAura.copy(alpha = 0.80f),
    glassFillTop = Color.White.copy(alpha = 0.72f),
    glassFillBottom = Color.White.copy(alpha = 0.46f),
    glassBorderHighlight = Color.White.copy(alpha = 0.92f),
    glassBorderTrailing = Color(0xFF94A3B8).copy(alpha = 0.25f),
    glassInnerGlow = Color(0xFF60A5FA).copy(alpha = 0.16f),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    accentCool = Color(0xFF0284C7),
    accentTorch = TorchWarmDeep,
    accentSuccess = Color(0xFF059669),
    accentWarning = Color(0xFFD97706),
    accentDanger = Color(0xFFE11D48),
    navBarFill = Color.White.copy(alpha = 0.72f)
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }

private val DarkColorScheme = darkColorScheme(
    primary = IceBlueAccent,
    onPrimary = Color(0xFF041326),
    secondary = TorchAmber,
    onSecondary = Color(0xFF281800),
    tertiary = StatusActiveGreen,
    background = MidnightDeep,
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
    background = GlacialMist,
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
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val glassPalette = if (useDark) DarkGlassPalette else LightGlassPalette

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ShakeLightTypography,
            shapes = ShakeLightShapes,
            content = content
        )
    }
}
