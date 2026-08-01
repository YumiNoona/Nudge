package com.nudge.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.R

/**
 * Complete design system for Nudge — single source of truth.
 * Every screen pulls from here. No hardcoded values elsewhere.
 */

// ── Colors ──

object DS {
    // Accent — deep emerald, warm and confident
    val Accent = Color(0xFF365244)
    val AccentDeep = Color(0xFF1D3027)
    val AccentSoft = Color(0xFFE7EFEA)
    val AccentTint = Color(0xFFD9E8DF)
    val Signal = Color(0xFFDEFF4A)

    // Surfaces — warm bone, NOT cold lavender
    val Surface0 = Color(0xFFF4F5F2)  // page background (warm bone)
    val Surface1 = Color(0xFFFFFFFF)  // cards, sheets
    val Surface2 = Color(0xFFF1EFEA)  // recessed inputs
    val SurfaceOverlay = Color(0x80000000)

    // Ink — warm, readable
    val InkPrimary = Color(0xFF111411)
    val InkSecondary = Color(0xFF626862)
    val InkTertiary = Color(0xFF9BA19B)
    val InkInverse = Color(0xFFFFFFFF)

    // Semantic
    val Positive = Color(0xFF1E9E62)    // same as accent — income, under-budget
    val Negative = Color(0xFFE5624F)    // warm coral — expenses, never alarming
    val Warning = Color(0xFFF5A524)     // amber
    val WarningBg = Color(0xFFFFF3E0)

    // Category chip palette (saturated icon + pastel bg)
    val ChipBlue = Color(0xFF5B8DEF); val ChipBlueBg = Color(0xFFE8EFFD)
    val ChipPink = Color(0xFFEF5DA8); val ChipPinkBg = Color(0xFFFDE9F3)
    val ChipTeal = Color(0xFF20C6B0); val ChipTealBg = Color(0xFFE3FAF6)
    val ChipOrange = Color(0xFFF59E4B); val ChipOrangeBg = Color(0xFFFFF1E3)
    val ChipViolet = Color(0xFF5D826C); val ChipVioletBg = Color(0xFFE7F0EA)
    val ChipRose = Color(0xFFF43F5E); val ChipRoseBg = Color(0xFFFFE4E8)
    val ChipCyan = Color(0xFF06B6D4); val ChipCyanBg = Color(0xFFE0F9FD)
    val ChipLime = Color(0xFF84CC16); val ChipLimeBg = Color(0xFFF4FDE4)

    // Dark theme
    val DarkSurface0 = Color(0xFF101012)  // near-black neutral
    val DarkSurface1 = Color(0xFF1C1C1F)  // cards
    val DarkSurface2 = Color(0xFF262628)  // recessed
    val DarkInkPrimary = Color(0xFFEBEAF0)
    val DarkInkSecondary = Color(0xFF9A98A8)
    val DarkInkTertiary = Color(0xFF5C5A68)
    val DarkAccent = Color(0xFFB7D7C3)
    val DarkAccentSoft = Color(0xFF223229)
    val DarkWarningBg = Color(0xFF332610)

    // Category colors rotation
    val ChipColors = listOf(ChipBlue, ChipPink, ChipTeal, ChipOrange, ChipViolet, ChipRose, ChipCyan, ChipLime)

    // Gradients
    val AccentGradient = Brush.linearGradient(listOf(Accent, AccentDeep))
}

// ── Typography ──

val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_bold, FontWeight.ExtraBold)
)

val DSTypography = Typography(
    displayLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 29.sp, letterSpacing = (-0.35).sp),
    headlineMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    bodyLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
)

// ── Spacing ──

object DSSpace {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val base = 16.dp
    val lg = 24.dp; val xl = 32.dp; val xxl = 48.dp; val xxxl = 64.dp
}

// ── Radius ──

object DSRadius {
    val sm = 8.dp; val md = 14.dp; val lg = 20.dp; val xl = 28.dp
    val card = 24.dp; val sheet = 28.dp; val pill = 50.dp
}

// ── Shadow specs ──

object DSShadow {
    data class Spec(val blur: Dp, val opacity: Float, val color: Color)
    val sm = Spec(4.dp, 0.04f, Color(0xFF000000))
    val md = Spec(10.dp, 0.08f, Color(0xFF000000))
    val lg = Spec(20.dp, 0.12f, Color(0xFF000000))
    val accent = Spec(16.dp, 0.30f, Color(0xFF1E9E62))
    val accentLg = Spec(24.dp, 0.40f, Color(0xFF1E9E62))
}

// ── Theme bridge (matches Nc but uses DS values) ──
// Reads from MaterialTheme.colorScheme so it always follows the app's
// dark/light toggle (not the OS setting).

object DSBridge {
    private val isDark: Boolean @Composable @ReadOnlyComposable get() = LocalNudgeIsDark.current
    @Composable @ReadOnlyComposable fun surface(): Color = if (isDark) DS.DarkSurface1 else DS.Surface1
    @Composable @ReadOnlyComposable fun background(): Color = if (isDark) DS.DarkSurface0 else DS.Surface0
    @Composable @ReadOnlyComposable fun ink(): Color = if (isDark) DS.DarkInkPrimary else DS.InkPrimary
    @Composable @ReadOnlyComposable fun inkSoft(): Color = if (isDark) DS.DarkInkSecondary else DS.InkSecondary
    @Composable @ReadOnlyComposable fun inkMute(): Color = if (isDark) DS.DarkInkTertiary else DS.InkTertiary
    @Composable @ReadOnlyComposable fun accent(): Color = if (isDark) DS.DarkAccent else DS.Accent
    @Composable @ReadOnlyComposable fun accentBg(): Color = if (isDark) DS.DarkAccentSoft else DS.AccentSoft
    @Composable @ReadOnlyComposable fun positive(): Color = DS.Positive
    @Composable @ReadOnlyComposable fun negative(): Color = DS.Negative
    @Composable @ReadOnlyComposable fun warning(): Color = DS.Warning
    @Composable @ReadOnlyComposable fun warningBg(): Color = if (isDark) DS.DarkWarningBg else DS.WarningBg
    @Composable @ReadOnlyComposable fun surfaceVariant(): Color = if (isDark) DS.DarkSurface2 else DS.Surface2
    @Composable fun chipColor(i: Int): Color = DS.ChipColors[i % DS.ChipColors.size]
}
