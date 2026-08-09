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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
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
import com.nudge.android.ui.components.NudgeHeroCard
import com.nudge.android.ui.components.NudgeHeroStyle
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    onUpdate: (TransactionEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingSource by remember { mutableStateOf<TransactionEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = remember(context) { NudgeHaptics(context) }
    var pendingDeleteIds by remember { mutableStateOf(emptySet<String>()) }
    val today = remember { Calendar.getInstance() }
    var selectedYear by rememberSaveable { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var selectedMonthIndex by rememberSaveable { mutableIntStateOf(today.get(Calendar.MONTH)) }
    val selectedMonth = remember(selectedYear, selectedMonthIndex) {
        Calendar.getInstance().apply { clear(); set(selectedYear, selectedMonthIndex, 1) }
    }
    val selectedMonthTransactions = remember(transactions, selectedYear, selectedMonthIndex) {
        transactions.filter { sameMonth(it.timestampEpoch, selectedMonth) }
    }
    val previous = remember(selectedYear, selectedMonthIndex) {
        Calendar.getInstance().apply { clear(); set(selectedYear, selectedMonthIndex, 1); add(Calendar.MONTH, -1) }
    }
    val previousMonth = transactions.filter { sameMonth(it.timestampEpoch, previous) }
    val spent = selectedMonthTransactions.filter { it.type == "debit" }.sumOf { it.amountCents }
    val income = selectedMonthTransactions.filter { it.type == "credit" }.sumOf { it.amountCents }
    val refunds = selectedMonthTransactions.filter { it.type == "refund" }.sumOf { it.amountCents }
    val previousSpent = previousMonth.filter { it.type == "debit" }.sumOf { it.amountCents }
    val delta = if (previousSpent > 0) (((spent - previousSpent) * 100f) / previousSpent).toInt() else 0
    val needsReview = selectedMonthTransactions.count { !it.isReviewed }
    val monthLabel = remember(selectedYear, selectedMonthIndex) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedMonth.time)
    }
    val canGoNext = selectedYear < today.get(Calendar.YEAR) ||
        (selectedYear == today.get(Calendar.YEAR) && selectedMonthIndex < today.get(Calendar.MONTH))

    val visible = remember(selectedMonthTransactions, query, filter, pendingDeleteIds) {
        selectedMonthTransactions.filter { txn ->
            if (txn.id in pendingDeleteIds) return@filter false
            val matchesType = when (filter) {
                "all" -> true
                "smart" -> txn.source != "manual"
                else -> txn.type == filter
            }
            val matchesQuery = query.isBlank() || txn.merchantRaw.contains(query, true) ||
                txn.note.orEmpty().contains(query, true) || txn.amountCents.toString().contains(query)
            matchesType && matchesQuery
        }
    }
    val groups = remember(visible) {
        visible.groupBy { dayKey(it.timestampEpoch) }.toList()
    }

    val latestCaptured = selectedMonthTransactions.firstOrNull {
        it.source != "manual" && System.currentTimeMillis() - it.createdAt < 12_000
    }
    var dismissedCaptureId by remember { mutableStateOf<String?>(null) }

    fun requestDelete(transaction: TransactionEntity) {
        if (transaction.id in pendingDeleteIds) return
        pendingDeleteIds = pendingDeleteIds + transaction.id
        haptics.warning()
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Transaction removed",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result != SnackbarResult.ActionPerformed) onDelete(transaction.id)
            pendingDeleteIds = pendingDeleteIds - transaction.id
        }
    }

    Box(Modifier.fillMaxSize().background(DSBridge.background())) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item {
                HistoryHeader(
                    context = context,
                    searchActive = showSearch,
                    onSearch = { showSearch = !showSearch },
                    onSettings = onSettings,
                )
                MonthSummary(
                    spent = spent,
                    income = income,
                    refunds = refunds,
                    delta = delta,
                    count = selectedMonthTransactions.size,
                    monthLabel = monthLabel,
                    canGoNext = canGoNext,
                    onPrevious = {
                        if (selectedMonthIndex == Calendar.JANUARY) {
                            selectedMonthIndex = Calendar.DECEMBER
                            selectedYear--
                        } else selectedMonthIndex--
                    },
                    onNext = {
                        if (canGoNext) {
                            if (selectedMonthIndex == Calendar.DECEMBER) {
                                selectedMonthIndex = Calendar.JANUARY
                                selectedYear++
                            } else selectedMonthIndex++
                        }
                    },
                )
            }

            if (needsReview > 0) {
                item {
                    Surface(
                        onClick = onReview,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = DSBridge.warningBg(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DS.Warning.copy(alpha = .28f)),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Sparkles(size = 20.dp, color = DS.Warning)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("$needsReview need${if (needsReview == 1) "s" else ""} review", fontWeight = FontWeight.SemiBold, color = DS.Warning)
                                Text("Confirm once and Nudge remembers", fontSize = 11.sp, color = DSBridge.inkSoft())
                            }
                            Lucide.ChevronRight(size = 18.dp, color = DSBridge.inkMute())
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Merchant, note or amount") },
                        leadingIcon = { Lucide.Search(size = 17.dp, color = DSBridge.inkMute()) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Lucide.X(size = 16.dp, color = DSBridge.inkMute())
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(15.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().tourTarget(TourTarget.TransactionFilters).horizontalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("all" to "All", "debit" to "Expenses", "credit" to "Income", "refund" to "Refunds", "smart" to "Smart").forEach { (id, label) ->
                        FilterChip(
                            selected = filter == id,
                            onClick = { filter = id },
                            label = { Text(label, fontSize = 10.sp, fontFamily = MonoFamily) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = if (id == "smart") ({ Lucide.Sparkles(size = 13.dp, color = if (filter == id) DSBridge.accent() else DSBridge.inkMute()) }) else null,
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
            }

            if (groups.isEmpty()) {
                item { EmptyHistory() }
            } else {
                groups.forEach { (day, entries) ->
                    item(key = "header_$day") {
                        Text(day, fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = .8.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    }
                    items(entries, key = { it.id }) { txn ->
                        SwipeToDeleteTransaction(
                            transaction = txn,
                            onDelete = { requestDelete(txn) },
                        ) {
                            HistoryRow(
                                txn,
                                categories.firstOrNull { it.id == txn.categoryId },
                                accounts.firstOrNull { it.id == txn.accountId },
                                onClick = { editing = txn },
                            )
                        }
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
                SwipeDismissCaptureBanner(
                    transaction = captured,
                    onEdit = { editing = captured },
                    onDismiss = { dismissedCaptureId = captured.id },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 94.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = DS.AccentDeep,
                contentColor = Color.White,
                actionColor = DS.Signal,
                dismissActionContentColor = Color.White.copy(alpha = .65f),
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteTransaction(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dragOffset by remember(transaction.id) { mutableFloatStateOf(0f) }
    var rowWidth by remember(transaction.id) { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxWidth().onSizeChanged { rowWidth = it.width.toFloat() },
    ) {
        Box(
            Modifier.matchParentSize().background(DSBridge.background()).padding(horizontal = 22.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text("Delete", color = DS.Negative, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = dragOffset }
                .pointerInput(transaction.id, rowWidth) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffset = (dragOffset + amount).coerceIn(-rowWidth * .38f, 0f)
                        },
                        onDragCancel = {
                            scope.launch { androidx.compose.animation.core.animate(dragOffset, 0f) { value, _ -> dragOffset = value } }
                        },
                        onDragEnd = {
                            if (dragOffset <= -rowWidth * .28f) onDelete()
                            else scope.launch { androidx.compose.animation.core.animate(dragOffset, 0f) { value, _ -> dragOffset = value } }
                        },
                    )
                },
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeDismissCaptureBanner(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var dragOffset by remember(transaction.id) { mutableStateOf(Offset.Zero) }
    var bannerWidth by remember { mutableFloatStateOf(1f) }
    var bannerHeight by remember { mutableFloatStateOf(1f) }
    val dismissProgress = maxOf(abs(dragOffset.x) / bannerWidth, -dragOffset.y / bannerHeight).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size -> bannerWidth = size.width.toFloat(); bannerHeight = size.height.toFloat() }
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                alpha = 1f - dismissProgress * .48f
            }
            .pointerInput(transaction.id) {
                detectDragGestures(
                    onDragCancel = { dragOffset = Offset.Zero },
                    onDragEnd = {
                        val dismiss = abs(dragOffset.x) > bannerWidth * .18f || dragOffset.y < -bannerHeight * .22f
                        if (dismiss) onDismiss() else dragOffset = Offset.Zero
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset = Offset(
                            x = dragOffset.x + amount.x,
                            y = minOf(0f, dragOffset.y + amount.y),
                        )
                    },
                )
            },
        shape = RoundedCornerShape(18.dp),
        color = DS.AccentDeep,
        shadowElevation = 10.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(DS.Signal), contentAlignment = Alignment.Center) {
                Lucide.Check(size = 19.dp, color = DS.InkPrimary)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Captured ${if (transaction.type == "debit") "−" else ""}${formatCents(transaction.amountCents)}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(transaction.merchantRaw, color = Color.White.copy(alpha = .6f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onEdit) { Text("Edit", color = DS.Signal) }
            IconButton(onClick = onDismiss) { Lucide.X(size = 16.dp, color = Color.White.copy(alpha = .6f)) }
        }
    }
}

@Composable
private fun HistoryHeader(
    context: Context,
    searchActive: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val prefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
    val name = prefs.getString("display_name", "You") ?: "You"
    val path = prefs.getString("profile_photo_path", null)
    val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it) } }
    Box(Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 18.dp)) {
        Box(
            Modifier.size(40.dp).align(Alignment.CenterStart).tourTarget(TourTarget.Profile).clip(CircleShape).background(DSBridge.accentBg()).clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) Image(bitmap.asImageBitmap(), "Profile", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(name.take(1).uppercase(), color = DSBridge.accent(), fontWeight = FontWeight.Bold)
        }
        Text("Transactions", style = DSTypography.headlineMedium, color = DSBridge.ink(), modifier = Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.CenterEnd)) {
            CompactHeaderAction(active = searchActive, onClick = onSearch) {
                if (searchActive) Lucide.X(size = 18.dp, color = DSBridge.accent())
                else Lucide.Search(size = 18.dp, color = DSBridge.inkSoft())
            }
        }
    }
}

@Composable
private fun CompactHeaderAction(active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = if (active) DSBridge.accentBg() else DSBridge.surface(),
        border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .16f)),
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun MonthSummary(
    spent: Long,
    income: Long,
    refunds: Long,
    delta: Int,
    count: Int,
    monthLabel: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val netFlow = income + refunds - spent
    val currency = currentCurrencySymbol()
    NudgeHeroCard(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp).tourTarget(TourTarget.MonthSummary),
        style = NudgeHeroStyle.CashFlow,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NET CASH FLOW", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = 1.1.sp, color = Color.White.copy(alpha = .55f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(24.dp)) {
                            Lucide.ChevronLeft(size = 14.dp, color = Color.White.copy(alpha = .72f))
                        }
                        Text(monthLabel, fontSize = 11.sp, color = Color.White.copy(alpha = .82f), maxLines = 1)
                        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(24.dp)) {
                            Lucide.ChevronRight(size = 14.dp, color = Color.White.copy(alpha = if (canGoNext) .72f else .22f))
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(9.dp), color = if (delta <= 0) DS.Positive.copy(alpha = .10f) else DS.Negative.copy(alpha = .10f)) {
                    Text(
                        if (delta == 0) "STEADY" else "${kotlin.math.abs(delta)}% ${if (delta > 0) "MORE" else "LESS"} SPENT",
                        fontFamily = MonoFamily,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (delta <= 0) DS.Positive else DS.Negative,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            AnimatedContent(netFlow, label = "monthFlow") { value ->
                Text(
                    "${if (value < 0) "−" else ""}$currency${formatCompactCentsPlain(kotlin.math.abs(value))}",
                    fontFamily = MonoFamily,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (value < 0) DS.Negative else Color.White,
                )
            }
            Text(
                if (netFlow < 0) "expenses exceeded money in · $count entries" else "money retained · $count entries",
                color = Color.White.copy(alpha = .52f),
                fontSize = 9.sp,
            )
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("MONEY IN", "$currency${formatCompactCentsPlain(income)}", DS.Positive, Modifier.weight(1f), onDark = true)
                SummaryPill("SPENT", "$currency${formatCompactCentsPlain(spent)}", DS.Negative, Modifier.weight(1f), onDark = true)
                SummaryPill("REFUNDS", "$currency${formatCompactCentsPlain(refunds)}", DS.Warning, Modifier.weight(1f), onDark = true)
            }
        }
    }
}

@Composable private fun SummaryPill(label: String, value: String, color: Color, modifier: Modifier = Modifier, onDark: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .08f)).padding(horizontal = 9.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(5.dp))
            Text(label, fontFamily = MonoFamily, fontSize = 7.sp, color = if (onDark) Color.White.copy(alpha = .50f) else DSBridge.inkMute(), maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (onDark) Color.White else DSBridge.ink(), maxLines = 1)
    }
}

@Composable
private fun HistoryRow(txn: TransactionEntity, category: CategoryEntity?, account: AccountEntity?, onClick: () -> Unit) {
    val expense = txn.type == "debit"
    val amountSign = when (txn.type) {
        "debit" -> "−"
        else -> ""
    }
    val semanticColor = when (txn.type) {
        "debit" -> DS.Negative
        "credit" -> DS.Positive
        "refund" -> DS.Warning
        else -> DSBridge.inkSoft()
    }
    Column(Modifier.fillMaxWidth().background(DSBridge.background())) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(DSBridge.surface()), contentAlignment = Alignment.Center) {
            if (category != null) {
                CategoryGlyph(category.icon, category.name, semanticColor, Modifier.size(17.dp))
            } else if (expense) Lucide.ShoppingCart(size = 17.dp, color = DS.Negative)
            else Lucide.TrendingUp(size = 17.dp, color = semanticColor)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(txn.merchantRaw, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.ink(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (!txn.isReviewed) Box(Modifier.padding(start = 6.dp).size(7.dp).clip(CircleShape).background(DS.Warning))
            }
            Text(listOfNotNull(category?.name ?: "Uncategorized", account?.name, txn.source.takeIf { it != "manual" }).joinToString(" · "), fontSize = 10.sp, color = DSBridge.inkMute(), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$amountSign${formatCents(txn.amountCents)}", fontFamily = MonoFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = semanticColor)
            Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(txn.timestampEpoch)), fontSize = 9.sp, color = DSBridge.inkMute())
        }
    }
        HorizontalDivider(Modifier.padding(start = 69.dp, end = 18.dp), color = DSBridge.inkMute().copy(alpha = .09f))
    }
}

@Composable private fun EmptyHistory() {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Lucide.ListTodo(size = 34.dp, color = DSBridge.inkMute())
        Spacer(Modifier.height(12.dp)); Text("No transactions yet", fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
        Text("Use the + button below or enable automatic capture", fontSize = 12.sp, color = DSBridge.inkMute(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
