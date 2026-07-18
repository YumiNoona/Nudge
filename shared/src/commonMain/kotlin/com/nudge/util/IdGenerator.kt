package com.nudge.util

import kotlin.random.Random

/**
 * Platform-agnostic UUID-like ID generator.
 * Uses Kotlin's Random for v4-like UUID generation.
 * On Android, can be replaced with java.util.UUID.
 * On JS, can be replaced with crypto.randomUUID().
 */
object IdGenerator {
    private val hexChars = "0123456789abcdef".toCharArray()

    fun generate(): String {
        val chars = CharArray(36)
        for (i in 0..35) {
            chars[i] = when (i) {
                8, 13, 18, 23 -> '-'
                14 -> '4'
                19 -> hexChars[Random.nextInt(4) or 8]
                else -> hexChars[Random.nextInt(16)]
            }
        }
        return chars.concatToString()
    }

    fun shortId(length: Int = 8): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
