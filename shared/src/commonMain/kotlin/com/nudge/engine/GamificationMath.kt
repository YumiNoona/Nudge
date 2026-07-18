package com.nudge.engine

import com.nudge.model.*
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure gamification math. All XP, level, and streak calculations.
 * No platform dependencies — works identically on Android and Web.
 */
object GamificationMath {

    /**
     * XP required for a given level: 100 * level^1.5
     */
    fun xpForLevel(level: Int): Long {
        return (100.0 * level.toDouble().pow(1.5)).roundToLong()
    }

    /**
     * Calculate level from total XP
     */
    fun levelFromXp(totalXp: Long): Int {
        var level = 1
        while (xpRequiredUpToLevel(level) <= totalXp) {
            level++
        }
        return level.coerceAtLeast(1)
    }

    /**
     * Total XP required to reach a given level (cumulative)
     */
    private fun xpRequiredUpToLevel(level: Int): Long {
        var total = 0L
        for (i in 1..level) {
            total += xpForLevel(i)
        }
        return total
    }

    /**
     * XP progress within current level (0.0 to 1.0)
     */
    fun levelProgress(totalXp: Long): Float {
        val level = levelFromXp(totalXp)
        val xpIntoLevel = totalXp - xpRequiredUpToLevel(level - 1)
        val xpNeeded = xpForLevel(level)
        return if (xpNeeded > 0) xpIntoLevel.toFloat() / xpNeeded.toFloat() else 1f
    }

    /**
     * Level title based on level tier
     */
    fun levelTitle(level: Int): String {
        return when {
            level <= 5 -> "Budget Rookie"
            level <= 10 -> "Coin Collector"
            level <= 18 -> "Saving Scout"
            level <= 28 -> "Spending Sensei"
            level <= 40 -> "Finance Ninja"
            level <= 55 -> "Wealth Wizard"
            level <= 75 -> "Money Mogul"
            else -> "Nudge Legend"
        }
    }

    // --- XP Rewards ---

    const val XP_REVIEW_TRANSACTION = 5L
    const val XP_MANUAL_ENTRY_SAME_DAY = 5L
    const val XP_UNDER_BUDGET_WEEKLY = 20L
    const val XP_SAVINGS_MILESTONE = 50L
    const val XP_DAILY_CHECKIN = 2L
    const val XP_CORRECT_CATEGORIZATION = 3L
    const val XP_CHALLENGE_COMPLETE = 100L

    // --- Streak Freeze ---

    const val STREAK_FREEZE_EARN_DAYS = 14 // earn 1 freeze per 14 days of consistent use
    const val MAX_STREAK_FREEZES = 2

    /**
     * Calculate the number of streak freezes a user should have based on consistent usage days
     */
    fun earnedStreakFreezes(consistentDays: Int): Int {
        val earned = consistentDays / STREAK_FREEZE_EARN_DAYS
        return earned.coerceAtMost(MAX_STREAK_FREEZES)
    }

    // --- Milestone Streak Days ---

    fun streakMilestone(days: Int): String? {
        return when (days) {
            7 -> "7-Day Streak: One Week Strong!"
            30 -> "30-Day Streak: A Full Month!"
            100 -> "100-Day Streak: Centurion!"
            365 -> "365-Day Streak: A Whole Year!"
            else -> null
        }
    }
}
