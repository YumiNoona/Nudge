package com.nudge.engine

import com.nudge.model.*

/**
 * Core engine for categorizing transactions.
 * Implemented in KMP commonMain with platform-specific storage adapters.
 */
interface CategorizationEngine {
    /**
     * Try to auto-categorize a transaction based on merchant name
     * Returns the best-matching category ID or null if uncertain
     */
    fun autoCategorize(merchantNormalized: String, amount: Long): CategorizationResult

    /**
     * Learn from a user's manual correction — maps merchant to category
     * for future auto-categorization on this device only
     */
    fun learn(merchantNormalized: String, categoryId: String)

    /**
     * Get the confidence score for a merchant-category mapping
     */
    fun getConfidence(merchantNormalized: String, categoryId: String): Float

    /**
     * Get all learned merchant-category mappings (for export/backup)
     */
    fun getLearnedMappings(): Map<String, String>

    /**
     * Import learned mappings (from backup/sync)
     */
    fun importMappings(mappings: Map<String, String>)
}

data class CategorizationResult(
    val categoryId: String?,
    val confidence: Float,
    val source: CategorizationSource
)

enum class CategorizationSource {
    BUILT_IN_RULE,
    USER_LEARNED,
    HEURISTIC_FALLBACK
}
