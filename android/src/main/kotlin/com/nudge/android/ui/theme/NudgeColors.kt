package com.nudge.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic design token color implementation for Android/Compose.
 * Electric indigo as accent — not generic fintech blue.
 * Negative is warm coral — not alarming red.
 */
object NudgeColors {
    // Light theme
    val SurfaceBase = Color(0xFFFAFAFA)
    val SurfaceRaised = Color(0xFFFFFFFF)
    val SurfaceOverlay = Color(0x80000000)

    val ContentPrimary = Color(0xFF1A1A2E)
    val ContentSecondary = Color(0xFF6B7280)
    val ContentTertiary = Color(0xFF9CA3AF)

    val AccentPrimary = Color(0xFF6366F1)    // electric indigo
    val AccentSecondary = Color(0xFF818CF8)

    // Note: negative is warm coral, NOT alarming red
    val Positive = Color(0xFF10B981)         // emerald green
    val Negative = Color(0xFFF472B6)         // warm coral pink
    val Warning = Color(0xFFF59E0B)          // amber

    // Dark theme (OLED-friendly near-black surfaces)
    val DarkSurfaceBase = Color(0xFF0A0A0F)
    val DarkSurfaceRaised = Color(0xFF16161F)
    val DarkSurfaceOverlay = Color(0x80FFFFFF)

    val DarkContentPrimary = Color(0xFFF1F5F9)
    val DarkContentSecondary = Color(0xFF94A3B8)
    val DarkContentTertiary = Color(0xFF64748B)

    // Accent same in both modes
    val DarkAccentPrimary = Color(0xFF818CF8)
    val DarkAccentSecondary = Color(0xFFA5B4FC)

    // Category palette
    val CategoryColors = listOf(
        Color(0xFF6366F1), // indigo
        Color(0xFF10B981), // emerald
        Color(0xFFF59E0B), // amber
        Color(0xFFEF4444), // red
        Color(0xFF8B5CF6), // violet
        Color(0xFFEC4899), // pink
        Color(0xFF06B6D4), // cyan
        Color(0xFFF97316), // orange
        Color(0xFF84CC16), // lime
        Color(0xFF14B8A6), // teal
        Color(0xFFE11D48), // rose
        Color(0xFF7C3AED), // purple
        Color(0xFF0EA5E9), // sky
        Color(0xFFD946EF), // fuchsia
        Color(0xFF22D3EE), // cyan-light
        Color(0xFFA855F7)  // purple-light
    )
}

data class NudgeColorScheme(
    val surfaceBase: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color
)

fun lightColorScheme() = NudgeColorScheme(
    surfaceBase = NudgeColors.SurfaceBase,
    surfaceRaised = NudgeColors.SurfaceRaised,
    surfaceOverlay = NudgeColors.SurfaceOverlay,
    contentPrimary = NudgeColors.ContentPrimary,
    contentSecondary = NudgeColors.ContentSecondary,
    contentTertiary = NudgeColors.ContentTertiary,
    accentPrimary = NudgeColors.AccentPrimary,
    accentSecondary = NudgeColors.AccentSecondary,
    positive = NudgeColors.Positive,
    negative = NudgeColors.Negative,
    warning = NudgeColors.Warning
)

fun darkColorScheme() = NudgeColorScheme(
    surfaceBase = NudgeColors.DarkSurfaceBase,
    surfaceRaised = NudgeColors.DarkSurfaceRaised,
    surfaceOverlay = NudgeColors.DarkSurfaceOverlay,
    contentPrimary = NudgeColors.DarkContentPrimary,
    contentSecondary = NudgeColors.DarkContentSecondary,
    contentTertiary = NudgeColors.DarkContentTertiary,
    accentPrimary = NudgeColors.DarkAccentPrimary,
    accentSecondary = NudgeColors.DarkAccentSecondary,
    positive = NudgeColors.Positive,
    negative = NudgeColors.Negative,
    warning = NudgeColors.Warning
)
