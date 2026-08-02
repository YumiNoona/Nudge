package com.nudge.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = DS.Accent,
    onPrimary = Color.White,
    primaryContainer = DS.AccentSoft,
    onPrimaryContainer = DS.AccentDeep,
    secondary = DS.AccentDeep,
    onSecondary = Color.White,
    background = DS.Surface0,
    onBackground = DS.InkPrimary,
    surface = DS.Surface1,
    onSurface = DS.InkPrimary,
    surfaceVariant = DS.Surface2,
    onSurfaceVariant = DS.InkSecondary,
    outline = DS.InkTertiary,
    error = DS.Negative,
    onError = Color.White,
    errorContainer = DS.WarningBg,
    onErrorContainer = DS.Negative
)

private val DarkScheme = darkColorScheme(
    primary = DS.DarkAccent,
    onPrimary = Color.Black,
    primaryContainer = DS.DarkAccentSoft,
    onPrimaryContainer = DS.DarkAccent,
    secondary = DS.DarkAccent,
    onSecondary = Color.Black,
    background = DS.DarkSurface0,
    onBackground = DS.DarkInkPrimary,
    surface = DS.DarkSurface1,
    onSurface = DS.DarkInkPrimary,
    surfaceVariant = DS.DarkSurface2,
    onSurfaceVariant = DS.DarkInkSecondary,
    outline = DS.DarkInkTertiary,
    error = DS.Negative,
    onError = Color.Black,
    errorContainer = DS.DarkWarningBg,
    onErrorContainer = DS.Negative
)

// App-wide dark-mode state — lets DSBridge pick the right tokens
// regardless of the OS theme setting.
val LocalNudgeIsDark = staticCompositionLocalOf { false }

@Composable
fun NudgeTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (isDark) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalNudgeIsDark provides isDark) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DSTypography,
            shapes = Shapes(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}
