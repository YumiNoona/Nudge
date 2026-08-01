package com.nudge.android.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.math.absoluteValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeHaptics
import java.text.NumberFormat
import java.util.*

@Composable
fun NeedsReviewSwipeScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onCategorize: (transactionId: String, categoryId: String) -> Unit,
    onDismiss: (transactionId: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = remember { NudgeHaptics(context) }

    if (transactions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("All caught up!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Nc.ink)
                Text("No transactions need review right now", fontSize = 14.sp, color = Nc.inkSoft)
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentTxn = transactions.getOrNull(currentIndex)

    if (currentTxn == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("All reviewed!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Nc.ink)
                TextButton(onClick = onBack) { Text("Done") }
            }
        }
        return
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val fmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val swipeThreshold = 120f
    val rotation = (offsetX / 300f * 12f).coerceIn(-12f, 12f)
    val bgAlpha = (offsetX.absoluteValue / swipeThreshold).coerceIn(0f, 0.15f)
    val bgColor = if (offsetX > 0) Nc.accent.copy(alpha = bgAlpha)
                  else if (offsetX < 0) Nc.negative.copy(alpha = bgAlpha)
                  else Color.Transparent

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Lucide.ChevronLeft(size = 18.dp, strokeWidth = 2.dp)
            Text("Back", color = Nc.inkSoft)
        }

        Text(
            "${currentIndex + 1} / ${transactions.size}",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            fontSize = 13.sp,
            color = Nc.inkMute
        )

        // Hints
        if (offsetX < -40f) Text("Skip ←", modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp).swipeAlpha(offsetX.absoluteValue / 80f), color = Nc.negative, fontWeight = FontWeight.SemiBold)
        if (offsetX > 40f) Text("Categorize →", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).swipeAlpha(offsetX / 80f), color = Nc.accent, fontWeight = FontWeight.SemiBold)

        // Peek cards
        if (currentIndex + 1 < transactions.size) {
            Card(modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.7f).offset(y = 16.dp).swipeAlpha(0.3f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Nc.surface)) {
                Box(modifier = Modifier.height(140.dp))
            }
        }

        // Main swipe card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .offset(x = offsetX.dp, y = offsetY.dp)
                .rotate(rotation)
                .pointerInput(currentTxn.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > swipeThreshold) {
                                haptics.confirm()
                                showCategoryPicker = true
                            } else if (offsetX < -swipeThreshold) {
                                haptics.warning()
                                onDismiss(currentTxn.id)
                                currentIndex++
                                offsetX = 0f; offsetY = 0f
                            } else {
                                offsetX = 0f; offsetY = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f; offsetY = 0f },
                        onHorizontalDrag = { _, drag ->
                            offsetX += drag
                            offsetY = (drag * -0.05f).coerceIn(-30f, 30f)
                        }
                    )
                },
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Nc.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val amount = currentTxn.amountCents / 100.0
                Text(
                    "₹${fmt.format(amount)}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MonoFamily,
                    color = if (currentTxn.type == "debit") Nc.negative else Nc.accent
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentTxn.merchantRaw, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Nc.ink)
                if (currentTxn.sourceRawText != null) {
                    Text(
                        "\"${currentTxn.sourceRawText}\"",
                        fontSize = 12.sp,
                        color = Nc.inkMute,
                        maxLines = 3,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (currentTxn.confidenceScore < 0.7f) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Nc.amberBg, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Low confidence (${(currentTxn.confidenceScore * 100).toInt()}%)", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = Nc.warning)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    // Skip
                    Surface(
                        modifier = Modifier.size(52.dp).clickable {
                            haptics.warning(); onDismiss(currentTxn.id); currentIndex++; offsetX = 0f; offsetY = 0f
                        },
                        shape = CircleShape,
                        color = Nc.coralBg
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Lucide.X(size = 22.dp, strokeWidth = 2.dp)
                        }
                    }
                    // Categorize
                    Surface(
                        modifier = Modifier.size(52.dp).clickable {
                            haptics.confirm(); showCategoryPicker = true
                        },
                        shape = CircleShape,
                        color = Nc.accent
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Lucide.Check(size = 22.dp, strokeWidth = 2.5.dp)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryPicker && currentTxn != null) {
        CategoryPickerDialog(
            currentTxn = currentTxn,
            categories = categories,
            onSelect = { categoryId ->
                onCategorize(currentTxn.id, categoryId)
                showCategoryPicker = false
                currentIndex++
                offsetX = 0f; offsetY = 0f
            },
            onDismiss = { showCategoryPicker = false; offsetX = 0f; offsetY = 0f }
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
    val fmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Categorize ₹${fmt.format(currentTxn.amountCents / 100)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Nc.ink
            )
        },
        text = {
            Column {
                Text(currentTxn.merchantRaw, fontSize = 14.sp, color = Nc.inkSoft)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories.filter { it.type == "expense" }) { cat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Nc.background)
                                .clickable { onSelect(cat.id) }
                                .padding(8.dp)
                        ) {
                            Text(cat.icon ?: "📁", fontSize = 18.sp)
                            Text(cat.name, fontSize = 10.sp, color = Nc.inkSoft, maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Nc.inkSoft) }
        },
        containerColor = Nc.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

private fun Modifier.swipeAlpha(a: Float): Modifier = this.then(Modifier.graphicsLayer { alpha = a })
