package com.nudge.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsListScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) } // 0 = All, 1 = Expenses, 2 = Income
    var showSearch by remember { mutableStateOf(false) }

    val filtered = remember(transactions, query, filter) {
        transactions.filter { txn ->
            val matchesType = when (filter) {
                1 -> txn.type == "debit"
                2 -> txn.type == "credit"
                else -> true
            }
            if (!matchesType) return@filter false
            if (query.isBlank()) return@filter true
            val cat = categories.find { it.id == txn.categoryId }
            txn.merchantRaw.contains(query, true) ||
                (cat?.name?.contains(query, true) == true) ||
                (txn.note?.contains(query, true) == true)
        }
    }

    // Group by day, newest first
    val groups = remember(filtered) {
        filtered.groupBy { dayLabel(it.timestampEpoch) }
    }

    val monthSpend = remember(filtered) {
        filtered.filter { it.type == "debit" }.sumOf { it.amountCents }
    }
    val monthIncome = remember(filtered) {
        filtered.filter { it.type == "credit" }.sumOf { it.amountCents }
    }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        // ── Header ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DSSpace.lg, vertical = DSSpace.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = DSTypography.headlineLarge, color = DSBridge.ink())
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showSearch = !showSearch }) {
                if (showSearch) Lucide.X(size = 20.dp, strokeWidth = 1.8.dp, color = DSBridge.inkSoft())
                else Lucide.Filter(size = 20.dp, strokeWidth = 1.8.dp, color = DSBridge.inkSoft())
            }
        }

        // ── Month summary strip ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DSSpace.lg),
            horizontalArrangement = Arrangement.spacedBy(DSSpace.sm)
        ) {
            SummaryPill("Spent", formatCents(monthSpend), DSBridge.negative(), Modifier.weight(1f))
            SummaryPill("Income", formatCents(monthIncome), DSBridge.positive(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(DSSpace.md))

        // ── Search + filters ──
        AnimatedVisibility(visible = showSearch) {
            Column(Modifier.padding(horizontal = DSSpace.lg)) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(fontSize = 14.sp, color = DSBridge.ink()),
                    cursorBrush = SolidColor(DSBridge.accent()),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DSBridge.surface())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isEmpty()) {
                                Text("Search transactions…", fontSize = 14.sp, color = DSBridge.inkMute())
                            }
                            Spacer(Modifier.width(8.dp))
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(DSSpace.sm))
            }
        }

        Row(
            Modifier.padding(horizontal = DSSpace.lg),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Expenses", "Income").forEachIndexed { i, label ->
                FilterChip(
                    selected = filter == i,
                    onClick = { filter = i },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DSBridge.accentBg(),
                        selectedLabelColor = DSBridge.accent()
                    )
                )
            }
        }

        Spacer(Modifier.height(DSSpace.sm))

        // ── List ──
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DSIconChip({ Lucide.Wallet(size = 24.dp, strokeWidth = 1.5.dp, color = DSBridge.inkMute()) }, size = 56.dp)
                    Spacer(Modifier.height(DSSpace.md))
                    Text(
                        if (transactions.isEmpty()) "No transactions yet" else "Nothing found",
                        style = DSTypography.titleMedium, color = DSBridge.ink()
                    )
                    Text(
                        if (transactions.isEmpty()) "Tap + to add your first entry" else "Try a different search or filter",
                        style = DSTypography.bodySmall, color = DSBridge.inkMute()
                    )
                    if (transactions.isEmpty()) {
                        Spacer(Modifier.height(DSSpace.md))
                        Button(
                            onClick = onAdd,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent())
                        ) { Text("Add Transaction") }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                groups.forEach { (label, txns) ->
                    item(key = "header_$label") {
                        Text(
                            label,
                            style = DSTypography.labelMedium,
                            color = DSBridge.inkMute(),
                            modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(txns, key = { it.id }) { txn ->
                        SwipeableTransactionRow(txn, categories.find { it.id == txn.categoryId }, onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = DSBridge.surface()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = DSTypography.labelSmall, color = DSBridge.inkMute())
            Text(value, style = DSTypography.titleMedium, fontFamily = MonoFamily, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SwipeableTransactionRow(
    txn: TransactionEntity,
    category: CategoryEntity?,
    onDelete: (String) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(txn.id)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DSBridge.negative())
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Lucide.Trash2(size = 20.dp, strokeWidth = 1.8.dp, color = Color.White)
            }
        }
    ) {
        TransactionRowCard(txn, category)
    }
}

@Composable
private fun TransactionRowCard(txn: TransactionEntity, category: CategoryEntity?) {
    val isDebit = txn.type == "debit"
    val date = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(txn.timestampEpoch))
    val catColor = category?.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { DSBridge.accent() } } ?: DSBridge.accent()

    DSCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DSIconChip(
                { Text(category?.icon ?: "💳", fontSize = 18.sp) },
                tint = catColor, bg = catColor.copy(alpha = 0.12f), size = 44.dp
            )
            Spacer(Modifier.width(DSSpace.md))
            Column(Modifier.weight(1f)) {
                Text(txn.merchantRaw, style = DSTypography.titleMedium, color = DSBridge.ink(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${category?.name ?: "Other"} · $date",
                    style = DSTypography.labelSmall, color = DSBridge.inkMute(), maxLines = 1
                )
                if (txn.note != null) {
                    Text(txn.note, style = DSTypography.labelSmall, color = DSBridge.inkMute(), maxLines = 1)
                }
            }
            Spacer(Modifier.width(DSSpace.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isDebit) "−" else "+"}${formatCents(txn.amountCents)}",
                    style = DSTypography.titleMedium, fontFamily = MonoFamily,
                    color = if (isDebit) DSBridge.negative() else DSBridge.positive()
                )
                if (!txn.isReviewed) {
                    DSBadge("New", DSBridge.accent(), DSBridge.accentBg())
                }
            }
        }
    }
}

private fun dayLabel(timestampEpoch: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestampEpoch }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val sameDay: (Calendar) -> Boolean = { other ->
        cal.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }
    return when {
        sameDay(today) -> "Today"
        sameDay(yesterday) -> "Yesterday"
        else -> SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cal.time)
    }
}
