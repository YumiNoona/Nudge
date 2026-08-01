package com.nudge.android.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.data.SavedSourceMessageEntity
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    sources: List<SavedSourceMessageEntity>,
    decryptSource: (SavedSourceMessageEntity?) -> String?,
    captureEnabled: Boolean,
    onSettings: () -> Unit,
    onReview: () -> Unit,
    onAdd: () -> Unit,
    onUpdate: (TransactionEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingSource by remember { mutableStateOf<TransactionEntity?>(null) }
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }
    val currentMonth = transactions.filter { sameMonth(it.timestampEpoch, now) }
    val previous = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    val previousMonth = transactions.filter { sameMonth(it.timestampEpoch, previous) }
    val spent = currentMonth.filter { it.type == "debit" }.sumOf { it.amountCents }
    val income = currentMonth.filter { it.type == "credit" }.sumOf { it.amountCents }
    val previousSpent = previousMonth.filter { it.type == "debit" }.sumOf { it.amountCents }
    val delta = if (previousSpent > 0) (((spent - previousSpent) * 100f) / previousSpent).toInt() else 0
    val needsReview = transactions.count { !it.isReviewed }

    val visible = remember(transactions, query, filter) {
        transactions.filter { txn ->
            val matchesType = filter == "all" || txn.type == filter
            val matchesQuery = query.isBlank() || txn.merchantRaw.contains(query, true) ||
                txn.note.orEmpty().contains(query, true) || txn.amountCents.toString().contains(query)
            matchesType && matchesQuery
        }
    }
    val groups = remember(visible) {
        visible.groupBy { dayKey(it.timestampEpoch) }.toList()
    }

    val latestCaptured = transactions.firstOrNull {
        it.source != "manual" && System.currentTimeMillis() - it.createdAt < 12_000
    }
    var dismissedCaptureId by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(DSBridge.background())) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item {
                HistoryHeader(context, captureEnabled, onSettings)
                MonthSummary(spent, income, delta)
            }

            if (needsReview > 0) {
                item {
                    Surface(
                        onClick = onReview,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = DS.WarningBg
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Sparkles(size = 20.dp, color = DS.Warning)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("$needsReview need${if (needsReview == 1) "s" else ""} review", fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
                                Text("Confirm once and Nudge remembers", fontSize = 11.sp, color = DSBridge.inkSoft())
                            }
                            Lucide.ChevronRight(size = 18.dp, color = DSBridge.inkMute())
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search merchant, note or amount") },
                    leadingIcon = { Lucide.Filter(size = 18.dp, color = DSBridge.inkMute()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("all" to "All", "debit" to "Expenses", "credit" to "Income", "refund" to "Refunds").forEach { (id, label) ->
                        FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label, fontSize = 11.sp) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (groups.isEmpty()) {
                item { EmptyHistory(onAdd) }
            } else {
                groups.forEach { (day, entries) ->
                    item(key = "header_$day") {
                        Text(day, fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = .8.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    }
                    items(entries, key = { it.id }) { txn ->
                        HistoryRow(
                            txn,
                            categories.firstOrNull { it.id == txn.categoryId },
                            accounts.firstOrNull { it.id == txn.accountId },
                            onClick = { editing = txn },
                            onSource = if (txn.source != "manual") ({ viewingSource = txn }) else null
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = latestCaptured != null && latestCaptured.id != dismissedCaptureId,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)
        ) {
            latestCaptured?.let { captured ->
                Surface(shape = RoundedCornerShape(18.dp), color = DS.AccentDeep, shadowElevation = 10.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(DS.Signal), contentAlignment = Alignment.Center) {
                            Lucide.Check(size = 19.dp, color = DS.InkPrimary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Captured ${formatCents(captured.amountCents)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(captured.merchantRaw, color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
                        }
                        TextButton(onClick = { editing = captured }) { Text("Edit", color = DS.Signal) }
                        IconButton(onClick = { dismissedCaptureId = captured.id }) { Lucide.X(size = 16.dp, color = Color.White.copy(alpha = .6f)) }
                    }
                }
            }
        }
    }

    editing?.let { txn ->
        TransactionEditSheet(txn, categories, accounts, onDismiss = { editing = null }, onSource = if (txn.source != "manual") ({ editing = null; viewingSource = txn }) else null, onSave = {
            onUpdate(it); editing = null
        }, onDelete = { onDelete(txn.id); editing = null })
    }

    viewingSource?.let { txn ->
        val source = sources.firstOrNull { it.transactionId == txn.id }
        SourceMessageSheet(
            txn,
            source,
            decryptSource(source),
            accounts.firstOrNull { it.id == txn.accountId },
            categories.firstOrNull { it.id == txn.categoryId },
            onDismiss = { viewingSource = null }
        )
    }
}

@Composable
private fun HistoryHeader(context: Context, enabled: Boolean, onSettings: () -> Unit) {
    val prefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
    val name = prefs.getString("display_name", "You") ?: "You"
    val path = prefs.getString("profile_photo_path", null)
    val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it) } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Transactions", style = DSTypography.headlineLarge, color = DSBridge.ink())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (enabled) DS.Positive else DS.InkTertiary))
                Spacer(Modifier.width(6.dp))
                Text(if (enabled) "AUTO CAPTURE ON" else "AUTO CAPTURE OFF", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = .8.sp, color = DSBridge.inkMute())
            }
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(DSBridge.accent()).clickable(onClick = onSettings), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonthSummary(spent: Long, income: Long, delta: Int) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(28.dp), color = DS.AccentDeep, shadowElevation = 8.dp) {
        Column(Modifier.padding(21.dp)) {
            Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase(), fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp, color = Color.White.copy(alpha = .5f))
            Spacer(Modifier.height(5.dp))
            AnimatedContent(spent, label = "monthSpend") { value ->
                Text(formatCents(value), fontFamily = MonoFamily, fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("spent this month", color = Color.White.copy(alpha = .56f), fontSize = 11.sp)
            Spacer(Modifier.height(17.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryPill("INCOME", formatCents(income), DS.Signal)
                SummaryPill("VS LAST MONTH", if (delta == 0) "—" else "${if (delta > 0) "+" else ""}$delta%", if (delta <= 0) DS.Signal else DS.Negative)
            }
        }
    }
}

@Composable private fun SummaryPill(label: String, value: String, color: Color) {
    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .07f)).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, fontFamily = MonoFamily, fontSize = 8.sp, color = Color.White.copy(alpha = .48f))
        Text(value, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun HistoryRow(txn: TransactionEntity, category: CategoryEntity?, account: AccountEntity?, onClick: () -> Unit, onSource: (() -> Unit)?) {
    val expense = txn.type == "debit"
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (expense) DS.Negative.copy(alpha = .1f) else DS.Positive.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
            if (category != null) {
                CategoryGlyph(category.icon, category.name, if (expense) DS.Negative else DS.Positive, Modifier.size(19.dp))
            } else if (expense) Lucide.ShoppingCart(size = 19.dp, color = DS.Negative)
            else Lucide.TrendingUp(size = 19.dp, color = DS.Positive)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(txn.merchantRaw, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.ink(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (!txn.isReviewed) Box(Modifier.padding(start = 6.dp).size(7.dp).clip(CircleShape).background(DS.Warning))
            }
            Text(listOfNotNull(category?.name ?: "Uncategorized", account?.name, txn.source.takeIf { it != "manual" }).joinToString(" · "), fontSize = 10.sp, color = DSBridge.inkMute(), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${if (expense) "−" else "+"}${formatCents(txn.amountCents)}", fontFamily = MonoFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (expense) DSBridge.ink() else DS.Positive)
            Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(txn.timestampEpoch)), fontSize = 9.sp, color = DSBridge.inkMute())
        }
        if (onSource != null) {
            Spacer(Modifier.width(5.dp))
            IconButton(onClick = onSource, modifier = Modifier.size(36.dp)) { Lucide.Message(size = 16.dp, color = DSBridge.inkMute()) }
        }
    }
}

@Composable private fun EmptyHistory(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Lucide.ListTodo(size = 34.dp, color = DSBridge.inkMute())
        Spacer(Modifier.height(12.dp)); Text("No transactions yet", fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
        Text("Add one or enable automatic capture", fontSize = 12.sp, color = DSBridge.inkMute())
        TextButton(onClick = onAdd) { Text("Add transaction") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditSheet(txn: TransactionEntity, categories: List<CategoryEntity>, accounts: List<AccountEntity>, onDismiss: () -> Unit, onSource: (() -> Unit)?, onSave: (TransactionEntity) -> Unit, onDelete: () -> Unit) {
    var merchant by remember { mutableStateOf(txn.merchantRaw) }
    var amount by remember { mutableStateOf((txn.amountCents / 100.0).toString()) }
    var type by remember { mutableStateOf(txn.type) }
    var categoryId by remember { mutableStateOf(txn.categoryId) }
    var accountId by remember { mutableStateOf(txn.accountId) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = remember(context) { NudgeHaptics(context) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DSBridge.surface(), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).imePadding()
                .padding(horizontal = 22.dp).padding(bottom = 32.dp)
        ) {
            Text("Edit transaction", style = DSTypography.headlineMedium, color = DSBridge.ink())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Amount") }, prefix = { Text("₹ ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("debit" to "Expense", "credit" to "Income", "refund" to "Refund", "transfer" to "Transfer").forEach { (id, label) ->
                    FilterChip(selected = type == id, onClick = { type = id }, label = { Text(label, fontSize = 10.sp) })
                }
            }
            Text("Category", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                categories.filter { it.type == if (type == "credit") "income" else "expense" }.forEach { category ->
                    FilterChip(selected = categoryId == category.id, onClick = { categoryId = category.id }, label = { Text(category.name, fontSize = 10.sp) })
                }
            }
            Text("Account", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                accounts.filter { !it.isArchived }.forEach { account ->
                    FilterChip(selected = accountId == account.id, onClick = { accountId = account.id }, label = { Text(account.name, fontSize = 10.sp) })
                }
            }
            Spacer(Modifier.height(16.dp))
            if (onSource != null) {
                OutlinedButton(onClick = onSource, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Lucide.Message(size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("View source message")
                }
                Spacer(Modifier.height(10.dp))
            }
            Button(onClick = {
                val cents = ((amount.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                if (cents > 0 && merchant.isNotBlank()) {
                    haptics.success()
                    onSave(txn.copy(amountCents = cents, merchantRaw = merchant.trim(), merchantNormalized = merchant.trim(), type = type, categoryId = categoryId, accountId = accountId, isReviewed = true))
                }
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent())) { Text("Save changes") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DS.Negative),
                border = androidx.compose.foundation.BorderStroke(1.dp, DS.Negative.copy(alpha = .38f))
            ) {
                Lucide.Trash2(size = 18.dp, color = DS.Negative)
                Spacer(Modifier.width(8.dp))
                Text("Delete transaction", color = DS.Negative)
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete transaction?") },
        text = { Text("The transaction and its linked source metadata will be removed. This cannot be undone.") },
        confirmButton = { Button(onClick = { confirmDelete = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = DS.Negative)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}

private fun sameMonth(epoch: Long, target: Calendar): Boolean = Calendar.getInstance().apply { timeInMillis = epoch }.let {
    it.get(Calendar.YEAR) == target.get(Calendar.YEAR) && it.get(Calendar.MONTH) == target.get(Calendar.MONTH)
}
private fun dayKey(epoch: Long): String {
    val date = Calendar.getInstance().apply { timeInMillis = epoch }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        date.get(Calendar.YEAR) == today.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "TODAY"
        date.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "YESTERDAY"
        else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(epoch)).uppercase()
    }
}
