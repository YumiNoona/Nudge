package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeRadius
import com.nudge.engine.GamificationMath
import com.nudge.engine.RecurringDetection
import com.nudge.model.RecurringInterval
import com.nudge.model.RecurringRule
import com.nudge.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Subscriptions & Recurring Transactions screen.
 * Shows:
 * 1. Total monthly recurring burn at top
 * 2. Upcoming charges timeline (next 5)
 * 3. Auto-detected recurring transactions list
 * 4. Option to add alerts/reminders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    recurringRules: List<RecurringRuleEntity>,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    // Detect recurring transactions from history
    val detectedRecurring = remember(transactions) {
        // Convert entities to shared model types for the detection engine
        val modelTxns = transactions.map { txn ->
            com.nudge.model.Transaction(
                id = txn.id,
                amount = txn.amountCents,
                type = when (txn.type) { "debit" -> TransactionType.DEBIT else -> TransactionType.CREDIT },
                merchantRaw = txn.merchantRaw,
                merchantNormalized = txn.merchantNormalized,
                categoryId = txn.categoryId,
                accountId = txn.accountId,
                source = com.nudge.model.TransactionSource.MANUAL,
                isRecurring = txn.isRecurring,
                recurringGroupId = txn.recurringGroupId,
                timestamp = kotlinx.datetime.Instant.fromEpochMilliseconds(txn.timestampEpoch),
                confidenceScore = 1f
            )
        }

        val modelRules = recurringRules.map { rule ->
            RecurringRule(
                id = rule.id,
                merchantPattern = rule.merchantPattern,
                expectedDayOfMonth = rule.expectedDay,
                interval = rule.interval?.let {
                    try { RecurringInterval.valueOf(it.uppercase()) } catch (_: Exception) { null }
                },
                categoryId = rule.categoryId
            )
        }

        RecurringDetection.detectRecurring(modelTxns, modelRules)
    }

    // Total monthly recurring spend
    val monthlyRecurringTotal = detectedRecurring
        .filter { it.interval == RecurringInterval.MONTHLY || it.interval == RecurringInterval.BIWEEKLY }
        .sumOf { if (it.interval == RecurringInterval.BIWEEKLY) it.avgAmount * 2 else it.avgAmount }

    // Upcoming charges (next 5 by date)
    val upcomingCharges = detectedRecurring
        .sortedBy { it.nextExpectedDate }
        .take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Nc.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Nc.inkSoft)
            }
            Text(
                "Subscriptions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Nc.ink
            )
            Spacer(modifier = Modifier.width(64.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total monthly recurring card
            item {
                Card(
                    shape = RoundedCornerShape(NudgeRadius.XL),
                    colors = CardDefaults.cardColors(
                        containerColor = Nc.accent.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Monthly Recurring",
                            fontSize = 13.sp,
                            color = Nc.inkSoft
                        )
                        Text(
                            "₹${NumberFormat.getNumberInstance(Locale.getDefault()).format(monthlyRecurringTotal / 100)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonoFamily,
                            color = Nc.accent
                        )
                        Text(
                            "per month",
                            fontSize = 12.sp,
                            color = Nc.inkMute
                        )
                    }
                }
            }

            // Upcoming charges timeline
            if (upcomingCharges.isNotEmpty()) {
                item {
                    Text(
                        "Upcoming Charges",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Nc.ink,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(upcomingCharges) { recurring ->
                    val merchant = categories.find {
                        it.id == recurringRules.find { r -> r.merchantPattern == recurring.merchantNormalized }?.categoryId
                    }

                    Card(
                        shape = RoundedCornerShape(NudgeRadius.MD),
                        colors = CardDefaults.cardColors(containerColor = Nc.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    recurring.merchantNormalized,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Nc.ink
                                )
                                Text(
                                    "Every ${recurring.interval.name.lowercase().replaceFirstChar { it.uppercase() }}" +
                                    " · Next: ${dateFormat.format(Date(recurring.nextExpectedDate.toEpochMilliseconds()))}",
                                    fontSize = 12.sp,
                                    color = Nc.inkMute
                                )
                                // Category hint
                                merchant?.let {
                                    Text(
                                        "${it.icon ?: "📁"} ${it.name}",
                                        fontSize = 11.sp,
                                        color = Nc.accent
                                    )
                                }
                            }
                            Text(
                                "₹${NumberFormat.getNumberInstance(Locale.getDefault()).format(recurring.avgAmount / 100)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = MonoFamily,
                                color = Nc.negative
                            )
                        }
                    }
                }
            }

            // All detected recurring
            if (detectedRecurring.isNotEmpty()) {
                item {
                    Text(
                        "All Detected Recurring",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Nc.ink,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Exclude already-shown upcoming ones
                val remaining = if (upcomingCharges.size < detectedRecurring.size) {
                    detectedRecurring.drop(upcomingCharges.size)
                } else emptyList()

                items(remaining) { recurring ->
                    Card(
                        shape = RoundedCornerShape(NudgeRadius.MD),
                        colors = CardDefaults.cardColors(
                            containerColor = Nc.surface.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    recurring.merchantNormalized,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Nc.ink
                                )
                                Text(
                                    "~${recurring.interval.name.lowercase().replaceFirstChar { it.uppercase() }}" +
                                    " · ${(recurring.confidence * 100).toInt()}% confidence",
                                    fontSize = 11.sp,
                                    color = Nc.inkMute
                                )
                            }
                            Text(
                                "₹${NumberFormat.getNumberInstance(Locale.getDefault()).format(recurring.avgAmount / 100)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = MonoFamily,
                                color = Nc.inkSoft
                            )
                        }
                    }
                }
            }

            // Empty state
            if (detectedRecurring.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No recurring transactions detected yet",
                                fontSize = 14.sp,
                                color = Nc.inkSoft
                            )
                            Text(
                                "Keep tracking — patterns will emerge over time",
                                fontSize = 12.sp,
                                color = Nc.inkMute
                            )
                        }
                    }
                }
            }
        }
    }
}

// Shared radius values
