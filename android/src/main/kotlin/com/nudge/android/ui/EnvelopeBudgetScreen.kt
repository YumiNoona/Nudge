package com.nudge.android.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.android.ui.theme.NudgeRadius
import java.text.NumberFormat
import java.util.Locale

/**
 * Envelope-style budgeting mode — optional, toggleable per §5.
 *
 * Assigns spending limits ("envelopes") to categories.
 * Shows fill-level visual for each envelope.
 * Supports rollover of unused amounts to next period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeBudgetScreen(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    onAddBudget: () -> Unit,
    onEditBudget: (BudgetEntity) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NudgeColors.SurfaceBase)
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
                Text("← Back", color = NudgeColors.ContentSecondary)
            }
            Text(
                "Envelopes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NudgeColors.ContentPrimary
            )
            TextButton(onClick = onAddBudget) {
                Text("+ Envelope", color = NudgeColors.AccentPrimary)
            }
        }

        // Explanation card
        Card(
            shape = RoundedCornerShape(NudgeRadius.LG),
            colors = CardDefaults.cardColors(
                containerColor = NudgeColors.AccentPrimary.copy(alpha = 0.05f)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "How Envelope Budgeting Works",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NudgeColors.ContentPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Put money in envelopes for each category. When an envelope is empty, you stop spending in that category. Unused money can roll over to next month.",
                    fontSize = 12.sp,
                    color = NudgeColors.ContentSecondary
                )
            }
        }

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✉️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No envelopes yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.ContentPrimary
                    )
                    Text(
                        "Create envelopes to track spending by category",
                        fontSize = 13.sp,
                        color = NudgeColors.ContentSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onAddBudget,
                        shape = RoundedCornerShape(NudgeRadius.MD)
                    ) {
                        Text("Create First Envelope")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(budgets) { index, budget ->
                    val category = categories.find { it.id == budget.categoryId }
                    val spent = transactions
                        .filter { it.categoryId == budget.categoryId && it.type == "debit" }
                        .sumOf { it.amountCents }

                    val remaining = budget.amountCents - spent
                    val progress = (spent.toFloat() / budget.amountCents.toFloat()).coerceIn(0f, 1.5f)
                    val dailyLeft = maxOf(0, remaining) / 100
                    val budgetAmount = budget.amountCents / 100
                    val spentAmount = spent / 100

                    EnvelopeCard(
                        categoryName = category?.name ?: "Overall",
                        categoryIcon = category?.icon ?: "💵",
                        categoryColor = parseColor(category?.color, index),
                        spentAmount = spentAmount,
                        budgetAmount = budgetAmount,
                        progress = progress,
                        remaining = dailyLeft,
                        hasRollover = budget.rolloverEnabled,
                        onClick = { onEditBudget(budget) }
                    )
                }
            }
        }
    }
}

@Composable
fun EnvelopeCard(
    categoryName: String,
    categoryIcon: String,
    categoryColor: Color,
    spentAmount: Long,
    budgetAmount: Long,
    progress: Float,
    remaining: Long,
    hasRollover: Boolean,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "envelopeFill"
    )

    val fillColor = when {
        progress > 1f -> NudgeColors.Negative
        progress > 0.8f -> NudgeColors.Warning
        else -> categoryColor
    }

    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(NudgeRadius.SM))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(categoryIcon, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(categoryName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NudgeColors.ContentPrimary)
                        Row {
                            Text(
                                "₹${formatter.format(spentAmount)} of ₹${formatter.format(budgetAmount)}",
                                fontSize = 13.sp,
                                color = NudgeColors.ContentSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                            if (hasRollover) {
                                Text(
                                    " · Rollover",
                                    fontSize = 12.sp,
                                    color = NudgeColors.AccentPrimary
                                )
                            }
                        }
                    }
                }
                // Remaining
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${formatter.format(maxOf(0, remaining))}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (remaining >= 0) NudgeColors.ContentPrimary else NudgeColors.Negative
                    )
                    Text(
                        if (remaining >= 0) "left" else "over",
                        fontSize = 11.sp,
                        color = NudgeColors.ContentTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Envelope fill bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(NudgeRadius.SM))
                    .background(NudgeColors.ContentTertiary.copy(alpha = 0.15f))
            ) {
                // Filled portion
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(NudgeRadius.SM))
                        .background(fillColor)
                )

                // Tick marks at 25%, 50%, 75%
                for (tick in listOf(0.25f, 0.5f, 0.75f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(tick)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(NudgeColors.SurfaceBase.copy(alpha = 0.5f))
                    )
                }

                // Percentage text centered
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private fun parseColor(hex: String?, index: Int): Color {
    if (hex.isNullOrBlank()) {
        return NudgeColors.CategoryColors[index % NudgeColors.CategoryColors.size]
    }
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        NudgeColors.CategoryColors[index % NudgeColors.CategoryColors.size]
    }
}

