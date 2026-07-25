package com.nudge.android.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.GamificationProfileEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.engine.GamificationMath
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToReview: () -> Unit = {},
    onNavigateToMore: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val needsReviewCount by viewModel.needsReviewCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val gamification by viewModel.gamificationProfile.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    val fmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val cal = remember { Calendar.getInstance() }
    val thisMonth = cal.get(Calendar.MONTH)
    val thisYear = cal.get(Calendar.YEAR)
    val monthTxns = remember(transactions) {
        transactions.filter {
            val d = Calendar.getInstance().apply { timeInMillis = it.timestampEpoch }
            d.get(Calendar.MONTH) == thisMonth && d.get(Calendar.YEAR) == thisYear
        }
    }
    val spend = remember(monthTxns) { monthTxns.filter { it.type == "debit" }.sumOf { it.amountCents } }
    val income = remember(monthTxns) { monthTxns.filter { it.type == "credit" }.sumOf { it.amountCents } }
    val leftToSpend = 0L // placeholder — budget math would go here

    val surface = if (isDark) NudgeColors.SurfaceDark else NudgeColors.Surface
    val bg = if (isDark) NudgeColors.Dark else NudgeColors.Bone
    val ink = if (isDark) NudgeColors.InkDark else NudgeColors.Ink

    Scaffold(
        modifier = Modifier.background(bg),
        containerColor = bg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Greeting + streak strip ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Hello 👋",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NudgeColors.InkSoft
                        )
                        Text(
                            "Your money",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ink
                        )
                    }
                    val g = gamification
                    if (g != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NudgeColors.EmeraldBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Lucide.Flame(size = 16.dp, strokeWidth = 1.8.dp)
                                Text(
                                    "${g.currentStreakDays}d",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NudgeColors.Emerald
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. Hero card — spent this month ──
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = NudgeColors.Emerald)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Spent this month",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${fmt.format(spend / 100)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // quick-action pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.2f),
                                onClick = onNavigateToReview
                            ) {
                                Text(
                                    "${needsReviewCount} to review →",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.2f),
                                onClick = { showAddSheet = true }
                            ) {
                                Text(
                                    "+ Add",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Quick-stat row ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Income",
                        value = "₹${fmt.format(income / 100)}",
                        icon = { Lucide.TrendingUp(size = 16.dp, strokeWidth = 1.8.dp) },
                        tint = NudgeColors.Emerald,
                        surface = surface
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Spent",
                        value = "₹${fmt.format(spend / 100)}",
                        icon = { Lucide.ShoppingCart(size = 16.dp, strokeWidth = 1.8.dp) },
                        tint = NudgeColors.Coral,
                        surface = surface
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Left",
                        value = "—",
                        icon = { Lucide.Wallet(size = 16.dp, strokeWidth = 1.8.dp) },
                        tint = NudgeColors.InkMute,
                        surface = surface
                    )
                }
            }

            // ── Widgets row ──
            item {
                HomeWidgetsRow(
                    streakDays = gamification?.currentStreakDays ?: 0,
                    spend = spend,
                    income = income,
                    categories = categories,
                    transactions = monthTxns,
                    gamification = gamification,
                    surface = surface
                )
            }

            // ── 4. Review callout (if pending) ──
            if (needsReviewCount > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = NudgeColors.AmberBg),
                        onClick = onNavigateToReview
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${needsReviewCount} transaction${if (needsReviewCount > 1) "s" else ""} need review",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NudgeColors.Amber
                                )
                                Text(
                                    "Swipe to categorize",
                                    fontSize = 11.sp,
                                    color = NudgeColors.InkMute
                                )
                            }
                            Lucide.ArrowLeft(size = 16.dp, strokeWidth = 2.dp)
                        }
                    }
                }
            }

            // ── 5. Recent Activity ──
            item {
                Text(
                    "Recent Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NudgeColors.InkSoft,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Lucide.Wallet(size = 32.dp, strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No transactions yet", fontSize = 14.sp, color = NudgeColors.InkSoft)
                            Text("Tap + to add your first entry", fontSize = 12.sp, color = NudgeColors.InkMute)
                        }
                    }
                }
            } else {
                items(transactions.take(20), key = { it.id }) { txn ->
                    val cat = categories.find { it.id == txn.categoryId }
                    TransactionRow(
                        transaction = txn,
                        category = cat,
                        isDark = isDark,
                        onDelete = { viewModel.deleteTransaction(txn.id) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            categories = categories,
            accounts = accounts,
            onDismiss = { showAddSheet = false },
            onAdd = { amount, type, merchant, accountId, categoryId, note ->
                viewModel.addTransaction(amount, type, merchant, accountId, categoryId, note)
                showAddSheet = false
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    tint: androidx.compose.ui.graphics.Color,
    surface: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NudgeColors.InkMute)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = tint
            )
        }
    }
}

// ─── Home Widgets Row ─────────────────────────────────────────

@Composable
private fun HomeWidgetsRow(
    streakDays: Int,
    spend: Long,
    income: Long,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    gamification: GamificationProfileEntity?,
    surface: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak widget
        WidgetCard(surface) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Lucide.Flame(size = 22.dp, strokeWidth = 1.8.dp, color = NudgeColors.Coral)
                Column {
                    Text("${streakDays}d", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NudgeColors.Ink)
                    Text("streak", fontSize = 10.sp, color = NudgeColors.InkMute)
                }
            }
        }

        // Monthly ring widget
        WidgetCard(surface) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val totalBudget = 0L // placeholder
                val progress = if (totalBudget > 0) (spend.toFloat() / totalBudget).coerceIn(0f, 1f) else 0.5f
                AnimatedRing(progress, NudgeColors.Emerald)
                Text("${(progress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                Text("of budget", fontSize = 10.sp, color = NudgeColors.InkMute)
            }
        }

        // Category leaderboard
        WidgetCard(surface) {
            val topCats = transactions
                .filter { it.type == "debit" }
                .groupBy { it.categoryId }
                .mapValues { (_, txns) -> txns.sumOf { it.amountCents } }
                .entries
                .sortedByDescending { it.value }
                .take(3)
            Column {
                Text("Top categories", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.Ink)
                Spacer(Modifier.height(6.dp))
                topCats.forEach { (catId, spent) ->
                    val cat = categories.find { it.id == catId }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(NudgeColors.CatBlue))
                        Text(cat?.name ?: "Other", fontSize = 10.sp, color = NudgeColors.InkSoft, modifier = Modifier.width(60.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("₹${NumberFormat.getNumberInstance(Locale.getDefault()).format(spent / 100)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = NudgeColors.Ink)
                    }
                }
            }
        }

        // XP widget
        if (gamification != null) {
            WidgetCard(surface) {
                Column {
                    Text("Level ${gamification.level}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    val xpProgress = GamificationMath.levelProgress(gamification.xpTotal)
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(NudgeColors.EmeraldBg)) {
                        Box(Modifier.fillMaxWidth(xpProgress).fillMaxHeight().background(NudgeColors.Emerald))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${gamification.xpTotal} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = NudgeColors.Emerald)
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(surface: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        Box(Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun AnimatedRing(progress: Float, color: Color) {
    val animProgress by animateFloatAsState(progress, tween(600), label = "ring")
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.LightGray.copy(alpha = 0.2f), 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
            drawArc(color, -90f, animProgress * 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    val isDebit = transaction.type == "debit"
    val surface = if (isDark) NudgeColors.SurfaceDark else NudgeColors.Surface
    val fmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NudgeColors.parse(category?.color, NudgeColors.InkMute).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category?.icon ?: "💳", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        transaction.merchantRaw,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) NudgeColors.InkDark else NudgeColors.Ink
                    )
                    Text(
                        "${category?.name ?: "Uncategorized"} · ${java.text.SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transaction.timestampEpoch))}",
                        fontSize = 11.sp,
                        color = NudgeColors.InkMute
                    )
                }
            }
            Text(
                "${if (isDebit) "−" else "+"}₹${fmt.format(transaction.amountCents / 100)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = if (isDebit) NudgeColors.Coral else NudgeColors.Emerald
            )
        }
    }
}
