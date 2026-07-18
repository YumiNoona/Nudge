package com.nudge.engine

import com.nudge.model.*
import kotlinx.datetime.*

/**
 * Deduplication engine to prevent double-counting when both
 * SMS and app notification fire for the same transaction.
 */
object DeduplicationEngine {

    data class DedupKey(
        val amount: Long,
        val timestamp: Instant,
        val senderId: String
    )

    /**
     * Generate a stable hash key for deduplication.
     * Two transactions within the same 60-second window,
     * same amount, and same sender are considered duplicates.
     */
    fun generateKey(
        amount: Long,
        timestamp: Instant,
        senderId: String
    ): String {
        // Round timestamp to the nearest 60-second window
        val epochSeconds = timestamp.epochSeconds
        val windowSeconds = epochSeconds - (epochSeconds % 60)

        return "dedup:${amount}:${windowSeconds}:${senderId.hashCode()}"
    }

    /**
     * Check if a new parsed transaction duplicates an existing one
     */
    fun isDuplicate(
        new: ParsedTransaction,
        existing: List<Transaction>,
        senderId: String,
        timestamp: Instant
    ): Boolean {
        val newKey = generateKey(new.amount, timestamp, senderId)
        val existingKeys = existing.map {
            generateKey(it.amount, it.timestamp, it.source.name + (it.merchantRaw))
        }
        return existingKeys.any { it == newKey }
    }
}
