package com.nudge.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WalletCarouselScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onAddAccount: () -> Unit,
    onSetDefault: (String) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onEdit: (AccountEntity) -> Unit,
    onBack: () -> Unit
) {
    val activeAccounts by remember(accounts) { derivedStateOf { accounts.filter { !it.isArchived } } }
    val archivedAccounts by remember(accounts) { derivedStateOf { accounts.filter { it.isArchived } } }
    var showArchived by remember { mutableStateOf(false) }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().background(NudgeColors.Bone)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = NudgeColors.InkSoft) }
            Text("Wallets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
            Spacer(Modifier.width(64.dp))
        }

        if (activeAccounts.isEmpty() && archivedAccounts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Lucide.Wallet(size = 48.dp, strokeWidth = 1.5.dp, color = NudgeColors.InkMute)
                    Spacer(Modifier.height(12.dp))
                    Text("No wallets yet", fontSize = 16.sp, color = NudgeColors.InkSoft)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onAddAccount, colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)) { Text("+ Add Wallet") }
                }
            }
            return@Column
        }

        // Carousel
        val pagerState = rememberPagerState(pageCount = { activeAccounts.size + 1 }) // +1 for ghost add card
        val scope = rememberCoroutineScope()

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(280.dp),
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val scale by animateFloatAsState(1f - (pageOffset.absoluteValue * 0.06f).coerceAtMost(0.12f), label = "cardScale")
                val alpha by animateFloatAsState(1f - (pageOffset.absoluteValue * 0.3f).coerceAtMost(0.4f), label = "cardAlpha")
                val elevation by animateDpAsState(
                    if (pageOffset.absoluteValue < 0.5f) 8.dp else 2.dp,
                    label = "cardElevation"
                )

                if (page < activeAccounts.size) {
                    val account = activeAccounts[page]
                    val spent = transactions
                        .filter { it.accountId == account.id && it.type == "debit" }
                        .sumOf { it.amountCents }
                    val cardColor = try { Color(android.graphics.Color.parseColor(account.color ?: "#10B981")) } catch (_: Exception) { NudgeColors.Emerald }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .scale(scale)
                            .graphicsLayer { this.alpha = alpha }
                            .shadow(elevation, RoundedCornerShape(24.dp), spotColor = cardColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(cardColor.copy(alpha = 0.08f), Color.Transparent)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            // Top row: icon + default badge + menu
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                                            .background(cardColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = when (account.accountType) {
                                            "credit_card" -> { @Composable { Lucide.CreditCard(size = 20.dp, strokeWidth = 1.8.dp, color = cardColor) } }
                                            "savings" -> { @Composable { Lucide.Home(size = 20.dp, strokeWidth = 1.8.dp, color = cardColor) } }
                                            "cash" -> { @Composable { Lucide.Wallet(size = 20.dp, strokeWidth = 1.8.dp, color = cardColor) } }
                                            else -> { @Composable { Lucide.Wallet(size = 20.dp, strokeWidth = 1.8.dp, color = cardColor) } }
                                        }
                                        icon()
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(account.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                                        Text(
                                            when (account.accountType) {
                                                "credit_card" -> "Credit Card"
                                                "savings" -> "Savings"
                                                "cash" -> "Cash"
                                                "upi" -> "UPI"
                                                else -> account.accountType.replaceFirstChar { it.uppercase() }
                                            },
                                            fontSize = 12.sp, color = NudgeColors.InkMute
                                        )
                                    }
                                }
                                if (account.isDefault) {
                                    Surface(shape = RoundedCornerShape(50), color = NudgeColors.EmeraldBg) {
                                        Text("Default", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Emerald)
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Balance
                            if (account.balanceCents != null) {
                                Text(
                                    "₹${fmt.format(account.balanceCents / 100)}",
                                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace, color = NudgeColors.Ink
                                )
                                Text("Current balance", fontSize = 11.sp, color = NudgeColors.InkMute)
                            } else {
                                Text(
                                    "₹${fmt.format(spent / 100)}",
                                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace, color = NudgeColors.Ink
                                )
                                Text("Spent this month", fontSize = 11.sp, color = NudgeColors.InkMute)
                            }

                            if (account.last4Digits != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("•••• ${account.last4Digits}", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = NudgeColors.InkMute)
                            }

                            Spacer(Modifier.height(16.dp))

                            // Actions row
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!account.isDefault) {
                                    Surface(
                                        onClick = { onSetDefault(account.id) },
                                        shape = RoundedCornerShape(12.dp), color = NudgeColors.EmeraldBg
                                    ) {
                                        Text("Set Default", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.Emerald)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { onEdit(account) }) {
                                    Lucide.Settings(size = 14.dp, strokeWidth = 1.5.dp, color = NudgeColors.InkSoft)
                                }
                                TextButton(onClick = { onArchive(account.id) }) {
                                    Lucide.Trash2(size = 14.dp, strokeWidth = 1.5.dp, color = NudgeColors.Coral)
                                }
                            }
                        }
                    }
                } else {
                    // Ghost "add wallet" card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .scale(scale)
                            .graphicsLayer { this.alpha = alpha },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .border(2.dp, NudgeColors.Emerald.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .clickable { onAddAccount() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Lucide.Plus(size = 32.dp, strokeWidth = 1.5.dp, color = NudgeColors.Emerald)
                                Spacer(Modifier.height(8.dp))
                                Text("Add Wallet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.Emerald)
                            }
                        }
                    }
                }
            }
        }

        // Archived link
        if (archivedAccounts.isNotEmpty()) {
            TextButton(
                onClick = { showArchived = !showArchived },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            ) {
                Text("Archived (${archivedAccounts.size}) ${if (showArchived) "▲" else "▼"}", fontSize = 13.sp, color = NudgeColors.InkMute)
            }

            if (showArchived) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    archivedAccounts.forEach { a ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface.copy(alpha = 0.6f))
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(a.name, fontSize = 14.sp, color = NudgeColors.Ink)
                                TextButton(onClick = { onRestore(a.id) }) {
                                    Text("Restore", fontSize = 12.sp, color = NudgeColors.Emerald)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom nav spacer
        Spacer(Modifier.height(80.dp))
    }
}
