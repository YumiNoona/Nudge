package com.nudge.android.data

object SourceMessagePolicy {
    fun shouldSaveBody(enabled: Boolean, sourceType: String): Boolean =
        enabled && sourceType != "manual"

    fun retentionCutoff(now: Long, days: Int?): Long? =
        days?.takeIf { it > 0 }?.let { now - it * 24L * 60L * 60L * 1000L }
}
