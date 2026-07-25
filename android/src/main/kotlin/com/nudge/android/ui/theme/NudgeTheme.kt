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
    primary = NudgeColors.Purple,
    onPrimary = Color.White,
    primaryContainer = NudgeColors.Purple.copy(alpha = 0.15f),
    onPrimaryContainer = NudgeColors.Purple,
    secondary = NudgeColors.PurpleDeep,
    onSecondary = Color.White,
    background = NudgeColors.LavenderBg,
    onBackground = NudgeColors.Ink,
    surface = NudgeColors.Surface,
    onSurface = NudgeColors.Ink,
    surfaceVariant = NudgeColors.LavenderBg,
    onSurfaceVariant = NudgeColors.InkSoft,
    error = NudgeColors.Coral,
    onError = Color.White,
    outline = NudgeColors.InkMute.copy(alpha = 0.3f)
)

// True OLED dark — near-black surfaces, not just dark gray
private val OLEDDarkColorScheme = darkColorScheme(
    primary = NudgeColors.Purple,
    onPrimary = Color.Black,
    primaryContainer = NudgeColors.Purple.copy(alpha = 0.2f),
    onPrimaryContainer = NudgeColors.Purple,
    secondary = NudgeColors.PurpleDeep,
    onSecondary = Color.Black,
    background = NudgeColors.DarkBg,         // #0F0E17 — OLED-friendly
    onBackground = NudgeColors.InkDark,
    surface = NudgeColors.SurfaceDark,        // #1A1925
    onSurface = NudgeColors.InkDark,
    surfaceVariant = NudgeColors.SurfaceDark.copy(alpha = 0.5f),
    onSurfaceVariant = NudgeColors.InkSoftDark,
    error = NudgeColors.Coral,
    onError = Color.Black,
    outline = NudgeColors.InkMuteDark.copy(alpha = 0.3f)
)

/**
 * Standard dark (slightly lighter than OLED — for users who prefer it)
 */
private val StandardDarkColorScheme = darkColorScheme(
    primary = NudgeColors.Purple,
    onPrimary = Color.Black,
    primaryContainer = NudgeColors.Purple.copy(alpha = 0.2f),
    onPrimaryContainer = NudgeColors.Purple,
    secondary = NudgeColors.PurpleDeep,
    onSecondary = Color.Black,
    background = Color(0xFF12121A),
    onBackground = NudgeColors.InkDark,
    surface = Color(0xFF1E1E2A),
    onSurface = NudgeColors.InkDark,
    surfaceVariant = Color(0xFF1E1E2A).copy(alpha = 0.5f),
    onSurfaceVariant = NudgeColors.InkSoftDark,
    error = NudgeColors.Coral,
    onError = Color.Black,
    outline = NudgeColors.InkMuteDark.copy(alpha = 0.3f)
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
            val window = (view.context as? android.app.Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
