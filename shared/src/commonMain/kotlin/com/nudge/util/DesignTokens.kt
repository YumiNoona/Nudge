package com.nudge.util

/**
 * Semantic design tokens shared across platforms.
 * Platform implementations generate actual color/typography/spacing values.
 * NEVER hardcode raw colors/spacing — always reference these tokens.
 */
object DesignTokens {

    object Colors {
        // Semantic surface tokens
        const val SURFACE_BASE = "surface-base"
        const val SURFACE_RAISED = "surface-raised"
        const val SURFACE_OVERLAY = "surface-overlay"

        // Content
        const val CONTENT_PRIMARY = "content-primary"
        const val CONTENT_SECONDARY = "content-secondary"
        const val CONTENT_TERTIARY = "content-tertiary"

        // Brand accent — electric indigo (not generic fintech blue/green)
        const val ACCENT_PRIMARY = "accent-primary"
        const val ACCENT_SECONDARY = "accent-secondary"

        // Semantic colors — note: negative is warm coral, not alarming red
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
        const val WARNING = "warning"

        // Category palette prefix — 16 colors, color-blind safe
        const val CATEGORY_PREFIX = "category-"

        // Standard category color palette (color-blind safe, accessible)
        val CATEGORY_COLORS = listOf(
            "#6366F1", // indigo
            "#10B981", // emerald
            "#F59E0B", // amber
            "#EF4444", // red
            "#8B5CF6", // violet
            "#EC4899", // pink
            "#06B6D4", // cyan
            "#F97316", // orange
            "#84CC16", // lime
            "#14B8A6", // teal
            "#E11D48", // rose
            "#7C3AED", // purple
            "#0EA5E9", // sky
            "#D946EF", // fuchsia
            "#22D3EE", // cyan-light
            "#A855F7"  // purple-light
        )

        fun categoryColor(index: Int): String {
            return CATEGORY_COLORS[index % CATEGORY_COLORS.size]
        }
    }

    object Typography {
        // Type scale (size, line-height)
        data class TypeStyle(val size: Int, val lineHeight: Int)

        val DISPLAY = TypeStyle(36, 44)
        val TITLE = TypeStyle(24, 32)
        val HEADING = TypeStyle(18, 24)
        val BODY = TypeStyle(16, 22)
        val CAPTION = TypeStyle(13, 18)
        val MICRO = TypeStyle(11, 14)

        // Font family tokens
        const val FONT_DISPLAY = "CabinetGrotesk" // expressive, for large numbers/headlines
        const val FONT_BODY = "Inter" // legible, for body/data
    }

    object Spacing {
        // 4px base unit
        const val XS = 4
        const val SM = 8
        const val MD = 12
        const val BASE = 16
        const val LG = 24
        const val XL = 32
        const val XXL = 48
        const val XXXL = 64
    }

    object Radius {
        const val SM = 8
        const val MD = 14
        const val LG = 20
        const val XL = 28
        const val PILL = 9999 // effectively infinite for pill shapes
    }

    object Motion {
        // Standard durations in ms
        const val DURATION_QUICK = 150
        const val DURATION_STANDARD = 250
        const val DURATION_CELEBRATION = 500

        // Spring physics
        const val SPRING_DAMPING = 0.8f
        const val SPRING_STIFFNESS = 300f
    }

    object Haptic {
        // Semantic haptic event identifiers (Android maps these to real APIs)
        const val SELECTION = "selection"
        const val CONFIRM = "confirm"
        const val SUCCESS = "success"
        const val WARNING = "warning"
        const val ERROR = "error"
        const val IMPACT_LIGHT = "impact_light"
        const val IMPACT_MEDIUM = "impact_medium"
        const val IMPACT_HEAVY = "impact_heavy"
        const val CELEBRATION = "celebration"
    }
}
