package com.nudge.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.android.ui.theme.DS
import com.nudge.android.ui.theme.formatCents
import com.nudge.android.ui.components.FloatingActionCube
import com.nudge.android.ui.theme.NudgeHaptics
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    accounts: List<AccountEntity>,
    transactions: List<TransactionEntity>,
    onAdd: (AccountEntity) -> Unit,
    onUpdate: (AccountEntity) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<AccountEntity?>(null) }

    val now = Calendar.getInstance()
    val currentMonth = now.get(Calendar.MONTH)
    val currentYear = now.get(Calendar.YEAR)

    fun accountBalance(accountId: String): Long {
        return transactions
            .filter { it.accountId == accountId }
            .filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestampEpoch }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }
            .sumOf { if (it.type.equals("credit", ignoreCase = true)) it.amountCents else -it.amountCents }
    }
    val visibleAccounts = accounts.filter { !it.isArchived }
    val totalBalance = visibleAccounts.sumOf { accountBalance(it.id) }

    Box(modifier = Modifier.fillMaxSize().background(Nc.background).statusBarsPadding()) {
    Column(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Lucide.ArrowLeft(size = 22.dp, color = Nc.inkSoft) }
            Text("Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Nc.ink)
            Spacer(Modifier.width(48.dp))
        }

        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp)).background(DS.AccentDeep).padding(20.dp)
        ) {
            Column {
                Text("TOTAL ACROSS ACCOUNTS", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.1.sp, color = Color.White.copy(alpha = .55f))
                Spacer(Modifier.height(5.dp))
                Text(formatCents(totalBalance), fontFamily = MonoFamily, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(13.dp))
                Text("${visibleAccounts.size} ACTIVE", fontFamily = MonoFamily, fontSize = 10.sp, color = Color.White.copy(alpha = .62f))
            }
            Box(Modifier.align(Alignment.TopEnd).size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                Lucide.Wallet(size = 21.dp, color = DS.Signal)
            }
        }

        if (visibleAccounts.isNotEmpty()) {
            AccountStackPreview(
                accounts = visibleAccounts,
                balanceFor = ::accountBalance,
                onOpen = { account -> editingAccount = account; showSheet = true }
            )
        }

        if (visibleAccounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Lucide.Wallet(size = 48.dp, strokeWidth = 1.8.dp, color = Nc.inkMute)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No accounts yet — tap + to add one",
                        fontSize = 14.sp,
                        color = Nc.inkSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

    }
        FloatingActionCube(
            contentDescription = "Add account",
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp),
            onClick = {
            editingAccount = null
            showSheet = true
            },
        ) { Lucide.Plus(size = 24.dp, color = DS.InkPrimary) }
    }

    if (showSheet) {
        AccountEditSheet(
            account = editingAccount,
            onSave = { account ->
                if (editingAccount != null) onUpdate(account) else onAdd(account)
                showSheet = false
            },
            onDelete = { account ->
                showSheet = false
                showDeleteDialog = account
            },
            onDismiss = { showSheet = false }
        )
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Account", fontWeight = FontWeight.SemiBold) },
            text = { Text("Are you sure you want to delete \"${showDeleteDialog?.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog?.let { onDelete(it.id) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Nc.negative)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AccountStackPreview(
    accounts: List<AccountEntity>,
    balanceFor: (String) -> Long,
    onOpen: (AccountEntity) -> Unit
) {
    var frontIndex by remember(accounts.map { it.id }) { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = remember(context) { NudgeHaptics(context) }
    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(194.dp).padding(horizontal = 34.dp)
                .pointerInput(accounts.map { it.id }, frontIndex) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val threshold = 54.dp.toPx()
                                val animator = Animatable(dragX)
                                if (abs(dragX) > threshold) {
                                    val direction = if (dragX < 0f) 1 else -1
                                    val exit = if (dragX < 0f) -size.width * 1.1f else size.width * 1.1f
                                    animator.animateTo(exit, tween(190, easing = FastOutSlowInEasing)) { dragX = value }
                                    frontIndex = (frontIndex + direction).mod(accounts.size)
                                    dragX = 0f
                                    haptics.impactLight()
                                } else {
                                    animator.animateTo(0f, spring(stiffness = 620f, dampingRatio = .74f)) { dragX = value }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                Animatable(dragX).animateTo(0f, spring(stiffness = 620f, dampingRatio = .74f)) { dragX = value }
                            }
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragX = (dragX + amount).coerceIn(-size.width * .62f, size.width * .62f)
                        },
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            val page = frontIndex
            val account = accounts[page]
            val depth = (abs(dragX) / 280f).coerceIn(0f, 1f)
            val tint = NudgeColors.parse(account.color, DS.Accent)
            Box(Modifier.fillMaxWidth().height(184.dp).padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                val backCount = minOf(2, accounts.size - 1)
                val revealDirection = if (dragX > 0f) -1 else 1
                for (stackDepth in backCount downTo 1) {
                    val backAccount = accounts[(page + revealDirection * stackDepth).mod(accounts.size)]
                    val backTint = NudgeColors.parse(backAccount.color, DS.Accent)
                    Box(
                        Modifier.fillMaxWidth().height(146.dp)
                            .graphicsLayer {
                                val animatedDepth = (stackDepth - depth).coerceAtLeast(0f)
                                scaleX = 1f - animatedDepth * .035f
                                scaleY = 1f - animatedDepth * .035f
                                translationY = animatedDepth * 11.dp.toPx()
                            }
                            .zIndex((2 - stackDepth).toFloat())
                            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = backTint.copy(alpha = .18f))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(backTint.copy(alpha = .82f), DS.AccentDeep)))
                            .padding(19.dp),
                    ) {
                        AccountCardContent(backAccount, balanceFor(backAccount.id), compact = true)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(146.dp)
                        .zIndex(3f)
                        .graphicsLayer {
                            scaleX = 1f - depth * .07f
                            scaleY = 1f - depth * .07f
                            translationX = dragX
                            rotationZ = dragX / 95f
                            translationY = depth * 5.dp.toPx()
                            alpha = 1f - depth * .10f
                        }
                        .shadow(18.dp, RoundedCornerShape(24.dp), spotColor = tint.copy(alpha = .26f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(tint.copy(alpha = .96f), DS.AccentDeep)))
                        .clickable { onOpen(account) }
                        .padding(19.dp),
                ) {
                    AccountCardContent(account, balanceFor(account.id))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            accounts.indices.forEach { index ->
                val selected = frontIndex == index
                Box(
                    Modifier.padding(horizontal = 3.dp).width(if (selected) 18.dp else 6.dp).height(5.dp)
                        .clip(CircleShape).background(if (selected) Nc.accent else Nc.inkMute.copy(alpha = .25f)),
                )
            }
        }
    }
}

@Composable
private fun AccountCardContent(account: AccountEntity, balance: Long, compact: Boolean = false) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountIcon(account.accountType, Color.White, if (compact) 18.dp else 20.dp)
            Spacer(Modifier.width(9.dp))
            Text(account.name, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (account.isDefault) Text("DEFAULT", fontFamily = MonoFamily, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DS.Signal)
        }
        Spacer(Modifier.height(if (compact) 13.dp else 18.dp))
        Text(formatCents(balance), fontFamily = MonoFamily, fontSize = if (compact) 20.sp else 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            listOfNotNull(account.bankName, account.last4Digits?.let { "•••• $it" }).joinToString(" · ").ifBlank { account.accountType.replace('_', ' ').uppercase() },
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = .64f),
            maxLines = 1,
        )
    }
}

@Composable
private fun AccountIcon(accountType: String, tint: Color, size: androidx.compose.ui.unit.Dp) {
    when {
        accountType.contains("cash", true) -> Lucide.Wallet(size = size, strokeWidth = 1.8.dp, color = tint)
        accountType.contains("savings", true) -> Lucide.Home(size = size, strokeWidth = 1.8.dp, color = tint)
        accountType.contains("credit_card", true) || accountType.contains("debit_card", true) || accountType.contains("card", true) -> Lucide.CreditCard(size = size, strokeWidth = 1.8.dp, color = tint)
        accountType.contains("upi", true) -> Lucide.Tag(size = size, strokeWidth = 1.8.dp, color = tint)
        accountType.contains("wallet", true) -> Lucide.Wallet(size = size, strokeWidth = 1.8.dp, color = tint)
        else -> Lucide.Wallet(size = size, strokeWidth = 1.8.dp, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountEditSheet(
    account: AccountEntity?,
    onSave: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = account != null
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.accountType ?: "cash") }
    var last4 by remember { mutableStateOf(account?.last4Digits ?: "") }
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var selectedColor by remember { mutableStateOf(account?.color ?: "") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    var showScanner by remember { mutableStateOf(false) }

    val accountTypes = listOf("cash", "savings", "credit_card", "debit_card", "upi")
    val displayNames = mapOf(
        "cash" to "Cash",
        "savings" to "Savings",
        "credit_card" to "Credit Card",
        "debit_card" to "Debit Card",
        "upi" to "UPI"
    )
    val presetColors = listOf("#365244", "#5D826C", "#149A8B", "#E38B42", "#3E6F8E", "#C65D4B")
    val isCardType = selectedType == "credit_card" || selectedType == "debit_card"

    val isValid = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Nc.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Nc.inkMute)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(20.dp))

            Text(
                if (isEditing) "Edit Account" else "Add Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Nc.ink
            )
            Spacer(Modifier.height(20.dp))

            // Name
            Text("Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Nc.inkSoft)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(fontSize = 15.sp, color = Nc.ink),
                cursorBrush = SolidColor(Nc.accent),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Nc.background)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text("Account name", fontSize = 15.sp, color = Nc.inkMute)
                    inner()
                }
            )
            Spacer(Modifier.height(16.dp))

            // Type
            Text("Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Nc.inkSoft)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                accountTypes.forEach { type ->
                    val isSel = selectedType == type
                    val label = displayNames[type] ?: type
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedType = type },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AccountIcon(type, if (isSel) Nc.accent else Nc.inkSoft, 14.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Nc.accentBg,
                            selectedLabelColor = Nc.accent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Last 4 digits
            if (isCardType) {
                Text("Last 4 digits (optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Nc.inkSoft)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4 = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = Nc.ink, fontFamily = MonoFamily),
                    cursorBrush = SolidColor(Nc.accent),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Nc.background)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (last4.isEmpty()) Text("••••", fontSize = 15.sp, color = Nc.inkMute)
                        inner()
                    }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Lucide.Camera(size = 19.dp, color = Nc.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(if (last4.isBlank()) "Scan card" else "Scan again")
                    Spacer(Modifier.weight(1f))
                    Text("ON-DEVICE", fontFamily = MonoFamily, fontSize = 8.sp, color = Nc.inkMute)
                }
                if (bankName.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text("$bankName · •••• $last4", style = MaterialTheme.typography.bodySmall, color = Nc.inkSoft)
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Default account", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Nc.ink)
                    Text("Preselect it when adding expenses", fontSize = 11.sp, color = Nc.inkMute)
                }
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }
            Spacer(Modifier.height(16.dp))

            // Color tag
            Text("Color tag (optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Nc.inkSoft)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                presetColors.forEach { colorHex ->
                    val color = NudgeColors.parse(colorHex)
                    val isSel = selectedColor == colorHex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.18f))
                            .then(
                                if (isSel) Modifier.border(2.5.dp, color, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = if (isSel) "" else colorHex },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            // Save
            Button(
                onClick = {
                    val entity = account?.copy(
                        name = name.trim(),
                        accountType = selectedType,
                        bankName = bankName.ifBlank { null },
                        last4Digits = last4.ifBlank { null },
                        color = selectedColor.ifBlank { null },
                        isDefault = isDefault
                    ) ?: AccountEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        bankName = bankName.ifBlank { null },
                        accountType = selectedType,
                        last4Digits = last4.ifBlank { null },
                        color = selectedColor.ifBlank { null },
                        isDefault = isDefault
                    )
                    onSave(entity)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Nc.accent),
                enabled = isValid
            ) {
                Text(
                    if (isEditing) "Save Changes" else "Add Account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isEditing) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { onDelete(account) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Account", color = Nc.negative, fontSize = 14.sp)
                }
            }
        }
    }

    if (showScanner) {
        CardScannerDialog(
            onDismiss = { showScanner = false },
            onScanned = { result ->
                last4 = result.last4
                bankName = result.network + (result.expiry?.let { " · $it" } ?: "")
                if (name.isBlank()) name = "${result.network} •••• ${result.last4}"
                showScanner = false
            }
        )
    }
}
