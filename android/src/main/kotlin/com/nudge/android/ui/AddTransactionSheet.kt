package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.CategoryEntity
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

    val amountInCents = amountStr.filter { it.isDigit() }.toLongOrNull()?.times(100L)
    val isValid = amountInCents != null && amountInCents > 0 && merchant.isNotBlank() && selectedAccountId != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NudgeColors.SurfaceBase, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(24.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NudgeColors.ContentTertiary)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Add Transaction",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = NudgeColors.ContentPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Amount field (big, amount-first)
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("₹", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeColors.AccentPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.ContentPrimary,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(NudgeColors.AccentPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (amountStr.isEmpty()) {
                        Text(
                            "0",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = NudgeColors.ContentTertiary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction type toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionType.entries.forEach { type ->
                val isSelected = selectedType == type
                val label = when (type) {
                    TransactionType.DEBIT -> "Expense"
                    TransactionType.CREDIT -> "Income"
                    TransactionType.REFUND -> "Refund"
                    TransactionType.TRANSFER -> "Transfer"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedType = type },
                    label = {
                        Text(label, fontSize = 12.sp)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeColors.AccentPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = NudgeColors.AccentPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Merchant field
        BasicTextField(
            value = merchant,
            onValueChange = { merchant = it },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = NudgeColors.ContentPrimary
            ),
            cursorBrush = SolidColor(NudgeColors.AccentPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NudgeColors.SurfaceRaised)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                if (merchant.isEmpty()) {
                    Text("What was this for?", fontSize = 16.sp, color = NudgeColors.ContentTertiary)
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category grid
        Text(
            "Category",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = NudgeColors.ContentSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        val expenseCategories = categories.filter { it.type == "expense" }
        if (expenseCategories.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenseCategories) { category ->
                    val isSelected = selectedCategoryId == category.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NudgeColors.AccentPrimary.copy(alpha = 0.12f)
                                else NudgeColors.SurfaceRaised
                            )
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, NudgeColors.AccentPrimary, RoundedCornerShape(12.dp))
                                else Modifier.border(1.dp, NudgeColors.ContentTertiary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            )
                            .clickable { selectedCategoryId = category.id }
                            .padding(8.dp)
                    ) {
                        Text(
                            category.icon ?: "📁",
                            fontSize = 18.sp
                        )
                        Text(
                            category.name,
                            fontSize = 10.sp,
                            color = if (isSelected) NudgeColors.AccentPrimary else NudgeColors.ContentSecondary,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Account selector
        Text(
            "Account",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = NudgeColors.ContentSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.take(4).forEach { account ->
                val isSelected = selectedAccountId == account.id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAccountId = account.id },
                    label = { Text(account.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NudgeColors.AccentPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = NudgeColors.AccentPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note field
        BasicTextField(
            value = note,
            onValueChange = { note = it },
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = NudgeColors.ContentPrimary
            ),
            cursorBrush = SolidColor(NudgeColors.AccentPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NudgeColors.SurfaceRaised)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                if (note.isEmpty()) {
                    Text("Add a note (optional)", fontSize = 14.sp, color = NudgeColors.ContentTertiary)
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Add button
        Button(
            onClick = {
                onAdd(
                    amountInCents ?: return@Button,
                    selectedType,
                    merchant,
                    selectedAccountId ?: return@Button,
                    selectedCategoryId,
                    note.ifBlank { null }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NudgeColors.AccentPrimary,
                disabledContainerColor = NudgeColors.ContentTertiary.copy(alpha = 0.3f)
            ),
            enabled = isValid
        ) {
            Text(
                "Add ${if (selectedType == TransactionType.DEBIT) "Expense" else "Income"}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
