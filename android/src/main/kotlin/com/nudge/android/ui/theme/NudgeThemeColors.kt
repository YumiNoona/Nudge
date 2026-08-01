package com.nudge.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware semantic color bridge.
 * Routes every semantic color name through MaterialTheme.colorScheme
 * so dark/light mode works automatically everywhere.
 *
 * Usage: Nc.surface, Nc.ink, Nc.inkSoft, etc.
 * These read from the current MaterialTheme, so they react to theme changes.
 */

object Nc {
    // Surfaces
    val surface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
    val background: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
    val surfaceVariant: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    // Ink
    val ink: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val inkSoft: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val inkMute: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline

    // Primary / accent (emerald)
    val accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val accentDeep: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
    val accentBg: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer

    // Semantic
    val positive: Color @Composable @ReadOnlyComposable get() = NudgeColors.Emerald  // static — same in both modes
    val negative: Color @Composable @ReadOnlyComposable get() = NudgeColors.Coral
    val warning: Color @Composable @ReadOnlyComposable get() = NudgeColors.Amber
    val amberBg: Color @Composable @ReadOnlyComposable get() = NudgeColors.AmberBg
    val coralBg: Color @Composable @ReadOnlyComposable get() = NudgeColors.CoralBg
    val greenBg: Color @Composable @ReadOnlyComposable get() = NudgeColors.EmeraldBg
    val accentBgLight: Color @Composable @ReadOnlyComposable get() = NudgeColors.EmeraldBg

    // Category chip palette (always static — they're decorative)
    val catColors get() = NudgeColors.CategoryColors
    fun catColor(index: Int): Color = NudgeColors.CategoryColors[index % NudgeColors.CategoryColors.size]
}
