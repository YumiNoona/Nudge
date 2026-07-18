package com.nudge.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Nudge theme with true OLED dark mode and semantic color tokens.
 *
 * Light theme: warm, tactile, confident
 * Dark theme: OLED-friendly near-black surfaces, NOT just dark gray
 * For Material You support (Android 12+): dynamic colors can be plugged in
 * via the dynamicColorScheme parameter.
 */
private val LightColorScheme = lightColorScheme(
    primary = NudgeColors.AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = NudgeColors.AccentPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = NudgeColors.AccentPrimary,
    secondary = NudgeColors.AccentSecondary,
    onSecondary = Color.White,
    background = NudgeColors.SurfaceBase,
    onBackground = NudgeColors.ContentPrimary,
    surface = NudgeColors.SurfaceRaised,
    onSurface = NudgeColors.ContentPrimary,
    surfaceVariant = NudgeColors.SurfaceBase,
    onSurfaceVariant = NudgeColors.ContentSecondary,
    error = NudgeColors.Negative,
    onError = Color.White,
    outline = NudgeColors.ContentTertiary.copy(alpha = 0.3f)
)

// True OLED dark — near-black surfaces, not just dark gray
private val OLEDDarkColorScheme = darkColorScheme(
    primary = NudgeColors.DarkAccentPrimary,
    onPrimary = Color.Black,
    primaryContainer = NudgeColors.AccentPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = NudgeColors.DarkAccentPrimary,
    secondary = NudgeColors.DarkAccentSecondary,
    onSecondary = Color.Black,
    background = NudgeColors.DarkSurfaceBase, // #0A0A0F — OLED-friendly
    onBackground = NudgeColors.DarkContentPrimary,
    surface = NudgeColors.DarkSurfaceRaised,  // #16161F
    onSurface = NudgeColors.DarkContentPrimary,
    surfaceVariant = NudgeColors.DarkSurfaceRaised.copy(alpha = 0.5f),
    onSurfaceVariant = NudgeColors.DarkContentSecondary,
    error = NudgeColors.Negative,
    onError = Color.Black,
    outline = NudgeColors.DarkContentTertiary.copy(alpha = 0.3f)
)

/**
 * Standard dark (slightly lighter than OLED — for users who prefer it)
 */
private val StandardDarkColorScheme = darkColorScheme(
    primary = NudgeColors.DarkAccentPrimary,
    onPrimary = Color.Black,
    primaryContainer = NudgeColors.AccentPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = NudgeColors.DarkAccentPrimary,
    secondary = NudgeColors.DarkAccentSecondary,
    onSecondary = Color.Black,
    background = Color(0xFF12121A),
    onBackground = NudgeColors.DarkContentPrimary,
    surface = Color(0xFF1E1E2A),
    onSurface = NudgeColors.DarkContentPrimary,
    surfaceVariant = Color(0xFF1E1E2A).copy(alpha = 0.5f),
    onSurfaceVariant = NudgeColors.DarkContentSecondary,
    error = NudgeColors.Negative,
    onError = Color.Black,
    outline = NudgeColors.DarkContentTertiary.copy(alpha = 0.3f)
)

/**
 * Theme wrapper that applies the correct color scheme and edge-to-edge bars.
 *
 * @param isDark Whether to use dark theme
 * @param useOLED True for OLED-friendly near-black surfaces (default)
 * @param dynamicColorScheme Optional Material You dynamic colors from wallpaper
 */
@Composable
fun NudgeTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    useOLED: Boolean = true,
    dynamicColorScheme: androidx.compose.material3.ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = dynamicColorScheme ?: when {
        isDark && useOLED -> OLEDDarkColorScheme
        isDark -> StandardDarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            WindowCompat.getInsetsController(
                view.context.window!!,  // Safe: only called when window exists
                view
            ).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
