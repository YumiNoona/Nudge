package com.nudge.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = NudgeColors.Emerald,
    onPrimary = Color.White,
    primaryContainer = NudgeColors.EmeraldBg,
    onPrimaryContainer = NudgeColors.EmeraldDeep,
    secondary = NudgeColors.EmeraldDeep,
    onSecondary = Color.White,
    background = NudgeColors.Bone,
    onBackground = NudgeColors.Ink,
    surface = NudgeColors.Surface,
    onSurface = NudgeColors.Ink,
    surfaceVariant = NudgeColors.SurfaceHover,
    onSurfaceVariant = NudgeColors.InkSoft,
    outline = NudgeColors.InkMute.copy(alpha = 0.3f),
    error = NudgeColors.Coral,
    onError = Color.White,
    errorContainer = NudgeColors.CoralBg,
    onErrorContainer = NudgeColors.Coral
)

private val DarkScheme = darkColorScheme(
    primary = NudgeColors.Emerald,
    onPrimary = Color.Black,
    primaryContainer = NudgeColors.EmeraldBg.copy(alpha = 0.2f),
    onPrimaryContainer = NudgeColors.Emerald,
    secondary = NudgeColors.EmeraldDeep,
    onSecondary = Color.Black,
    background = NudgeColors.Dark,
    onBackground = NudgeColors.InkDark,
    surface = NudgeColors.SurfaceDark,
    onSurface = NudgeColors.InkDark,
    surfaceVariant = NudgeColors.SurfaceDark.copy(alpha = 0.5f),
    onSurfaceVariant = NudgeColors.InkSoftDark,
    outline = NudgeColors.InkMuteDark.copy(alpha = 0.3f),
    error = NudgeColors.Coral,
    onError = Color.Black,
    errorContainer = NudgeColors.CoralBgDark,
    onErrorContainer = NudgeColors.Coral
)

@Composable
fun NudgeTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor) {
        // Material You slot — uses wallpaper-derived colors when available
        if (isDark) dynamicDarkColorScheme(LocalContext.current)
        else dynamicLightColorScheme(LocalContext.current)
    } else {
        if (isDark) DarkScheme else LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view)
                    .isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
