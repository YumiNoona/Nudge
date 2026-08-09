package com.nudge.android.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.importer.FinancialDocumentImporter
import com.nudge.android.importer.StatementDraft
import com.nudge.android.ui.components.FloatingActionCube
import com.nudge.android.ui.theme.*
import com.nudge.engine.DefaultSmsParserEngine
import com.nudge.model.TransactionType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SharedFinancialImport(val uri: Uri? = null, val text: String? = null)

@Composable
fun FinancialImportScreen(
    accounts: List<AccountEntity>,
    sharedImport: SharedFinancialImport?,
    onSharedImportConsumed: () -> Unit,
    onImport: (List<StatementDraft>, String, Boolean, (Int, Int) -> Unit) -> Unit,
    onCreateAccount: (AccountEntity) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<List<StatementDraft>>(emptyList()) }
    var replaceExistingStatements by remember { mutableStateOf(false) }
    var selectedAccount by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.isDefault && it.isActive }?.id ?: accounts.firstOrNull { it.isActive }?.id)
    }
    var showAccountCreator by remember { mutableStateOf(false) }
    var pendingAccountSelection by remember { mutableStateOf<String?>(null) }
    val detectedDateRange = remember(drafts) {
        drafts.takeIf { it.isNotEmpty() }?.let { rows ->
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val first = rows.minOf { it.timestampEpoch }
            val last = rows.maxOf { it.timestampEpoch }
            if (first == last) formatter.format(Date(first))
            else "${formatter.format(Date(first))} – ${formatter.format(Date(last))}"
        }
    }
    LaunchedEffect(accounts, pendingAccountSelection) {
        val pending = pendingAccountSelection
        if (pending != null && accounts.any { it.id == pending && it.isActive && !it.isArchived }) {
            selectedAccount = pending
            pendingAccountSelection = null
        }
    }

    fun parseText(text: String) {
        val statementRows = FinancialDocumentImporter.parseStatement(text)
        drafts = if (statementRows.isNotEmpty()) statementRows else {
            DefaultSmsParserEngine().parse(text, "EMAIL")?.let { parsed ->
                listOf(
                    StatementDraft(
                        parsed.amount,
                        parsed.type,
                        parsed.merchantNormalized ?: parsed.merchantRaw,
                        System.currentTimeMillis(),
                    ),
                )
            } ?: FinancialDocumentImporter.parseReceipt(text)?.let { receipt ->
                listOf(StatementDraft(receipt.amountCents, TransactionType.DEBIT, receipt.merchant, System.currentTimeMillis()))
            }.orEmpty()
        }
        error = if (drafts.isEmpty()) {
            "No transaction rows were detected. Use a sharper image or export a detailed CSV/PDF statement."
        } else null
    }

    fun processUri(uri: Uri) {
        loading = true
        error = null
        warning = null
        drafts = emptyList()
        scope.launch {
            runCatching { FinancialDocumentImporter.readDocument(context, uri) }
                .onSuccess { document ->
                    warning = document.warning
                    parseText(document.text)
                }
                .onFailure { error = it.message ?: "Nudge could not read this document. Choose another file." }
            loading = false
        }
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) processUri(uri)
    }
    val chooseDocument = {
        documentPicker.launch(arrayOf("text/csv", "text/plain", "application/pdf", "application/vnd.ms-excel", "image/*"))
    }
    fun importDetected() {
        val account = selectedAccount
        if (account == null) {
            Toast.makeText(context, "Add or enable an account before importing", Toast.LENGTH_LONG).show()
            return
        }
        onImport(drafts, account, replaceExistingStatements) { imported, skipped ->
            Toast.makeText(context, "Imported $imported · skipped $skipped duplicates", Toast.LENGTH_LONG).show()
            drafts = emptyList()
            warning = null
            replaceExistingStatements = false
        }
    }

    LaunchedEffect(sharedImport) {
        val shared = sharedImport ?: return@LaunchedEffect
        warning = null
        when {
            shared.uri != null -> processUri(shared.uri)
            !shared.text.isNullOrBlank() -> parseText(shared.text)
        }
        onSharedImportConsumed()
    }

    Box(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft())
            }
            Text("Smart import", style = DSTypography.headlineLarge, color = DSBridge.ink(), modifier = Modifier.align(Alignment.Center))
        }

        if (drafts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
                Column(
                    Modifier.align(Alignment.Center).offset(y = (-26).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(62.dp).background(DSBridge.accentBg(), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Lucide.FileText(size = 27.dp, color = DSBridge.accent())
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        if (loading) "Reading on device" else "Import a statement",
                        color = DSBridge.ink(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (loading) "Finding transaction rows and checking document quality…"
                        else "Choose a bank statement, exported text file, screenshot or receipt image.",
                        color = DSBridge.inkMute(), fontSize = 10.sp, lineHeight = 15.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 290.dp),
                    )
                    Spacer(Modifier.height(17.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("PDF", "CSV / TXT", "IMAGE", "EMAIL").forEach { format ->
                            Surface(shape = RoundedCornerShape(9.dp), color = DSBridge.surface()) {
                                Text(format, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), fontFamily = MonoFamily, fontSize = 7.sp, color = DSBridge.inkMute())
                            }
                        }
                    }
                    error?.let { ImportStatus(it, isError = true) }
                }

                Text(
                    "Password-protected PDFs and unreadable images are flagged before anything is imported.",
                    color = DSBridge.inkMute(), fontSize = 8.sp, lineHeight = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 102.dp).widthIn(max = 300.dp),
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(DSBridge.accentBg(), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Lucide.Check(size = 21.dp, color = DSBridge.accent())
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${drafts.size} transactions found", color = DSBridge.ink(), fontWeight = FontWeight.Bold)
                        Text(detectedDateRange ?: "Nothing is saved until you confirm", color = DSBridge.inkMute(), fontSize = 9.sp)
                    }
                    IconButton(onClick = chooseDocument) { Lucide.Upload(size = 19.dp, color = DSBridge.inkSoft()) }
                }

                warning?.let { ImportStatus(it, isError = false) }

                Text("IMPORT TO", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = 1.sp, color = DSBridge.inkMute())
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    accounts.filter { it.isActive && !it.isArchived }.forEach { account ->
                        val selected = account.id == selectedAccount
                        Surface(
                            onClick = { selectedAccount = account.id },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = if (selected) DSBridge.accentBg() else DSBridge.surface(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) DSBridge.accent() else DSBridge.inkMute().copy(alpha = .14f)),
                        ) {
                            Row(Modifier.fillMaxHeight().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Lucide.Wallet(size = 16.dp, color = if (selected) DSBridge.accent() else DSBridge.inkMute())
                                Spacer(Modifier.width(7.dp))
                                Text(account.name, fontSize = 11.sp, color = if (selected) DSBridge.accent() else DSBridge.inkSoft())
                            }
                        }
                    }
                    Surface(
                        onClick = { showAccountCreator = true },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = DSBridge.surface(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .14f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Lucide.Plus(size = 18.dp, color = DSBridge.accent())
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(17.dp), color = DSBridge.surface()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Replace earlier statement imports", color = DSBridge.ink(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Only statement rows in this account", color = DSBridge.inkMute(), fontSize = 8.sp)
                        }
                        Switch(checked = replaceExistingStatements, onCheckedChange = { replaceExistingStatements = it })
                    }
                }

                Surface(shape = RoundedCornerShape(20.dp), color = DSBridge.surface()) {
                    Column {
                        drafts.take(12).forEachIndexed { index, draft ->
                            ImportPreviewRow(draft)
                            if (index < minOf(drafts.size, 12) - 1) HorizontalDivider(color = DSBridge.background())
                        }
                        if (drafts.size > 12) Text("+ ${drafts.size - 12} more", modifier = Modifier.padding(14.dp), fontSize = 10.sp, color = DSBridge.inkMute())
                    }
                }
                Spacer(Modifier.height(108.dp))
            }
        }
        }

        FloatingActionCube(
            contentDescription = when {
                loading -> "Reading document"
                drafts.isEmpty() -> if (error == null) "Choose file or image" else "Choose another file"
                else -> "Import ${drafts.size} transactions"
            },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp),
            onClick = {
                if (!loading) {
                    if (drafts.isEmpty()) chooseDocument() else importDetected()
                }
            },
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = DS.InkPrimary)
                drafts.isEmpty() -> Lucide.Upload(size = 24.dp, color = DS.InkPrimary)
                else -> Lucide.Check(size = 24.dp, color = DS.InkPrimary)
            }
        }

        if (showAccountCreator) {
            AccountEditSheet(
                account = null,
                onSave = { account ->
                    onCreateAccount(account)
                    pendingAccountSelection = account.id
                    showAccountCreator = false
                },
                onDelete = {},
                onDismiss = { showAccountCreator = false },
            )
        }
    }
}

@Composable
private fun ImportStatus(message: String, isError: Boolean) {
    Spacer(Modifier.height(14.dp))
    Surface(
        modifier = Modifier.widthIn(max = 330.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isError) DS.Negative.copy(alpha = .09f) else DS.Warning.copy(alpha = .10f),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.Top) {
            Lucide.Info(size = 18.dp, color = if (isError) DS.Negative else DS.Warning)
            Spacer(Modifier.width(9.dp))
            Text(message, color = DSBridge.inkSoft(), fontSize = 9.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ImportPreviewRow(draft: StatementDraft) {
    val debit = draft.type == TransactionType.DEBIT
    val color = when (draft.type) {
        TransactionType.DEBIT -> DS.Negative
        TransactionType.CREDIT -> DS.Positive
        TransactionType.REFUND -> DS.Warning
        TransactionType.TRANSFER -> DSBridge.inkSoft()
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(color.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
            if (debit) Lucide.ShoppingCart(size = 16.dp, color = color) else Lucide.TrendingUp(size = 16.dp, color = color)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(draft.merchant, color = DSBridge.ink(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(draft.timestampEpoch)), color = DSBridge.inkMute(), fontSize = 9.sp)
        }
        val sign = if (debit) "−" else ""
        Text(
            "$sign₹${"%,.2f".format(draft.amountCents / 100.0)}",
            color = color, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        )
    }
}
