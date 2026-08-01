package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.CategoryEntity
import com.nudge.android.ui.components.KeypadGrid
import com.nudge.android.ui.components.applyKeypadInput
import com.nudge.android.ui.components.ScrollableChipRow
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.DSSpace
import com.nudge.android.ui.theme.DSTypography
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.model.TransactionType
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAdd: (
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        accountId: String,
        categoryId: String?,
        note: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSBridge.surface(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .width(36.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DSBridge.inkMute().copy(alpha = 0.4f))
            )
        }
    ) {
        AddTransactionSheetContent(categories, accounts, onDismiss, onAdd)
    }
}

@Composable
private fun AddTransactionSheetContent(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onAdd: (
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        accountId: String,
        categoryId: String?,
        note: String?
    ) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    val selectableAccounts = remember(accounts) { accounts.filter { it.isActive && !it.isArchived } }

    LaunchedEffect(selectableAccounts) {
        if (selectedAccountId == null) {
            selectedAccountId = selectableAccounts.firstOrNull { it.isDefault }?.id ?: selectableAccounts.firstOrNull()?.id
        }
    }

    val cleaned = if (amountStr.endsWith(".")) amountStr.dropLast(1) else amountStr
    val amountCents = cleaned.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
    val isValid = amountCents > 0 && selectedAccountId != null

    val typeLabel = when (selectedType) {
        TransactionType.DEBIT -> "Expense"
        TransactionType.CREDIT -> "Income"
        TransactionType.REFUND -> "Refund"
        TransactionType.TRANSFER -> "Transfer"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add Transaction", style = DSTypography.headlineMedium, color = DSBridge.ink(), modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Lucide.X(size = 18.dp, strokeWidth = 2.dp, color = DSBridge.inkMute())
            }
        }
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Amount — read-only display + custom keypad below
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DSBridge.accent())
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (amountStr.isEmpty()) "0" else amountStr,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MonoFamily,
                color = if (amountStr.isEmpty()) DSBridge.inkMute() else DSBridge.ink(),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Keypad
        KeypadGrid(
            onKey = { key -> amountStr = applyKeypadInput(amountStr, key) },
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Type
        ScrollableChipRow(
            items = TransactionType.entries.toList(),
            selected = selectedType,
            onSelect = {
                selectedType = it
                selectedCategoryId = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { t ->
                Text(
                    when (t) {
                        TransactionType.DEBIT -> "Expense"
                        TransactionType.CREDIT -> "Income"
                        TransactionType.REFUND -> "Refund"
                        TransactionType.TRANSFER -> "Transfer"
                    },
                    fontSize = 12.sp
                )
            }
        )
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Merchant
        BasicTextField(
            value = merchant, onValueChange = { merchant = it },
            textStyle = TextStyle(fontSize = 15.sp, color = DSBridge.ink()),
            cursorBrush = SolidColor(DSBridge.accent()),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSBridge.background())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner -> if (merchant.isEmpty()) Text("What was this for? (optional)", fontSize = 15.sp, color = DSBridge.inkMute()); inner() }
        )
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Category grid
        Text("Category", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
        Spacer(modifier = Modifier.height(DSSpace.sm))

        val categoryType = if (selectedType == TransactionType.CREDIT) "income" else "expense"
        val expenseCats = categories.filter { it.type == categoryType }
        if (expenseCats.isNotEmpty()) {
            expenseCats.chunked(4).forEach { rowCats ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCats.forEach { cat ->
                        val isSel = selectedCategoryId == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) DSBridge.accentBg() else DSBridge.background())
                                .then(if (isSel) Modifier.border(1.5.dp, DSBridge.accent(), RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { selectedCategoryId = cat.id }
                                .padding(vertical = 10.dp)
                        ) {
                            CategoryIcon(cat.name, if (isSel) DSBridge.accent() else DSBridge.inkSoft(), 18.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(cat.name, fontSize = 9.sp, color = if (isSel) DSBridge.accent() else DSBridge.inkSoft(), maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                    repeat(4 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(DSSpace.sm))

        // Account
        Text("Account", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
        Spacer(modifier = Modifier.height(DSSpace.sm))
        if (selectableAccounts.isEmpty()) {
            Text(
                "No accounts yet — add one from More → Manage Accounts",
                fontSize = 12.sp, color = DSBridge.inkMute()
            )
        } else {
            selectableAccounts.forEach { a ->
                val isSel = selectedAccountId == a.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) DSBridge.accentBg() else DSBridge.background())
                        .then(if (isSel) Modifier.border(1.5.dp, DSBridge.accent(), RoundedCornerShape(12.dp)) else Modifier)
                        .clickable { selectedAccountId = a.id }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    AccountTypeIcon(a.accountType, if (isSel) DSBridge.accent() else DSBridge.inkSoft(), 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(a.name, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal, color = if (isSel) DSBridge.accent() else DSBridge.ink(), modifier = Modifier.weight(1f))
                    if (a.isDefault) {
                        Text("Default", fontSize = 10.sp, color = DSBridge.inkMute())
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(DSSpace.sm))

        // Note
        BasicTextField(
            value = note, onValueChange = { note = it },
            textStyle = TextStyle(fontSize = 13.sp, color = DSBridge.ink()),
            cursorBrush = SolidColor(DSBridge.accent()),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DSBridge.background())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { inner -> if (note.isEmpty()) Text("Add a note (optional)", fontSize = 13.sp, color = DSBridge.inkMute()); inner() }
        )
        Spacer(modifier = Modifier.height(DSSpace.lg))

        // Submit
        Button(
            onClick = {
                val accountId = selectedAccountId
                if (accountId != null) {
                    val description = merchant.trim().ifBlank { "Manual $typeLabel" }
                    onAdd(amountCents, selectedType, description, accountId, selectedCategoryId, note.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent()),
            enabled = isValid
        ) {
            Text("Add $typeLabel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategoryIcon(name: String, tint: Color, size: Dp = 18.dp) {
    val icon = when {
        name.contains("Food", true) || name.contains("Dining", true) -> { @Composable { Lucide.ShoppingCart(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Transport", true) -> { @Composable { Lucide.ChevronRight(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Groceries", true) -> { @Composable { Lucide.ShoppingCart(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Shopping", true) -> { @Composable { Lucide.ShoppingCart(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Entertainment", true) -> { @Composable { Lucide.Sparkles(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Utilities", true) -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Rent", true) || name.contains("Housing", true) -> { @Composable { Lucide.Home(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Health", true) -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Education", true) -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Subscription", true) -> { @Composable { Lucide.Bell(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Travel", true) -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Personal", true) -> { @Composable { Lucide.User(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Gift", true) -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Invest", true) -> { @Composable { Lucide.TrendingUp(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Salary", true) || name.contains("Income", true) -> { @Composable { Lucide.Wallet(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Freelance", true) -> { @Composable { Lucide.User(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Interest", true) -> { @Composable { Lucide.TrendingUp(size = size, strokeWidth = 1.6.dp, color = tint) } }
        name.contains("Refund", true) -> { @Composable { Lucide.RefreshCw(size = size, strokeWidth = 1.6.dp, color = tint) } }
        else -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
    }
    icon()
}

@Composable
private fun AccountTypeIcon(accountType: String, tint: Color, size: Dp = 14.dp) {
    val icon = when {
        accountType.contains("cash", true) -> { @Composable { Lucide.Wallet(size = size, strokeWidth = 1.6.dp, color = tint) } }
        accountType.contains("savings", true) -> { @Composable { Lucide.Home(size = size, strokeWidth = 1.6.dp, color = tint) } }
        accountType.contains("credit_card", true) || accountType.contains("debit_card", true) || accountType.contains("card", true) -> { @Composable { Lucide.CreditCard(size = size, strokeWidth = 1.6.dp, color = tint) } }
        accountType.contains("upi", true) -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
        accountType.contains("wallet", true) -> { @Composable { Lucide.Wallet(size = size, strokeWidth = 1.6.dp, color = tint) } }
        else -> { @Composable { Lucide.Tag(size = size, strokeWidth = 1.6.dp, color = tint) } }
    }
    icon()
}
