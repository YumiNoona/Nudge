package com.nudge.android.data

import java.security.MessageDigest

object CaptureIdentity {
    fun closestMessage(candidates: List<Pair<String, Long>>, receivedAt: Long): String? {
        val scored = candidates.filter { it.second > 0 }.groupBy { it.first }
            .mapValues { (_, rows) -> rows.minOf { kotlin.math.abs(it.second - receivedAt) } }
            .filterValues { it <= 120_000L }
        val closest = scored.values.minOrNull() ?: return null
        return scored.filterValues { it == closest }.keys.singleOrNull()
    }
    fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun fingerprint(source: String, sender: String, text: String, timestamp: Long): String = digest(
        listOf(CaptureDuplicateMatcher.family(source), sender.lowercase(), timestamp.toString(),
            text.trim().replace(Regex("\\s+"), " ").lowercase()).joinToString("\u0000"))

    fun referenceHash(text: String): String? {
        val ids = Regex("""(?i)\b(?:utr|rrn|(?:upi\s+)?ref(?:erence)?(?:\s*(?:no|id|number))?|(?:txn|transaction)\s*(?:id|no|number))\s*[:#.\-]?\s*([a-z0-9]{6,40})\b""")
            .findAll(text).map { it.groupValues[1].uppercase() }
            .filter { it.any(Char::isDigit) && !it.contains("XXXX") }.distinct().toList()
        return ids.singleOrNull()?.let(::digest)
    }
}
