package com.nudge.android.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
fun HomeScreen(
    viewModel: MainViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToMore: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToCharts: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onAddTransaction: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val needsReviewCount by viewModel.needsReviewCount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val context = LocalContext.current
    val profilePrefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
    val displayName = profilePrefs.getString("display_name", "You") ?: "You"
    val profilePhotoPath = profilePrefs.getString("profile_photo_path", null)
    val profileBitmap = remember(profilePhotoPath) {
        profilePhotoPath?.let(BitmapFactory::decodeFile)
    }

    val now = remember { Calendar.getInstance() }
    val monthTransactions = remember(transactions) {
        transactions.filter {
            val date = Calendar.getInstance().apply { timeInMillis = it.timestampEpoch }
            date.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }
    }
    val spent = monthTransactions.filter { it.type == "debit" }.sumOf { it.amountCents }
    val income = monthTransactions.filter { it.type == "credit" }.sumOf { it.amountCents }
    val balance = income - spent
    val budget = budgets.sumOf { it.amountCents }
    val budgetProgress = if (budget > 0) (spent.toFloat() / budget).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(budgetProgress, spring(dampingRatio = .82f), label = "budgetProgress")
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Column(
        Modifier.fillMaxSize().background(DSBridge.background()).verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(bottom = 116.dp)
    ) {
        HomeHeader(displayName, profileBitmap, isDark, needsReviewCount, onToggleTheme, onNavigateToReview, onNavigateToMore)

        AnimatedVisibility(
            visible = entered,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
        ) {
            BalancePanel(
                balance = balance,
                income = income,
                spent = spent,
                budget = budget,
                budgetProgress = animatedProgress,
                onAdd = onAddTransaction,
                onReview = onNavigateToReview,
                onWallet = onNavigateToWallet
            )
        }

        if (needsReviewCount > 0) {
            ReviewPrompt(needsReviewCount, onNavigateToReview)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard("Spent", formatCents(spent), "this month", DSBridge.negative(), Modifier.weight(1f), onNavigateToCharts)
            MetricCard(
                "Budget",
                if (budget > 0) "${(budgetProgress * 100).toInt()}%" else "Set up",
                if (budget > 0) "${formatCents((budget - spent).coerceAtLeast(0))} left" else "stay on track",
                DSBridge.accent(),
                Modifier.weight(1f),
                onNavigateToBudgets
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recent activity", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
                Text("Your latest money moves", fontSize = 12.sp, color = DSBridge.inkMute())
            }
            Surface(onClick = onNavigateToTransactions, color = Color.Transparent) {
                Text("See all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.accent(), modifier = Modifier.padding(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        if (transactions.isEmpty()) {
            EmptyTransactions(onAddTransaction)
        } else {
            Surface(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = DSBridge.surface(),
                shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    transactions.take(8).forEachIndexed { index, transaction ->
                        TransactionLine(transaction, categories.find { it.id == transaction.categoryId })
                        if (index < minOf(transactions.size, 8) - 1) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(DSBridge.inkMute().copy(alpha = .1f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    name: String,
    profileBitmap: android.graphics.Bitmap?,
    isDark: Boolean,
    reviewCount: Int,
    onTheme: () -> Unit,
    onReview: () -> Unit,
    onProfile: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(DSBridge.accent()).clickable(onClick = onProfile),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hi, $name", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
            Text("Here is your money today", fontSize = 11.sp, color = DSBridge.inkMute())
        }
        IconButton(onClick = onTheme) {
            if (isDark) Lucide.Sun(size = 20.dp, strokeWidth = 1.8.dp, color = DSBridge.inkSoft())
            else Lucide.Moon(size = 20.dp, strokeWidth = 1.8.dp, color = DSBridge.inkSoft())
        }
        Box {
            IconButton(onClick = onReview) {
                Lucide.Bell(size = 20.dp, strokeWidth = 1.8.dp, color = DSBridge.inkSoft())
            }
            if (reviewCount > 0) Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 4.dp).size(16.dp)
                    .clip(CircleShape).background(DS.Signal),
                contentAlignment = Alignment.Center
            ) { Text(reviewCount.coerceAtMost(9).toString(), color = DS.InkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BalancePanel(
    balance: Long,
    income: Long,
    spent: Long,
    budget: Long,
    budgetProgress: Float,
    onAdd: () -> Unit,
    onReview: () -> Unit,
    onWallet: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = .12f)),
        shape = RoundedCornerShape(28.dp),
        color = DS.AccentDeep
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AVAILABLE BALANCE", fontSize = 10.sp, letterSpacing = 1.2.sp, color = Color.White.copy(alpha = .62f))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (balance < 0) "−${formatCents(-balance)}" else formatCents(balance),
                        fontFamily = MonoFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .1f)) {
                    Lucide.Wallet(modifier = Modifier.padding(13.dp), size = 22.dp, strokeWidth = 1.7.dp, color = Color.White)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BalanceStat("Income", income, DS.Signal)
                BalanceStat("Spent", spent, Color(0xFFFFA69A))
            }
            if (budget > 0) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Monthly plan", color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${(budgetProgress * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = .13f))) {
                    Box(Modifier.fillMaxWidth(budgetProgress).fillMaxHeight().clip(CircleShape).background(DS.Signal))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelAction("Add expense", { Lucide.Plus(size = 18.dp, strokeWidth = 2.dp, color = DS.InkPrimary) }, true, Modifier.weight(1f), onAdd)
                PanelAction("Review", { Lucide.ListTodo(size = 18.dp, strokeWidth = 1.8.dp, color = Color.White) }, false, Modifier.weight(1f), onReview)
                PanelAction("Wallet", { Lucide.Wallet(size = 18.dp, strokeWidth = 1.8.dp, color = Color.White) }, false, Modifier.weight(1f), onWallet)
            }
        }
    }
}

@Composable private fun BalanceStat(label: String, value: Long, color: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = .55f), fontSize = 10.sp)
        Text(formatCents(value), color = color, fontFamily = MonoFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun PanelAction(label: String, icon: @Composable () -> Unit, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = if (primary) DS.Signal else Color.White.copy(alpha = .1f)) {
        Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            icon(); Spacer(Modifier.height(4.dp)); Text(label, color = if (primary) DS.InkPrimary else Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable private fun ReviewPrompt(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DSBridge.accentBg()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(DS.Signal), contentAlignment = Alignment.Center) {
                Text(count.coerceAtMost(99).toString(), fontWeight = FontWeight.Bold, color = DS.InkPrimary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Quick review", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DSBridge.ink())
                Text("$count transaction${if (count == 1) "" else "s"} need a category", fontSize = 11.sp, color = DSBridge.inkSoft())
            }
            Text("Swipe →", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.accent())
        }
    }
}

@Composable private fun MetricCard(title: String, value: String, detail: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp), color = DSBridge.surface()) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.height(14.dp))
            Text(title, fontSize = 10.sp, color = DSBridge.inkMute())
            Text(value, fontFamily = MonoFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DSBridge.ink())
            Text(detail, fontSize = 10.sp, color = DSBridge.inkSoft())
        }
    }
}

@Composable private fun TransactionLine(transaction: TransactionEntity, category: CategoryEntity?) {
    val debit = transaction.type == "debit"
    val date = remember(transaction.timestampEpoch) { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.timestampEpoch)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(DSBridge.surfaceVariant()), contentAlignment = Alignment.Center) {
            Text(category?.icon ?: "•", fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.merchantNormalized ?: transaction.merchantRaw, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.ink(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${category?.name ?: "Uncategorized"} · $date", fontSize = 10.sp, color = DSBridge.inkMute())
        }
        Text(
            "${if (debit) "−" else "+"}${formatCents(transaction.amountCents)}",
            fontFamily = MonoFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (debit) DSBridge.ink() else DSBridge.positive()
        )
    }
}

@Composable private fun EmptyTransactions(onAdd: () -> Unit) {
    Surface(onClick = onAdd, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = DSBridge.surface()) {
        Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Lucide.Wallet(size = 28.dp, strokeWidth = 1.5.dp, color = DSBridge.inkMute())
            Spacer(Modifier.height(10.dp))
            Text("Your activity will appear here", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
            Text("Add one manually or enable automatic capture", fontSize = 10.sp, color = DSBridge.inkMute())
        }
    }
}
