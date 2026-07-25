package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.BudgetEntity
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.NudgeColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object NudgeRadius {
    const val SM = 8
    const val MD = 14
    const val LG = 20
    const val XL = 28
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    isDark: Boolean,
    onSave: (id: String?, categoryId: String?, amountCents: Long, period: String, rolloverEnabled: Boolean, startDateEpoch: Long) -> Unit,
    onDelete: (id: String) -> Unit,
    onBack: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Budgets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190", fontSize = 18.sp, color = NudgeColors.ContentSecondary)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        editingBudget = null
                        showSheet = true
                    }) {
                        Text(
                            "+ Add Budget",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NudgeColors.AccentPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) NudgeColors.DarkSurfaceBase else NudgeColors.SurfaceBase
                )
            )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (budgets.isEmpty()) {
                item {
                    BudgetsEmptyState()
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val category = categories.find { it.id == budget.categoryId }
                    val spent = remember(transactions, budget) {
                        transactions
                            .filter { it.categoryId == budget.categoryId && it.type == "debit" }
                            .sumOf { it.amountCents }
                    }
                    val progress = remember(spent, budget) {
                        if (budget.amountCents > 0) (spent.toFloat() / budget.amountCents.toFloat()).coerceIn(0f, 2f) else 0f
                    }
                    val remaining = budget.amountCents - spent

                    BudgetCard(
                        budget = budget,
                        category = category,
                        spent = spent,
                        progress = progress,
                        remaining = remaining,
                        isDark = isDark,
                        onClick = {
                            editingBudget = budget
                            showSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showSheet) {
        BudgetEditSheet(
            budget = editingBudget,
            categories = categories,
            onDismiss = { showSheet = false },
            onSave = { id, catId, amount, period, rollover, startDate ->
                onSave(id, catId, amount, period, rollover, startDate)
                showSheet = false
            },
            onDelete = editingBudget?.let { b ->
                { onDelete(b.id); showSheet = false }
            }
        )
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetEntity,
    category: CategoryEntity?,
    spent: Long,
    progress: Float,
    remaining: Long,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val spentFormatted = remember(spent) { formatter.format(spent / 100.0) }
    val totalFormatted = remember(budget) { formatter.format(budget.amountCents / 100.0) }
    val remainingFormatted = remember(remaining) { formatter.format(kotlin.math.abs(remaining) / 100.0) }

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NudgeColors.DarkSurfaceRaised else NudgeColors.SurfaceRaised
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress.coerceAtMost(1f),
                    modifier = Modifier.fillMaxSize(),
                    color = when {
                        progress > 1f -> NudgeColors.Negative
                        progress > 0.8f -> NudgeColors.Warning
                        else -> NudgeColors.AccentPrimary
                    },
                    strokeWidth = 5.dp,
                    trackColor = NudgeColors.ContentTertiary.copy(alpha = 0.2f)
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (category?.icon != null) {
                        Text(
                            category.icon,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        category?.name ?: "All Categories",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "\u20B9$spentFormatted spent of \u20B9$totalFormatted",
                    fontSize = 13.sp,
                    color = NudgeColors.ContentSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(NudgeRadius.SM),
                        color = NudgeColors.AccentPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            budget.period.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NudgeColors.AccentPrimary
                        )
                    }

                    if (budget.rolloverEnabled) {
                        Surface(
                            shape = RoundedCornerShape(NudgeRadius.SM),
                            color = NudgeColors.Warning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Rollover",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = NudgeColors.Warning
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (remaining >= 0) "\u20B9$remainingFormatted" else "-\u20B9$remainingFormatted",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (remaining >= 0) NudgeColors.Positive else NudgeColors.Negative
                )
                Text(
                    if (remaining >= 0) "left" else "over",
                    fontSize = 11.sp,
                    color = if (remaining >= 0) NudgeColors.Positive.copy(alpha = 0.7f) else NudgeColors.Negative.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun BudgetsEmptyState() {
    Card(
        shape = RoundedCornerShape(NudgeRadius.XL),
        colors = CardDefaults.cardColors(
            containerColor = NudgeColors.AccentPrimary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\uD83D\uDCB0",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No budgets yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NudgeColors.ContentSecondary
            )
            Text(
                "Tap \"+ Add Budget\" to set your first spending limit",
                fontSize = 13.sp,
                color = NudgeColors.ContentTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditSheet(
    budget: BudgetEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (id: String?, categoryId: String?, amountCents: Long, period: String, rolloverEnabled: Boolean, startDateEpoch: Long) -> Unit,
    onDelete: (() -> Unit)?
) {
    val isEditing = budget != null

    var selectedCategory by remember { mutableStateOf(categories.find { it.id == budget?.categoryId }) }
    var amountStr by remember {
        mutableStateOf(
            if (budget != null) (budget.amountCents / 100).toString() else ""
        )
    }
    var selectedPeriod by remember { mutableStateOf(budget?.period?.replaceFirstChar { it.uppercase() } ?: "Monthly") }
    var rolloverEnabled by remember { mutableStateOf(budget?.rolloverEnabled ?: false) }
    var startDateEpoch by remember { mutableStateOf(budget?.startDateEpoch ?: System.currentTimeMillis()) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountInCents = amountStr.filter { it.isDigit() }.toLongOrNull()?.times(100L)
    val isValid = amountInCents != null && amountInCents > 0

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = NudgeColors.SurfaceBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                if (isEditing) "Edit Budget" else "New Budget",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NudgeColors.ContentPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category selector
            Text(
                "Category",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedCategory != null) "${selectedCategory!!.icon ?: "\uD83D\uDCC1"} ${selectedCategory!!.name}" else "Select a category",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = NudgeColors.ContentTertiary.copy(alpha = 0.3f),
                        focusedBorderColor = NudgeColors.AccentPrimary,
                        focusedTextColor = NudgeColors.ContentPrimary,
                        unfocusedTextColor = NudgeColors.ContentPrimary
                    ),
                    textStyle = TextStyle(fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    val expenseCategories = categories.filter { it.type == "expense" }
                    if (expenseCategories.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No categories available", color = NudgeColors.ContentTertiary) },
                            onClick = { categoryDropdownExpanded = false }
                        )
                    } else {
                        expenseCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text("${category.icon ?: "\uD83D\uDCC1"} ${category.name}")
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            Text(
                "Budget Amount",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NudgeColors.SurfaceRaised)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    "\u20B9",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.AccentPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.ContentPrimary,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(NudgeColors.AccentPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (amountStr.isEmpty()) {
                            Text(
                                "0",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
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

            // Period selector
            Text(
                "Period",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Weekly", "Monthly", "Custom").forEach { period ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(period, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeColors.AccentPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = NudgeColors.AccentPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rollover toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Rollover",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = NudgeColors.ContentPrimary
                    )
                    Text(
                        "Carry unspent budget to next period",
                        fontSize = 12.sp,
                        color = NudgeColors.ContentTertiary
                    )
                }
                Switch(
                    checked = rolloverEnabled,
                    onCheckedChange = { rolloverEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NudgeColors.AccentPrimary,
                        checkedTrackColor = NudgeColors.AccentPrimary.copy(alpha = 0.3f),
                        uncheckedThumbColor = NudgeColors.ContentTertiary,
                        uncheckedTrackColor = NudgeColors.ContentTertiary.copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start date
            Text(
                "Start Date",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NudgeColors.ContentTertiary.copy(alpha = 0.3f))
            ) {
                Text(
                    dateFormat.format(Date(startDateEpoch)),
                    fontSize = 15.sp,
                    color = NudgeColors.ContentPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    onSave(
                        budget?.id,
                        selectedCategory?.id,
                        amountInCents ?: return@Button,
                        selectedPeriod.lowercase(),
                        rolloverEnabled,
                        startDateEpoch
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
                    if (isEditing) "Save Changes" else "Create Budget",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Delete button (only when editing)
            if (onDelete != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NudgeColors.Negative)
                ) {
                    Text(
                        "Delete Budget",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.Negative
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDateEpoch
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDateEpoch = millis
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = NudgeColors.AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = NudgeColors.ContentSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
