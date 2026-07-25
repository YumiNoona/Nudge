package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.CategoryEntity
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.model.TransactionType

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
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }

    val amountCents = amountStr.filter { it.isDigit() }.toLongOrNull()?.times(100L) ?: 0L
    val isValid = amountCents > 0 && merchant.isNotBlank() && selectedAccountId != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NudgeColors.Surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(24.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(36.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NudgeColors.InkMute)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text("Add Transaction", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
        Spacer(modifier = Modifier.height(20.dp))

        // Amount
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₹", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Emerald)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(NudgeColors.Emerald),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (amountStr.isEmpty()) Text("0", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeColors.InkMute, fontFamily = FontFamily.Monospace)
                    inner()
                },
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Type
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionType.entries.forEach { t ->
                val isSel = selectedType == t
                FilterChip(
                    selected = isSel, onClick = { selectedType = t },
                    label = { Text(when(t) { TransactionType.DEBIT -> "Expense"; TransactionType.CREDIT -> "Income"; else -> t.name }, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeColors.EmeraldBg,
                        selectedLabelColor = NudgeColors.Emerald
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Merchant
        BasicTextField(
            value = merchant, onValueChange = { merchant = it },
            textStyle = TextStyle(fontSize = 15.sp, color = NudgeColors.Ink),
            cursorBrush = SolidColor(NudgeColors.Emerald),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NudgeColors.Bone)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner -> if (merchant.isEmpty()) Text("What was this for?", fontSize = 15.sp, color = NudgeColors.InkMute); inner() }
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Category grid
        Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.InkSoft)
        Spacer(modifier = Modifier.height(8.dp))

        val expenseCats = categories.filter { it.type == "expense" }
        if (expenseCats.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), modifier = Modifier.height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenseCats) { cat ->
                    val isSel = selectedCategoryId == cat.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) NudgeColors.EmeraldBg else NudgeColors.Bone)
                            .then(if (isSel) Modifier.border(1.5.dp, NudgeColors.Emerald, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { selectedCategoryId = cat.id }
                            .padding(8.dp)
                    ) {
                        CategoryIcon(cat.name, if (isSel) NudgeColors.Emerald else NudgeColors.InkSoft, 16.dp)
                        Text(cat.name, fontSize = 9.sp, color = if (isSel) NudgeColors.Emerald else NudgeColors.InkSoft, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Account
        Text("Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NudgeColors.InkSoft)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.take(4).forEach { a ->
                val isSel = selectedAccountId == a.id
                FilterChip(
                    selected = isSel, onClick = { selectedAccountId = a.id },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccountTypeIcon(a.accountType, if (isSel) NudgeColors.Emerald else NudgeColors.InkSoft, 12.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(a.name, fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeColors.EmeraldBg,
                        selectedLabelColor = NudgeColors.Emerald
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Note
        BasicTextField(
            value = note, onValueChange = { note = it },
            textStyle = TextStyle(fontSize = 13.sp, color = NudgeColors.Ink),
            cursorBrush = SolidColor(NudgeColors.Emerald),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NudgeColors.Bone)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { inner -> if (note.isEmpty()) Text("Add a note (optional)", fontSize = 13.sp, color = NudgeColors.InkMute); inner() }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Submit
        Button(
            onClick = { onAdd(amountCents, selectedType, merchant, selectedAccountId!!, selectedCategoryId, note.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald),
            enabled = isValid
        ) {
            Text("Add ${if (selectedType == TransactionType.DEBIT) "Expense" else "Income"}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
