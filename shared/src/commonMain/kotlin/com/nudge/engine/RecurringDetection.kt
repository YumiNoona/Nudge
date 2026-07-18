package com.nudge.engine

import com.nudge.model.*
import kotlinx.datetime.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Detects recurring transactions (subscriptions, EMIs, rent, etc.)
 * from transaction history. Pure logic — no platform dependencies.
 */
object RecurringDetection {

    fun detectRecurring(
        transactions: List<Transaction>,
        existingRules: List<RecurringRule>
    ): List<DetectedRecurring> {
        val results = mutableListOf<DetectedRecurring>()

        val merchantGroups = transactions
            .filter { it.merchantNormalized != null }
            .groupBy { it.merchantNormalized!! }

        for ((merchant, txns) in merchantGroups) {
            if (txns.size < 2) continue

            val amountClusters = clusterByAmount(txns)
            for (cluster in amountClusters) {
                if (cluster.size < 2) continue

                val sorted = cluster.sortedBy { it.timestamp }
                val intervals = mutableListOf<Int>()
                for (i in 1 until sorted.size) {
                    val daysBetween = sorted[i].timestamp
                        .minus(sorted[i - 1].timestamp, DateTimeUnit.DAY, TimeZone.UTC)
                    intervals.add(daysBetween.toInt())
                }

                val detectedInterval = detectInterval(intervals)
                if (detectedInterval != null) {
                    val avgAmount = cluster.map { it.amount }.average().toLong()
                    val mostRecent = sorted.last()
                    val nextExpected = mostRecent.timestamp
                        .plus(detectedInterval.days, DateTimeUnit.DAY, TimeZone.UTC)

                    val alreadyKnown = existingRules.any {
                        it.merchantPattern == merchant &&
                        it.expectedAmountRange?.let { range ->
                            avgAmount in range.min..range.max
                        } != false
                    }

                    if (!alreadyKnown) {
                        results.add(
                            DetectedRecurring(
                                merchantNormalized = merchant,
                                avgAmount = avgAmount,
                                minAmount = cluster.minOf { it.amount },
                                maxAmount = cluster.maxOf { it.amount },
                                interval = detectedInterval,
                                lastDate = mostRecent.timestamp,
                                nextExpectedDate = nextExpected,
                                sampleTransactions = cluster.map { it.id },
                                confidence = calculateRecurringConfidence(intervals, cluster.size)
                            )
                        )
                    }
                }
            }
        }

        return results.sortedByDescending { it.confidence }
    }

    private fun clusterByAmount(transactions: List<Transaction>): List<List<Transaction>> {
        val sorted = transactions.sortedBy { it.amount }
        val clusters = mutableListOf<MutableList<Transaction>>()

        for (txn in sorted) {
            var added = false
            for (cluster in clusters) {
                val clusterAvg = cluster.map { it.amount }.average()
                val tolerance = clusterAvg * 0.10
                if (txn.amount.toDouble() in (clusterAvg - tolerance)..(clusterAvg + tolerance)) {
                    cluster.add(txn)
                    added = true
                    break
                }
            }
            if (!added) {
                clusters.add(mutableListOf(txn))
            }
        }
        return clusters
    }

    private fun detectInterval(daysBetween: List<Int>): RecurringInterval? {
        if (daysBetween.isEmpty()) return null
        val avg = daysBetween.average().toInt()

        return when {
            avg in 1..2 -> RecurringInterval.DAILY
            avg in 6..8 -> RecurringInterval.WEEKLY
            avg in 13..15 -> RecurringInterval.BIWEEKLY
            avg in 27..32 -> RecurringInterval.MONTHLY
            avg in 85..95 -> RecurringInterval.QUARTERLY
            avg in 350..380 -> RecurringInterval.YEARLY
            else -> null
        }
    }

    private fun calculateRecurringConfidence(intervals: List<Int>, count: Int): Float {
        if (intervals.size < 2 || count < 3) return 0.3f
        val avg = intervals.average()
        val variance = intervals.map { (it - avg).pow(2) }.average()
        val stdDev = sqrt(variance)
        val regularity = 1.0f - (stdDev / avg).toFloat().coerceIn(0f, 1f)
        val countBonus = (count / 10f).coerceAtMost(0.3f)
        return (0.5f + regularity * 0.4f + countBonus).coerceIn(0f, 1f)
    }
}

data class DetectedRecurring(
    val merchantNormalized: String,
    val avgAmount: Long,
    val minAmount: Long,
    val maxAmount: Long,
    val interval: RecurringInterval,
    val lastDate: Instant,
    val nextExpectedDate: Instant,
    val sampleTransactions: List<String>,
    val confidence: Float
)

internal val RecurringInterval.days: Int
    get() = when (this) {
        RecurringInterval.DAILY -> 1
        RecurringInterval.WEEKLY -> 7
        RecurringInterval.BIWEEKLY -> 14
        RecurringInterval.MONTHLY -> 30
        RecurringInterval.QUARTERLY -> 90
        RecurringInterval.YEARLY -> 365
    }
