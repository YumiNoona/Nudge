package com.nudge.android.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.engine.GamificationMath
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToReview: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val needsReviewCount by viewModel.needsReviewCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val gamification by viewModel.gamificationProfile.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = CircleShape,
                containerColor = NudgeColors.AccentPrimary,
                contentColor = NudgeColors.SurfaceRaised
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    if (isDark) NudgeColors.DarkSurfaceBase
                    else NudgeColors.SurfaceBase
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Greeting + Level Badge
            item {
                GreetingHeader(gamification)
            }

            // 2. Big "This Month" spend number
            item {
                MonthSpendCard(
                    transactions = transactions,
                    isDark = isDark
                )
            }

            // 3. Budget rings (horizontally scrollable)
            item {
                BudgetRingsRow(
                    budgets = budgets,
                    transactions = transactions,
                    categories = categories
                )
            }

            // 4. Streak flame + XP bar
            item {
                StreakXpStrip(gamification)
            }

            // 5. Needs Review teaser
            item {
                NeedsReviewTeaser(
                    count = needsReviewCount,
                    onClick = onNavigateToReview
                )
            }

            // 6. Recent transactions
            item {
                Text(
                    "Recent Transactions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(transactions.take(20), key = { it.id }) { txn ->
                    TransactionRow(
                        transaction = txn,
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
fun GreetingHeader(gamification: com.nudge.android.data.GamificationProfileEntity?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Hello,",
                fontSize = 14.sp,
                color = NudgeColors.ContentSecondary
            )
            Text(
                "Your Money",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NudgeColors.ContentPrimary
            )
        }
        if (gamification != null) {
            Surface(
                shape = RoundedCornerShape(NudgeRadius.LG),
                color = NudgeColors.AccentPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    GamificationMath.levelTitle(gamification.level),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NudgeColors.AccentPrimary
                )
            }
        }
    }
}

@Composable
fun MonthSpendCard(transactions: List<TransactionEntity>, isDark: Boolean) {
    val thisMonthSpend = remember(transactions) {
        transactions
            .filter { it.type == "debit" }
            .sumOf { it.amountCents }
    }
    val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(thisMonthSpend / 100.0)

    Card(
        shape = RoundedCornerShape(NudgeRadius.XL),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NudgeColors.DarkSurfaceRaised else NudgeColors.SurfaceRaised
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "This Month",
                fontSize = 13.sp,
                color = NudgeColors.ContentSecondary
            )
            Text(
                "₹$formatted",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "spent so far",
                fontSize = 12.sp,
                color = NudgeColors.ContentTertiary
            )
        }
    }
}

@Composable
fun BudgetRingsRow(
    budgets: List<com.nudge.android.data.BudgetEntity>,
    transactions: List<TransactionEntity>,
    categories: List<com.nudge.android.data.CategoryEntity>
) {
    if (budgets.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        budgets.forEach { budget ->
            val category = categories.find { it.id == budget.categoryId }
            val spent = transactions
                .filter { it.categoryId == budget.categoryId && it.type == "debit" }
                .sumOf { it.amountCents }
            val progress = (spent.toFloat() / budget.amountCents.toFloat()).coerceIn(0f, 1.5f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    CircularProgressIndicator(
                        progress = progress.coerceAtMost(1f),
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            progress > 1f -> NudgeColors.Negative
                            progress > 0.8f -> NudgeColors.Warning
                            else -> NudgeColors.AccentPrimary
                        },
                        strokeWidth = 4.dp,
                        trackColor = NudgeColors.ContentTertiary.copy(alpha = 0.2f)
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.ContentSecondary
                    )
                }
                Text(
                    category?.name ?: "All",
                    fontSize = 11.sp,
                    color = NudgeColors.ContentSecondary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StreakXpStrip(gamification: com.nudge.android.data.GamificationProfileEntity?) {
    if (gamification == null) return

    val levelProgress = GamificationMath.levelProgress(gamification.xpTotal)

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = NudgeColors.AccentPrimary.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Streak flame
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🔥",
                    fontSize = 20.sp
                )
                Text(
                    "${gamification.currentStreakDays}d",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.ContentPrimary
                )
                Text(
                    "streak",
                    fontSize = 10.sp,
                    color = NudgeColors.ContentTertiary
                )
            }

            // XP bar
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Level ${gamification.level}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.AccentPrimary
                    )
                    Text(
                        "${gamification.xpTotal} XP",
                        fontSize = 12.sp,
                        color = NudgeColors.ContentSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = levelProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NudgeColors.AccentPrimary,
                    trackColor = NudgeColors.AccentPrimary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun NeedsReviewTeaser(count: Int, onClick: () -> Unit) {
    if (count == 0) return

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = NudgeColors.Warning.copy(alpha = 0.1f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$count transaction${if (count > 1) "s" else ""} need review",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.Warning
            )
            Text("→", fontSize = 18.sp, color = NudgeColors.Warning)
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    isDark: Boolean,
    onDelete: () -> Unit
) {
    val isDebit = transaction.type == "debit"
    val formattedAmount = NumberFormat.getNumberInstance(Locale.getDefault())
        .format(transaction.amountCents / 100.0)

    Card(
        shape = RoundedCornerShape(NudgeRadius.MD),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NudgeColors.DarkSurfaceRaised else NudgeColors.SurfaceRaised
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
                    transaction.merchantRaw,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                )
                if (transaction.note != null) {
                    Text(
                        transaction.note,
                        fontSize = 12.sp,
                        color = NudgeColors.ContentTertiary
                    )
                }
            }
            Text(
                "${if (isDebit) "-" else "+"}₹$formattedAmount",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = if (isDebit) NudgeColors.Negative else NudgeColors.Positive
            )
        }
    }
}

@Composable
fun EmptyState() {
    Card(
        shape = RoundedCornerShape(NudgeRadius.XL),
        colors = CardDefaults.cardColors(
            containerColor = NudgeColors.AccentPrimary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "💰",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No transactions yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NudgeColors.ContentSecondary
            )
            Text(
                "Tap + to add your first entry",
                fontSize = 13.sp,
                color = NudgeColors.ContentTertiary
            )
        }
    }
}

// Shared radius values matching the design system
private object NudgeRadius {
    const val SM = 8
    const val MD = 14
    const val LG = 20
    const val XL = 28
}
