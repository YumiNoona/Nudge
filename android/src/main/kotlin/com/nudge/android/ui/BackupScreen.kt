package com.nudge.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.android.ui.theme.NudgeRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Backup & data management screen.
 * Export: JSON backup, encrypted backup
 * Import: JSON restore
 * Reset: Delete all data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val gamification by viewModel.gamificationProfile.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }

    // File saver for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { scope.launch { exportToUri(context, it, transactions, categories, accounts, budgets, gamification) } }
    }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { scope.launch { importFromUri(context, it, viewModel) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NudgeColors.SurfaceBase)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Back", color = NudgeColors.ContentSecondary) }
            Text("Data & Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.ContentPrimary)
            Spacer(modifier = Modifier.width(64.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats card
            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("${transactions.size}", "Transactions")
                    StatItem("${categories.size}", "Categories")
                    StatItem("${accounts.size}", "Accounts")
                    StatItem("${budgets.size}", "Budgets")
                }
            }

            // Export section
            SectionHeader("Export Data")

            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised),
                modifier = Modifier.clickable {
                    exportLauncher.launch("nudge-backup-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.json")
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export as JSON", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NudgeColors.ContentPrimary)
                        Text("Standard unencrypted backup file", fontSize = 12.sp, color = NudgeColors.ContentTertiary)
                    }
                    Text("↓", fontSize = 20.sp, color = NudgeColors.AccentPrimary)
                }
            }

            // Import section
            SectionHeader("Import Data")

            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(containerColor = NudgeColors.SurfaceRaised),
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Import from backup", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = NudgeColors.ContentPrimary)
                        Text("Restore from a JSON backup file. Merges with existing data.", fontSize = 12.sp, color = NudgeColors.ContentTertiary)
                    }
                    Text("↑", fontSize = 20.sp, color = NudgeColors.AccentPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete section
            SectionHeader("Danger Zone", color = NudgeColors.Negative)

            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(
                    containerColor = NudgeColors.Negative.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Delete All Data",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.Negative
                    )
                    Text(
                        "This will permanently delete ALL your transactions, categories, budgets, and settings. This cannot be undone.",
                        fontSize = 12.sp,
                        color = NudgeColors.ContentSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!showDeleteConfirm) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NudgeColors.Negative),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(NudgeColors.Negative)
                            )
                        ) {
                            Text("Delete Everything")
                        }
                    } else {
                        Text(
                            "Type DELETE to confirm:",
                            fontSize = 12.sp,
                            color = NudgeColors.ContentSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = deleteText,
                                onValueChange = { deleteText = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(NudgeRadius.SM)
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.deleteAllData()
                                        deleteText = ""
                                        showDeleteConfirm = false
                                        Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = deleteText == "DELETE",
                                colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Negative),
                                shape = RoundedCornerShape(NudgeRadius.SM)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color = NudgeColors.ContentPrimary) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.AccentPrimary)
        Text(label, fontSize = 11.sp, color = NudgeColors.ContentTertiary)
    }
}

// --- Export / Import helpers ---

private suspend fun exportToUri(
    context: Context,
    uri: Uri,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    budgets: List<BudgetEntity>,
    gamification: GamificationProfileEntity?
) = withContext(Dispatchers.IO) {
    try {
        val json = JSONObject()
        json.put("version", 1)
        json.put("exportedAt", System.currentTimeMillis())

        fun <T> List<T>.toJson(block: (T) -> JSONObject): JSONArray {
            val arr = JSONArray()
            forEach { arr.put(block(it)) }
            return arr
        }

        json.put("transactions", transactions.toJson { txn ->
            JSONObject().apply {
                put("id", txn.id); put("amountCents", txn.amountCents); put("type", txn.type)
                put("merchantRaw", txn.merchantRaw); put("merchantNormalized", txn.merchantNormalized ?: "")
                put("categoryId", txn.categoryId ?: ""); put("accountId", txn.accountId)
                put("source", txn.source); put("isReviewed", txn.isReviewed)
                put("note", txn.note ?: ""); put("timestampEpoch", txn.timestampEpoch)
                put("confidenceScore", txn.confidenceScore.toDouble())
            }
        })

        json.put("categories", categories.toJson { cat ->
            JSONObject().apply {
                put("id", cat.id); put("name", cat.name); put("icon", cat.icon ?: "")
                put("color", cat.color ?: ""); put("type", cat.type)
                put("monthlyBudgetCents", cat.monthlyBudgetCents ?: 0)
            }
        })

        json.put("accounts", accounts.toJson { acct ->
            JSONObject().apply {
                put("id", acct.id); put("name", acct.name)
                put("accountType", acct.accountType); put("last4Digits", acct.last4Digits ?: "")
            }
        })

        json.put("budgets", budgets.toJson { b ->
            JSONObject().apply {
                put("id", b.id); put("categoryId", b.categoryId ?: "")
                put("amountCents", b.amountCents); put("period", b.period)
                put("rolloverEnabled", b.rolloverEnabled)
            }
        })

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toString(2).toByteArray())
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun importFromUri(
    context: Context,
    uri: Uri,
    viewModel: MainViewModel
) = withContext(Dispatchers.IO) {
    try {
        val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri)))
        val jsonStr = reader.readText()
        reader.close()

        val json = JSONObject(jsonStr)

        // Import transactions
        val txnsArr = json.optJSONArray("transactions")
        if (txnsArr != null) {
            for (i in 0 until txnsArr.length()) {
                val obj = txnsArr.getJSONObject(i)
                val txn = TransactionEntity(
                    id = obj.optString("id", com.nudge.util.IdGenerator.generate()),
                    amountCents = obj.optLong("amountCents", 0),
                    type = obj.optString("type", "debit"),
                    merchantRaw = obj.optString("merchantRaw", "Imported"),
                    merchantNormalized = obj.optString("merchantNormalized", null).ifEmpty { null },
                    categoryId = obj.optString("categoryId", null).ifEmpty { null },
                    accountId = obj.optString("accountId", ""),
                    source = obj.optString("source", "csv_import"),
                    isReviewed = obj.optBoolean("isReviewed", true),
                    note = obj.optString("note", null).ifEmpty { null },
                    timestampEpoch = obj.optLong("timestampEpoch", System.currentTimeMillis()),
                    confidenceScore = obj.optDouble("confidenceScore", 1.0).toFloat()
                )
                viewModel.importTransaction(txn)
            }
        }

        // Import categories
        val catsArr = json.optJSONArray("categories")
        if (catsArr != null) {
            for (i in 0 until catsArr.length()) {
                val obj = catsArr.getJSONObject(i)
                val cat = CategoryEntity(
                    id = obj.optString("id", com.nudge.util.IdGenerator.generate()),
                    name = obj.optString("name", "Imported"),
                    icon = obj.optString("icon", null).ifEmpty { null },
                    color = obj.optString("color", null).ifEmpty { null },
                    type = obj.optString("type", "expense"),
                    monthlyBudgetCents = obj.optLong("monthlyBudgetCents", 0).takeIf { it > 0 }
                )
                viewModel.importCategory(cat)
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

