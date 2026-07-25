package com.nudge.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic design token color implementation for Android/Compose.
 * Purple gradient as accent — confident, not generic fintech blue.
 * Negative is warm coral — not alarming red.
 */
object NudgeColors {
    // Primary gradient pair
    val Purple = Color(0xFF7C6FF0)
    val PurpleDeep = Color(0xFF5B4FD1)

    // Page backgrounds
    val LavenderBg = Color(0xFFF3F2FA)
    val DarkBg = Color(0xFF0F0E17)

    // Surfaces
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1A1925)
    val SurfaceHover = Color(0xFFF8F7FF)

    // Ink (light theme)
    val Ink = Color(0xFF1E1B2E)
    val InkSoft = Color(0xFF6E6A85)
    val InkMute = Color(0xFFA6A2B8)

    // Ink (dark theme)
    val InkDark = Color(0xFFEBE9F5)
    val InkSoftDark = Color(0xFF9A96B5)
    val InkMuteDark = Color(0xFF5E5A78)

    // Semantic
    val Green = Color(0xFF1FAE6A)
    val GreenBg = Color(0xFFE6F8EE)
    val GreenBgDark = Color(0xFF0D3320)

    val Coral = Color(0xFFF0574B)
    val CoralBg = Color(0xFFFDECEA)
    val CoralBgDark = Color(0xFF331515)

    val Amber = Color(0xFFF5A524)
    val AmberBg = Color(0xFFFEF3E0)
    val AmberBgDark = Color(0xFF33280D)

    // Category chip palette (saturated icon + pastel background)
    val CatBlue = Color(0xFF5B8DEF)
    val CatBlueBg = Color(0xFFE8EFFD)
    val CatPink = Color(0xFFEF5DA8)
    val CatPinkBg = Color(0xFFFDE9F3)
    val CatTeal = Color(0xFF20C6B0)
    val CatTealBg = Color(0xFFE3FAF6)
    val CatOrange = Color(0xFFF59E4B)
    val CatOrangeBg = Color(0xFFFFF1E3)
    val CatViolet = Color(0xFF8B5CF6)
    val CatVioletBg = Color(0xFFF0EBFF)
    val CatRose = Color(0xFFF43F5E)
    val CatRoseBg = Color(0xFFFFE4E8)
    val CatCyan = Color(0xFF06B6D4)
    val CatCyanBg = Color(0xFFE0F9FD)
    val CatLime = Color(0xFF84CC16)
    val CatLimeBg = Color(0xFFF4FDE4)

    // Purple shadows (tinted, soft)
    val PurpleShadow = Color(0x595B4FD1)
    val PurpleShadowHeavy = Color(0x735B4FD1)

    // Category icon palette — the 8 saturated chip foreground colors
    val CategoryColors = listOf(
        CatBlue, CatPink, CatTeal, CatOrange,
        CatViolet, CatRose, CatCyan, CatLime
    )

    // ── Backward-compatible aliases (old API → new values) ──
    // These keep the hundreds of existing screen references working.

    @Deprecated("Use LavenderBg", ReplaceWith("LavenderBg"))
    val SurfaceBase get() = LavenderBg

    @Deprecated("Use Surface", ReplaceWith("Surface"))
    val SurfaceRaised get() = Surface

    @Deprecated("Use Surface.copy(alpha=0.5f)", ReplaceWith("Surface.copy(alpha = 0.5f)"))
    val SurfaceOverlay get() = Color(0x80000000)

    @Deprecated("Use Ink", ReplaceWith("Ink"))
    val ContentPrimary get() = Ink

    @Deprecated("Use InkSoft", ReplaceWith("InkSoft"))
    val ContentSecondary get() = InkSoft

    @Deprecated("Use InkMute", ReplaceWith("InkMute"))
    val ContentTertiary get() = InkMute

    @Deprecated("Use Purple", ReplaceWith("Purple"))
    val AccentPrimary get() = Purple

    @Deprecated("Use PurpleDeep", ReplaceWith("PurpleDeep"))
    val AccentSecondary get() = PurpleDeep

    @Deprecated("Use Green", ReplaceWith("Green"))
    val Positive get() = Green

    @Deprecated("Use Coral", ReplaceWith("Coral"))
    val Negative get() = Coral

    @Deprecated("Use Amber", ReplaceWith("Amber"))
    val Warning get() = Amber

    @Deprecated("Use DarkBg", ReplaceWith("DarkBg"))
    val DarkSurfaceBase get() = DarkBg

    @Deprecated("Use SurfaceDark", ReplaceWith("SurfaceDark"))
    val DarkSurfaceRaised get() = SurfaceDark

    @Deprecated("Use InkDark", ReplaceWith("InkDark"))
    val DarkContentPrimary get() = InkDark

    @Deprecated("Use InkSoftDark", ReplaceWith("InkSoftDark"))
    val DarkContentSecondary get() = InkSoftDark

    @Deprecated("Use InkMuteDark", ReplaceWith("InkMuteDark"))
    val DarkContentTertiary get() = InkMuteDark
}

data class NudgeColorScheme(
    val surfaceBase: Color,
    val surfaceRaised: Color,
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
    surfaceBase = NudgeColors.LavenderBg,
    surfaceRaised = NudgeColors.Surface,
    contentPrimary = NudgeColors.Ink,
    contentSecondary = NudgeColors.InkSoft,
    contentTertiary = NudgeColors.InkMute,
    accentPrimary = NudgeColors.Purple,
    accentSecondary = NudgeColors.PurpleDeep,
    positive = NudgeColors.Green,
    negative = NudgeColors.Coral,
    warning = NudgeColors.Amber
)

fun darkColorScheme() = NudgeColorScheme(
    surfaceBase = NudgeColors.DarkBg,
    surfaceRaised = NudgeColors.SurfaceDark,
    contentPrimary = NudgeColors.InkDark,
    contentSecondary = NudgeColors.InkSoftDark,
    contentTertiary = NudgeColors.InkMuteDark,
    accentPrimary = NudgeColors.Purple,
    accentSecondary = NudgeColors.PurpleDeep,
    positive = NudgeColors.Green,
    negative = NudgeColors.Coral,
    warning = NudgeColors.Amber
)
