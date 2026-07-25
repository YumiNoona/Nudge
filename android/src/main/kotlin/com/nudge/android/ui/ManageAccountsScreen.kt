package com.nudge.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors
import java.util.Calendar

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
            .sumOf { if (it.type == "CREDIT") it.amountCents else -it.amountCents }
    }

    Column(modifier = Modifier.fillMaxSize().background(NudgeColors.Bone)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = NudgeColors.InkSoft)
            }
            Text("Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
            Spacer(Modifier.width(64.dp))
        }

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Lucide.Wallet(size = 48.dp, strokeWidth = 1.8.dp, color = NudgeColors.InkMute)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No accounts yet — tap + to add one",
                        fontSize = 14.sp,
                        color = NudgeColors.InkSoft,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    val balance = accountBalance(account.id)
                    AccountCard(
                        account = account,
                        balance = balance,
                        onClick = {
                            editingAccount = account
                            showSheet = true
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        Button(
            onClick = {
                editingAccount = null
                showSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
        ) {
            Lucide.Plus(size = 18.dp, strokeWidth = 2.dp, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add Account", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
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
                    colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Coral)
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
private fun AccountCard(
    account: AccountEntity,
    balance: Long,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "press")

    val tint = NudgeColors.parse(account.color, NudgeColors.Emerald)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                AccountIcon(account.accountType, tint, 22.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NudgeColors.Ink
                )
                Text(
                    account.accountType.replace("_", " ").replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = NudgeColors.InkSoft
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format("%,.2f", balance / 100.0)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) NudgeColors.Emerald else NudgeColors.Coral
                )
                Text(
                    "balance",
                    fontSize = 10.sp,
                    color = NudgeColors.InkMute
                )
            }
        }
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
private fun AccountEditSheet(
    account: AccountEntity?,
    onSave: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = account != null
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.accountType ?: "cash") }
    var last4 by remember { mutableStateOf(account?.last4Digits ?: "") }
    var selectedColor by remember { mutableStateOf(account?.color ?: "") }

    val accountTypes = listOf("cash", "savings", "credit_card", "debit_card", "upi")
    val displayNames = mapOf(
        "cash" to "Cash",
        "savings" to "Savings",
        "credit_card" to "Credit Card",
        "debit_card" to "Debit Card",
        "upi" to "UPI"
    )
    val presetColors = listOf(
        "#1FAE6A", "#5B8DEF", "#EF5DA8", "#F59E4B",
        "#8B5CF6", "#F43F5E"
    )
    val isCardType = selectedType == "credit_card" || selectedType == "debit_card"

    val isValid = name.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = NudgeColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NudgeColors.InkMute)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(20.dp))

            Text(
                if (isEditing) "Edit Account" else "Add Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NudgeColors.Ink
            )
            Spacer(Modifier.height(20.dp))

            // Name
            Text("Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.InkSoft)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(fontSize = 15.sp, color = NudgeColors.Ink),
                cursorBrush = SolidColor(NudgeColors.Emerald),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NudgeColors.Bone)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text("Account name", fontSize = 15.sp, color = NudgeColors.InkMute)
                    inner()
                }
            )
            Spacer(Modifier.height(16.dp))

            // Type
            Text("Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.InkSoft)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                accountTypes.forEach { type ->
                    val isSel = selectedType == type
                    val label = displayNames[type] ?: type
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedType = type },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AccountIcon(type, if (isSel) NudgeColors.Emerald else NudgeColors.InkSoft, 14.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeColors.EmeraldBg,
                            selectedLabelColor = NudgeColors.Emerald
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Last 4 digits
            if (isCardType) {
                Text("Last 4 digits (optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.InkSoft)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4 = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = NudgeColors.Ink, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    cursorBrush = SolidColor(NudgeColors.Emerald),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NudgeColors.Bone)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (last4.isEmpty()) Text("••••", fontSize = 15.sp, color = NudgeColors.InkMute)
                        inner()
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Color tag
            Text("Color tag (optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.InkSoft)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
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
                    val entity = AccountEntity(
                        id = account?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        bankName = account?.bankName,
                        accountType = selectedType,
                        last4Digits = last4.ifBlank { null },
                        color = selectedColor.ifBlank { null },
                        icon = account?.icon,
                        isActive = account?.isActive ?: true
                    )
                    onSave(entity)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald),
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
                    Text("Delete Account", color = NudgeColors.Coral, fontSize = 14.sp)
                }
            }
        }
    }
}
