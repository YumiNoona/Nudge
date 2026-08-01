package com.nudge.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.*
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
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive && !it.isArchived } }
    val archivedAccounts = remember(accounts) { accounts.filter { it.isArchived } }
    var showArchived by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft())
            }
            Column(Modifier.weight(1f)) {
                Text("Wallets", style = DSTypography.headlineLarge, color = DSBridge.ink())
                Text("Swipe the stack to switch", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            }
            Surface(
                onClick = onAddAccount,
                shape = RoundedCornerShape(14.dp),
                color = DS.Signal
            ) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Plus(size = 17.dp, color = DS.InkPrimary)
                    Spacer(Modifier.width(5.dp))
                    Text("ADD", fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DS.InkPrimary)
                }
            }
        }

        if (activeAccounts.isEmpty()) {
            EmptyWalletState(onAddAccount, Modifier.weight(1f))
        } else {
            val pagerState = rememberPagerState(pageCount = { activeAccounts.size })

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 22.dp),
                pageSpacing = 14.dp,
                modifier = Modifier.fillMaxWidth().height(326.dp)
            ) { page ->
                val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val movement = offset.absoluteValue.coerceIn(0f, 1f)
                val account = activeAccounts[page]
                val spent = transactions.filter {
                    it.accountId == account.id && it.type.equals("debit", true)
                }.sumOf { it.amountCents }
                val balance = account.balanceCents ?: transactions.filter { it.accountId == account.id }.sumOf {
                    if (it.type.equals("credit", true)) it.amountCents else -it.amountCents
                }
                val color = accountColor(account)

                Box(Modifier.fillMaxSize().padding(top = 18.dp), contentAlignment = Alignment.TopCenter) {
                    // Two visible backs create a real card-stack silhouette.
                    for (depth in 2 downTo 1) {
                        val backAccount = activeAccounts[(page + depth).mod(activeAccounts.size)]
                        StackBack(
                            color = accountColor(backAccount),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = (depth * 12).dp)
                                .offset(y = (depth * 15).dp)
                                .scale(1f - depth * 0.035f)
                        )
                    }

                    WalletCard(
                        account = account,
                        balance = balance,
                        spent = spent,
                        color = color,
                        onSetDefault = onSetDefault,
                        onArchive = onArchive,
                        onEdit = onEdit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationZ = offset * -1.8f
                                rotationY = offset * 7f
                                cameraDistance = 18f * density
                                alpha = 1f - movement * 0.18f
                            }
                            .scale(1f - movement * 0.045f)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                activeAccounts.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateFloatAsState(if (selected) 22f else 6f, spring(), label = "walletDot")
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(width.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) DSBridge.accent() else DSBridge.inkMute().copy(alpha = .3f))
                    )
                }
            }

            Text(
                "${activeAccounts.size} ACTIVE ACCOUNT${if (activeAccounts.size == 1) "" else "S"}",
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = DSBridge.inkMute(),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)
            )
        }

        if (archivedAccounts.isNotEmpty()) {
            Surface(
                onClick = { showArchived = !showArchived },
                color = Color.Transparent,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Archived", style = DSTypography.titleMedium, color = DSBridge.ink(), modifier = Modifier.weight(1f))
                    Text("${archivedAccounts.size}", fontFamily = MonoFamily, color = DSBridge.inkMute())
                    Spacer(Modifier.width(6.dp))
                    Lucide.ChevronRight(
                        modifier = Modifier.graphicsLayer { rotationZ = if (showArchived) 90f else 0f },
                        size = 17.dp,
                        color = DSBridge.inkMute()
                    )
                }
            }
            AnimatedVisibility(showArchived) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(archivedAccounts, key = { it.id }) { account ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DSBridge.surface(),
                            modifier = Modifier.width(210.dp)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(account.name, color = DSBridge.ink(), modifier = Modifier.weight(1f))
                                TextButton(onClick = { onRestore(account.id) }) { Text("Restore") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(112.dp))
    }
}

@Composable
private fun StackBack(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.height(246.dp)
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = .18f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = .88f), DS.AccentDeep)))
    )
}

@Composable
private fun WalletCard(
    account: AccountEntity,
    balance: Long,
    spent: Long,
    color: Color,
    onSetDefault: (String) -> Unit,
    onArchive: (String) -> Unit,
    onEdit: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.height(246.dp)
            .shadow(22.dp, RoundedCornerShape(28.dp), spotColor = color.copy(alpha = .25f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF111512), DS.AccentDeep)))
    ) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(color))
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 34.dp, y = (-30).dp).size(150.dp)
                .clip(CircleShape).background(color.copy(alpha = .14f))
        )
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .09f)), contentAlignment = Alignment.Center) {
                    accountIcon(account.accountType, color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(accountLabel(account.accountType).uppercase(), fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = .8.sp, color = Color.White.copy(alpha = .56f))
                }
                if (account.isDefault) {
                    Text("DEFAULT", fontFamily = MonoFamily, fontSize = 8.sp, color = DS.Signal,
                        modifier = Modifier.clip(CircleShape).background(DS.Signal.copy(alpha = .12f)).padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }

            Spacer(Modifier.height(25.dp))
            Text("AVAILABLE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.3.sp, color = Color.White.copy(alpha = .5f))
            Text(formatCents(balance), style = DSTypography.displayMedium, fontFamily = MonoFamily, color = Color.White)
            Text("${formatCents(spent)} spent", fontFamily = MonoFamily, fontSize = 10.sp, color = Color.White.copy(alpha = .52f))

            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!account.isDefault) {
                    Text(
                        "MAKE DEFAULT",
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DS.Signal,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSetDefault(account.id) }
                            .background(DS.Signal.copy(alpha = .12f)).padding(horizontal = 11.dp, vertical = 8.dp)
                    )
                } else Spacer(Modifier.width(1.dp))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onEdit(account) }, modifier = Modifier.size(36.dp)) {
                    Lucide.Settings(size = 18.dp, color = Color.White.copy(alpha = .72f))
                }
                IconButton(onClick = { onArchive(account.id) }, modifier = Modifier.size(36.dp)) {
                    Lucide.Trash2(size = 18.dp, color = DS.Negative)
                }
            }
        }
    }
}

@Composable
private fun EmptyWalletState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(DSBridge.accentBg()), contentAlignment = Alignment.Center) {
                Lucide.Wallet(size = 30.dp, color = DSBridge.accent())
            }
            Spacer(Modifier.height(16.dp))
            Text("Build your wallet stack", style = DSTypography.headlineMedium, color = DSBridge.ink())
            Text("Add cash, bank, card or UPI accounts", style = DSTypography.bodyMedium, color = DSBridge.inkMute())
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent())) { Text("Add account") }
        }
    }
}

private fun accountColor(account: AccountEntity): Color = runCatching {
    val value = if (account.color.equals("#6366F1", true)) "#3E6F8E" else account.color ?: "#B9D8C3"
    Color(android.graphics.Color.parseColor(value))
}.getOrDefault(DS.DarkAccent)

private fun accountLabel(type: String) = when (type) {
    "credit_card" -> "Credit card"
    "savings" -> "Savings"
    "cash" -> "Cash"
    "upi" -> "UPI"
    else -> type.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun accountIcon(type: String, color: Color) {
    when (type) {
        "credit_card" -> Lucide.CreditCard(size = 21.dp, color = color)
        "savings" -> Lucide.Home(size = 21.dp, color = color)
        else -> Lucide.Wallet(size = 21.dp, color = color)
    }
}
