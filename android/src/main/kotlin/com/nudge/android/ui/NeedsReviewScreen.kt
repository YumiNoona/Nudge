package com.nudge.android.ui

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.android.ui.theme.NudgeHaptics
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Tinder-style swipe card deck for reviewing uncategorized transactions.
 * §4.3 spec: "satisfying swipe-to-categorize card UI rather than a boring list"
 *
 * - Card follows finger 1:1 with slight rotation on drag
 * - Background tints green (right swipe = categorize) or coral (left swipe = skip/dismiss)
 * - Spring-back if released in dead-zone
 * - Haptic tick at decision threshold crossing
 */
@Composable
fun NeedsReviewSwipeScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onCategorize: (transactionId: String, categoryId: String) -> Unit,
    onDismiss: (transactionId: String) -> Unit,
    onBack: () -> Unit
) {
    val haptics = remember { NudgeHaptics(LocalContext.current) }

    if (transactions.isEmpty()) {
        // Empty state — all caught up
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "All caught up!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.ContentPrimary
                )
                Text(
                    "No transactions need review right now",
                    fontSize = 14.sp,
                    color = NudgeColors.ContentSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("Back to home") }
            }
        }
        return
    }

    // We work with the top card only
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentTxn = transactions.getOrNull(currentIndex)

    if (currentTxn == null) {
        // All done
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("All reviewed!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.ContentPrimary)
                TextButton(onClick = onBack) { Text("Done") }
            }
        }
        return
    }

    // Swipe state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var swipedRight by remember { mutableStateOf(false) }

    val screenWidth = 300.dp.value // approximate card width in px
    val swipeThreshold = 120f // px to trigger action

    // Rotation based on drag
    val rotation = (offsetX / screenWidth * 15f).coerceIn(-15f, 15f)

    // Scale for "pressing down" feel
    val scale = 1f - (offsetX.absoluteValue / screenWidth * 0.05f).coerceIn(0f, 0.1f)

    // Background color tint based on swipe direction
    val bgAlpha = (offsetX.absoluteValue / swipeThreshold).coerceIn(0f, 0.3f)
    val bgColor = if (offsetX > 0) {
        NudgeColors.Positive.copy(alpha = bgAlpha)
    } else if (offsetX < 0) {
        NudgeColors.Negative.copy(alpha = bgAlpha)
    } else {
        Color.Transparent
    }

    // Swipe hints
    val leftHintAlpha = if (offsetX < -30f) (offsetX.absoluteValue / 80f).coerceIn(0f, 1f) else 0f
    val rightHintAlpha = if (offsetX > 30f) (offsetX / 80f).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        // Back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text("← Back", color = NudgeColors.ContentSecondary)
        }

        // Progress indicator
        Text(
            "${currentIndex + 1} / ${transactions.size}",
            modifier = Modifier.align(Alignment.TopCenter),
            fontSize = 14.sp,
            color = NudgeColors.ContentSecondary
        )

        // Hints
        if (leftHintAlpha > 0.1f) {
            Text(
                "← Skip",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NudgeColors.Negative.copy(alpha = leftHintAlpha)
            )
        }
        if (rightHintAlpha > 0.1f) {
            Text(
                "Categorize →",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NudgeColors.Positive.copy(alpha = rightHintAlpha)
            )
        }

        // The swipeable card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f)
                .offset(x = offsetX.dp, y = offsetY.dp)
                .rotate(rotation)
                .scale(scale)
                .pointerInput(currentTxn.id) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            if (offsetX > swipeThreshold) {
                                // Right swipe — show category picker
                                swipedRight = true
                                haptics.confirm()
                                showCategoryPicker = true
                            } else if (offsetX < -swipeThreshold) {
                                // Left swipe — skip/dismiss
                                haptics.warning()
                                isAnimating = true
                                onDismiss(currentTxn.id)
                                currentIndex++
                                offsetX = 0f
                                offsetY = 0f
                                isAnimating = false
                            } else {
                                // Spring back
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                        onDragCancel = {
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isAnimating && !showCategoryPicker) {
                                offsetX += dragAmount
                                offsetY = (dragAmount * -0.05f).coerceIn(-30f, 30f)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = (8 - (offsetX.absoluteValue / screenWidth * 4)).coerceAtLeast(2f).dp
            ),
            colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Amount — big and prominent
                val amount = currentTxn.amountCents / 100.0
                val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)

                Text(
                    "₹$formatted",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (currentTxn.type == "debit") NudgeColors.Negative else NudgeColors.Positive
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Merchant
                Text(
                    currentTxn.merchantRaw,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NudgeColors.ContentPrimary
                )

                // Source hint
                if (currentTxn.sourceRawText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "\"${currentTxn.sourceRawText}\"",
                        fontSize = 13.sp,
                        color = NudgeColors.ContentTertiary,
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confidence badge
                if (currentTxn.confidenceScore < 0.7f) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NudgeColors.Warning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Low confidence (${(currentTxn.confidenceScore * 100).toInt()}%)",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = NudgeColors.Warning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick-decision buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Skip button
                    OutlinedButton(
                        onClick = {
                            haptics.warning()
                            onDismiss(currentTxn.id)
                            currentIndex++
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NudgeColors.Negative)
                    ) {
                        Text("Skip", color = NudgeColors.Negative)
                    }

                    // Categorize button
                    Button(
                        onClick = {
                            haptics.confirm()
                            showCategoryPicker = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.AccentPrimary)
                    ) {
                        Text("Categorize")
                    }
                }
            }
        }

        // Stack peek effect — show next cards behind
        if (currentIndex + 1 < transactions.size) {
            // Second card (behind)
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.75f)
                    .offset(y = 12.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.height(180.dp))
            }

            // Third card (behind)
            if (currentIndex + 2 < transactions.size) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.65f)
                        .offset(y = 24.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    // Category picker dialog
    if (showCategoryPicker) {
        CategoryPickerDialog(
            currentTxn = currentTxn,
            categories = categories,
            onSelect = { categoryId ->
                onCategorize(currentTxn.id, categoryId)
                showCategoryPicker = false
                currentIndex++
                offsetX = 0f
                offsetY = 0f
            },
            onDismiss = {
                showCategoryPicker = false
                offsetX = 0f
                offsetY = 0f
            }
        )
    }
}

@Composable
fun CategoryPickerDialog(
    currentTxn: TransactionEntity,
    categories: List<CategoryEntity>,
    onSelect: (categoryId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val expenseCategories = categories.filter { it.type == "expense" }
    val incomeCategories = categories.filter { it.type == "income" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val amount = currentTxn.amountCents / 100.0
            Text(
                "Categorize ₹${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    currentTxn.merchantRaw,
                    fontSize = 15.sp,
                    color = NudgeColors.ContentSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Select category:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NudgeColors.ContentSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cats = expenseCategories + incomeCategories
                    items(cats) { category ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NudgeColors.SurfaceBase)
                                .clickable { onSelect(category.id) }
                                .padding(8.dp)
                        ) {
                            Text(category.icon ?: "📁", fontSize = 18.sp)
                            Text(
                                category.name,
                                fontSize = 10.sp,
                                color = NudgeColors.ContentSecondary,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NudgeColors.ContentSecondary)
            }
        },
        containerColor = NudgeColors.SurfaceRaised,
        shape = RoundedCornerShape(28.dp)
    )
}
