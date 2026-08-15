package com.nudge.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nudge.android.data.FriendEntity
import com.nudge.android.data.RecurrenceDraft
import com.nudge.android.data.SplitDraft
import com.nudge.android.ui.components.KeypadGrid
import com.nudge.android.ui.components.applyKeypadInput
import com.nudge.android.ui.components.ScrollableChipRow
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.DSSpace
import com.nudge.android.ui.theme.DSTypography
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.CategoryGlyph
import com.nudge.android.ui.theme.NudgeHaptics
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.nudge.model.TransactionType
import com.nudge.android.importer.DetailedReceiptDraft
import android.net.Uri
import java.io.File
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    friends: List<FriendEntity>,
    onDismiss: () -> Unit,
    onOpenSmartImport: () -> Unit,
    onCreateCategory: (CategoryEntity) -> Unit,
    onCreateAccount: (AccountEntity) -> Unit,
    onCreateFriend: (String) -> FriendEntity,
    onSaveReceipt: (DetailedReceiptDraft, String, String?, Boolean, (Boolean, String) -> Unit) -> Unit,
    onAdd: (
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        accountId: String,
        categoryId: String?,
        note: String?,
        timestampEpoch: Long,
        split: SplitDraft?,
        recurrence: RecurrenceDraft?,
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showReceiptScanner by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) showReceiptScanner = true
    }
    var detailedReceipt by remember { mutableStateOf<DetailedReceiptDraft?>(null) }
    var keepReceiptFiles by remember { mutableStateOf(false) }
    var categoryCreationType by remember { mutableStateOf<String?>(null) }
    var showAccountCreator by remember { mutableStateOf(false) }
    var preferredCategoryId by remember { mutableStateOf<String?>(null) }
    var preferredAccountId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DSBridge.surface(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.width(42.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DSBridge.inkMute().copy(alpha = .42f)),
                )
            }
        },
    ) {
        AddTransactionSheetContent(
            categories = categories,
            accounts = accounts,
            friends = friends,
            onScanReceipt = {
                if (cameraGranted) showReceiptScanner = true
                else cameraPermission.launch(Manifest.permission.CAMERA)
            },
            onOpenSmartImport = onOpenSmartImport,
            preferredCategoryId = preferredCategoryId,
            preferredAccountId = preferredAccountId,
            onRequestAddCategory = { categoryCreationType = it },
            onRequestAddAccount = { showAccountCreator = true },
            onCreateFriend = onCreateFriend,
            onAdd = onAdd,
        )
    }
    if (showReceiptScanner) ReceiptScannerDialog(
        onDismiss = { showReceiptScanner = false },
        onScanned = {
            keepReceiptFiles = false
            detailedReceipt = it
            showReceiptScanner = false
        },
    )
    detailedReceipt?.let { receipt ->
        ReceiptReviewDialog(
            initial = receipt,
            accounts = accounts,
            categories = categories,
            onDismiss = {
                if (!keepReceiptFiles) receipt.pages.forEach { page ->
                    runCatching { File(Uri.parse(page.localUri).path.orEmpty()).delete() }
                }
                keepReceiptFiles = false
                detailedReceipt = null
            },
            onSave = { draft, accountId, categoryId, itemized, result ->
                onSaveReceipt(draft, accountId, categoryId, itemized) { success, message ->
                    if (success) keepReceiptFiles = true
                    result(success, message)
                    if (success) onDismiss()
                }
            },
        )
    }
    categoryCreationType?.let { defaultType ->
        CategoryEditorSheet(
            category = null,
            defaultType = defaultType,
            onDismiss = { categoryCreationType = null },
            onSave = { name, type, icon, color ->
                val entity = CategoryEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    icon = icon,
                    color = color,
                    sortOrder = categories.size,
                )
                onCreateCategory(entity)
                preferredCategoryId = entity.id
                categoryCreationType = null
            },
        )
    }
    if (showAccountCreator) {
        AccountEditSheet(
            account = null,
            onSave = { account ->
                onCreateAccount(account)
                preferredAccountId = account.id
                showAccountCreator = false
            },
            onDelete = {},
            onDismiss = { showAccountCreator = false },
        )
    }
}

@Composable
private fun AddTransactionSheetContent(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    friends: List<FriendEntity>,
    onScanReceipt: () -> Unit,
    onOpenSmartImport: () -> Unit,
    preferredCategoryId: String?,
    preferredAccountId: String?,
    onRequestAddCategory: (String) -> Unit,
    onRequestAddAccount: () -> Unit,
    onCreateFriend: (String) -> FriendEntity,
    onAdd: (
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        accountId: String,
        categoryId: String?,
        note: String?,
        timestampEpoch: Long,
        split: SplitDraft?,
        recurrence: RecurrenceDraft?,
    ) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var timestampEpoch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var split by remember { mutableStateOf<SplitDraft?>(null) }
    var recurrence by remember { mutableStateOf<RecurrenceDraft?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSplitEditor by remember { mutableStateOf(false) }
    var showRecurrenceEditor by remember { mutableStateOf(false) }
    val selectableAccounts = remember(accounts) {
        accounts.filter {
            it.isActive && !it.isArchived && !it.accountType.contains("saving", ignoreCase = true)
        }
    }
    val localContext = LocalContext.current
    val haptics = remember(localContext) { NudgeHaptics(localContext) }

    LaunchedEffect(selectableAccounts) {
        if (selectedAccountId == null) {
            selectedAccountId = selectableAccounts.firstOrNull { it.isDefault }?.id ?: selectableAccounts.firstOrNull()?.id
        }
    }
    LaunchedEffect(preferredCategoryId, categories) {
        if (preferredCategoryId != null && categories.any { it.id == preferredCategoryId }) {
            selectedCategoryId = preferredCategoryId
        }
    }
    LaunchedEffect(preferredAccountId, selectableAccounts) {
        if (preferredAccountId != null && selectableAccounts.any { it.id == preferredAccountId }) {
            selectedAccountId = preferredAccountId
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
            .fillMaxHeight(.70f)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(DSSpace.sm))

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
            Surface(
                onClick = onScanReceipt,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = DSBridge.accentBg(),
            ) {
                Box(contentAlignment = Alignment.Center) { Lucide.Camera(size = 19.dp, color = DSBridge.accent()) }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onOpenSmartImport,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = DSBridge.surface(),
                border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .16f)),
            ) {
                Box(contentAlignment = Alignment.Center) { Lucide.Upload(size = 19.dp, color = DSBridge.inkSoft()) }
            }
        }
        Spacer(modifier = Modifier.height(DSSpace.md))

        TransactionExtrasRow(
            timestampEpoch = timestampEpoch,
            split = split,
            recurrence = recurrence,
            onDateClick = { showDatePicker = true },
            onSplitClick = { if (selectedType == TransactionType.DEBIT) showSplitEditor = true },
            onRepeatClick = { showRecurrenceEditor = true },
        )
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Keypad
        KeypadGrid(
            onKey = { key -> amountStr = applyKeypadInput(amountStr, key) },
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(DSSpace.md))

        BasicTextField(
            value = note,
            onValueChange = { note = it },
            textStyle = TextStyle(fontSize = 14.sp, color = DSBridge.ink()),
            cursorBrush = SolidColor(DSBridge.accent()),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DSBridge.background())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner -> if (note.isEmpty()) Text("Add a note (optional)", fontSize = 14.sp, color = DSBridge.inkMute()); inner() },
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
            decorationBox = { inner -> if (merchant.isEmpty()) Text("What was this for?", fontSize = 15.sp, color = DSBridge.inkMute()); inner() }
        )
        Spacer(modifier = Modifier.height(DSSpace.md))

        // Category grid
        Text("Category", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
        Spacer(modifier = Modifier.height(DSSpace.sm))

        val categoryType = if (selectedType == TransactionType.CREDIT) "income" else "expense"
        val expenseCats = categories.filter { it.type == categoryType }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
                val tileGap = 8.dp
                val categoryTileHeight = 67.dp
                val categoryWidth = (maxWidth - (tileGap * 2)) / 3
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((categoryTileHeight * 2) + tileGap),
                    horizontalArrangement = Arrangement.spacedBy(tileGap),
                    verticalArrangement = Arrangement.spacedBy(tileGap),
                ) {
                    items(expenseCats, key = { it.id }) { cat ->
                        val isSel = selectedCategoryId == cat.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .width(categoryWidth)
                                .height(categoryTileHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) DSBridge.accentBg() else DSBridge.background())
                                .then(if (isSel) Modifier.border(1.5.dp, DSBridge.accent(), RoundedCornerShape(12.dp)) else Modifier)
                                .clickable {
                                    haptics.impactLight()
                                    selectedCategoryId = cat.id
                                }
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            CategoryGlyph(cat.icon, cat.name, if (isSel) DSBridge.accent() else DSBridge.inkSoft(), Modifier.size(19.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(cat.name, fontSize = 11.sp, color = if (isSel) DSBridge.accent() else DSBridge.inkSoft(), maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                    item(key = "add_category") {
                        Box(
                            modifier = Modifier
                                .width(categoryWidth)
                                .height(categoryTileHeight)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DSBridge.accentBg().copy(alpha = .62f))
                                .clickable {
                                    haptics.impactLight()
                                    onRequestAddCategory(categoryType)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Lucide.Plus(size = 22.dp, color = DSBridge.accent())
                        }
                    }
                }
            }
        Spacer(modifier = Modifier.height(DSSpace.sm))

        // Account
        Text("Account", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
        Spacer(modifier = Modifier.height(DSSpace.sm))
        val accountTileHeight = 84.dp
        val accountsWithAdd: List<AccountEntity?> = selectableAccounts + listOf(null)
        accountsWithAdd.chunked(3).forEach { rowAccounts ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(accountTileHeight),
                ) {
                    rowAccounts.forEach { a ->
                        if (a == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(DSBridge.accentBg().copy(alpha = .62f))
                                    .clickable {
                                        haptics.impactLight()
                                        onRequestAddAccount()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Lucide.Plus(size = 22.dp, color = DSBridge.accent())
                            }
                        } else {
                        val isSel = selectedAccountId == a.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (isSel) DSBridge.accentBg() else DSBridge.background())
                                .then(if (isSel) Modifier.border(1.5.dp, DSBridge.accent(), RoundedCornerShape(13.dp)) else Modifier)
                                .clickable {
                                    haptics.impactLight()
                                    selectedAccountId = a.id
                                }
                                .padding(horizontal = 6.dp, vertical = 9.dp)
                        ) {
                            AccountTypeIcon(a.accountType, if (isSel) DSBridge.accent() else DSBridge.inkSoft(), 20.dp)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                a.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSel) DSBridge.accent() else DSBridge.inkSoft(),
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                        }
                    }
                    repeat(3 - rowAccounts.size) {
                        Spacer(Modifier.weight(1f).fillMaxHeight())
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        Spacer(modifier = Modifier.height(DSSpace.sm))

        Spacer(modifier = Modifier.height(DSSpace.md))

        // Submit
        Button(
            onClick = {
                val accountId = selectedAccountId
                if (accountId != null) {
                    val description = merchant.trim().ifBlank { "Manual $typeLabel" }
                    haptics.success()
                    onAdd(amountCents, selectedType, description, accountId, selectedCategoryId, note.trim().ifBlank { null }, timestampEpoch, split, recurrence)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent()),
            enabled = isValid
        ) {
            Text("Add $typeLabel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showDatePicker) TransactionDateDialog(timestampEpoch, { showDatePicker = false }) { timestampEpoch = it }
    if (showSplitEditor) SplitExpenseDialog(
        amountCents = amountCents,
        friends = friends,
        initial = split,
        onCreateFriend = onCreateFriend,
        onDismiss = { showSplitEditor = false },
        onSave = { split = it },
    )
    if (showRecurrenceEditor) RecurrenceDialog(recurrence, { showRecurrenceEditor = false }) { recurrence = it }
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
