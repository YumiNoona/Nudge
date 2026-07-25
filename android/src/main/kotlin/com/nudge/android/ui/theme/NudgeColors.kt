package com.nudge.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic design tokens for Nudge.
 * Accent: deep emerald green — confident, calm, never alarming.
 * Backgrounds: bone off-white (light), near-black neutral (dark).
 * Negative: warm coral — never alarm-red.
 */
object NudgeColors {
    // ── Primary accent (emerald) ──
    val Emerald = Color(0xFF1FAE6A)
    val EmeraldDeep = Color(0xFF158A54)
    val EmeraldBg = Color(0xFFE6F8EE)

    // ── Page backgrounds ──
    val Bone = Color(0xFFF6F5F2)        // off-white / warm bone
    val Dark = Color(0xFF0E0F13)        // neutral near-black (not tinted)

    // ── Surfaces ──
    val Surface = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1A1B1F)
    val SurfaceHover = Color(0xFFF0EFEC)

    // ── Ink (light theme) ──
    val Ink = Color(0xFF1E1B2E)
    val InkSoft = Color(0xFF6E6A85)
    val InkMute = Color(0xFFA6A2B8)

    // ── Ink (dark theme) ──
    val InkDark = Color(0xFFEBE9F5)
    val InkSoftDark = Color(0xFF9A96B5)
    val InkMuteDark = Color(0xFF5E5A78)

    // ── Semantic ──
    val Green = Emerald
    val GreenBg = EmeraldBg
    val GreenBgDark = Color(0xFF0D3320)

    val Coral = Color(0xFFF0574B)       // warm coral — never alarm-red
    val CoralBg = Color(0xFFFDECEA)
    val CoralBgDark = Color(0xFF331515)

    val Amber = Color(0xFFF5A524)
    val AmberBg = Color(0xFFFEF3E0)
    val AmberBgDark = Color(0xFF33280D)

    // ── Category chip palette (saturated icon + pastel bg) ──
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

    // ── Category icon palette (rotate per category) ──
    val CategoryColors = listOf(
        CatBlue, CatPink, CatTeal, CatOrange,
        CatViolet, CatRose, CatCyan, CatLime
    )

    // ── Backward-compatible aliases (old API → emerald values) ──

    val AccentPrimary get() = Emerald
    val AccentSecondary get() = EmeraldDeep
    val Positive get() = Green
    val Negative get() = Coral
    val Warning get() = Amber
    val SurfaceBase get() = Bone
    val SurfaceRaised get() = Surface
    val SurfaceOverlay get() = Color(0x80000000)
    val ContentPrimary get() = Ink
    val ContentSecondary get() = InkSoft
    val ContentTertiary get() = InkMute
    val DarkSurfaceBase get() = Dark
    val DarkSurfaceRaised get() = SurfaceDark
    val DarkContentPrimary get() = InkDark
    val DarkContentSecondary get() = InkSoftDark
    val DarkContentTertiary get() = InkMuteDark

    // Re-added colors
    val Purple = Color(0xFF8B5CF6)
    val PurpleDeep = Color(0xFF7C3AED)
    val DarkBg = Dark

    fun parse(colorString: String?, fallback: Color = InkMute): Color {
        if (colorString == null) return fallback
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            fallback
        }
    }
}
