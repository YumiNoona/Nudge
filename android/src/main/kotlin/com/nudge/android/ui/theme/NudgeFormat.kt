package com.nudge.android.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Money formatting helpers.
 * Always formats from cent-based values, keeping paise when present.
 */

private const val PREFS_NAME = "nudge_prefs"

fun currencySymbolForCode(code: String?): String = when (code) {
    "USD" -> "$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY" -> "¥"
    "AUD" -> "A$"
    "CAD" -> "C$"
    "SGD" -> "S$"
    else -> "₹"
}

@Composable
@ReadOnlyComposable
fun currentCurrencySymbol(): String {
    val context = LocalContext.current
    val code = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString("currency_code", "INR")
    return currencySymbolForCode(code)
}

/** "₹1,234" or "₹1,234.56" — no trailing zeros. */
@Composable
@ReadOnlyComposable
fun formatCents(cents: Long): String {
    val symbol = currentCurrencySymbol()
    val value = cents / 100.0
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    fmt.maximumFractionDigits = 2
    fmt.minimumFractionDigits = if (cents % 100 == 0L) 0 else 2
    return symbol + fmt.format(value)
}

/** "₹1,234.56" — always two decimals. */
@Composable
@ReadOnlyComposable
fun formatCentsFull(cents: Long): String {
    val symbol = currentCurrencySymbol()
    val value = cents / 100.0
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    fmt.maximumFractionDigits = 2
    fmt.minimumFractionDigits = 2
    return symbol + fmt.format(value)
}

/** Plain number from cents, e.g. "1,234.5". */
@Composable
@ReadOnlyComposable
fun formatCentsPlain(cents: Long): String {
    val value = cents / 100.0
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    fmt.maximumFractionDigits = 2
    fmt.minimumFractionDigits = if (cents % 100 == 0L) 0 else 2
    return fmt.format(value)
}

/** Compact plain amount for constrained chart centers: 2.4K, 1.2L, 3.1Cr. */
fun formatCompactCentsPlain(cents: Long): String {
    val value = cents / 100.0
    val absolute = abs(value)
    val (scaled, suffix) = when {
        absolute >= 10_000_000 -> value / 10_000_000 to "Cr"
        absolute >= 100_000 -> value / 100_000 to "L"
        absolute >= 1_000 -> value / 1_000 to "K"
        else -> return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = if (cents % 100 == 0L) 0 else 2
        }.format(value)
    }
    // Keep useful precision for glanceable totals: 24.5K is materially clearer than 24K.
    // Very large three-digit compact values stay whole so constrained cards do not overflow.
    val decimals = when {
        scaled % 1.0 == 0.0 -> 0
        abs(scaled) < 100 -> 1
        else -> 0
    }
    return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
    }.format(scaled) + suffix
}
