package com.nudge.engine

import com.nudge.model.*
import kotlinx.datetime.*

/**
 * Core budget math engine. Pure functions, no platform dependencies.
 * All monetary values in cents (Long) to avoid floating-point bugs.
 */
object BudgetMath {

    /**
     * Calculate total spending in a category for a given period
     */
    fun totalSpend(
        transactions: List<Transaction>,
        categoryId: String,
        start: Instant,
        end: Instant
    ): Long {
        return transactions
            .filter { it.categoryId == categoryId && it.type == TransactionType.DEBIT }
            .filter { it.timestamp >= start && it.timestamp <= end }
            .sumOf { it.amount }
    }

    /**
     * Calculate budget remaining (amount in cents)
     */
    fun remaining(budget: Budget, spent: Long): Long {
        return budget.amount - spent
    }

    /**
     * Calculate budget progress as a normalized float (0.0 to >1.0 if overspent)
     */
    fun progress(spent: Long, budgetAmount: Long): Float {
        if (budgetAmount == 0L) return 0f
        return spent.toFloat() / budgetAmount.toFloat()
    }

    /**
     * Predict when the budget will be exhausted at the current spending rate
     * Returns the estimated date, or null if spending is on track
     */
    fun predictExhaustionDate(
        spent: Long,
        budgetAmount: Long,
        daysIntoPeriod: Int,
        totalDaysInPeriod: Int
    ): Instant? {
        if (spent <= 0 || daysIntoPeriod <= 0) return null
        val dailyRate = spent.toDouble() / daysIntoPeriod
        if (dailyRate <= 0.0) return null
        val daysTillEmpty = (budgetAmount - spent).toDouble() / dailyRate
        if (daysTillEmpty < 0) return null // already overspent
        if (daysTillEmpty >= (totalDaysInPeriod - daysIntoPeriod)) return null // won't run out

        val now = Clock.System.now()
        return now.plus(daysTillEmpty.toLong(), DateTimeUnit.DAY, TimeZone.currentSystemDefault())
    }

    /**
     * Check if user is on track to stay under budget
     */
    fun isOnTrack(spent: Long, budgetAmount: Long, daysIntoPeriod: Int, totalDaysInPeriod: Int): Boolean {
        val expectedSpendRatio = daysIntoPeriod.toFloat() / totalDaysInPeriod.toFloat()
        val actualSpendRatio = if (budgetAmount == 0L) 0f else spent.toFloat() / budgetAmount.toFloat()
        return actualSpendRatio <= expectedSpendRatio
    }

    /**
     * Daily suggested spend to stay under budget for the rest of the period
     */
    fun dailySuggestedSpend(spent: Long, budgetAmount: Long, remainingDays: Int): Long {
        if (remainingDays <= 0) return budgetAmount - spent
        return (budgetAmount - spent) / remainingDays
    }

    /**
     * Calculate rollover amount for next period
     */
    fun rollover(spent: Long, budget: Budget): Long {
        if (!budget.rolloverEnabled) return 0L
        val remaining = budget.amount - spent
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Get the current period start and end dates for a budget
     */
    fun currentPeriod(budget: Budget): Pair<Instant, Instant> {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        return when (budget.period) {
            BudgetPeriod.WEEKLY -> {
                val start = today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
                val startInstant = start.atStartOfDayIn(TimeZone.currentSystemDefault())
                val endInstant = startInstant.plus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                Pair(startInstant, endInstant)
            }
            BudgetPeriod.MONTHLY -> {
                val start = LocalDate(today.year, today.monthNumber, 1)
                val end = LocalDate(today.year, today.monthNumber, start.daysInMonth)
                Pair(
                    start.atStartOfDayIn(TimeZone.currentSystemDefault()),
                    end.plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                )
            }
            BudgetPeriod.CUSTOM -> {
                // For custom, use start_date + 30 days as a sensible default period
                val end = budget.startDate.plus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                Pair(budget.startDate, end)
            }
        }
    }
}
