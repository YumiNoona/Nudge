package com.nudge.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedMessagesScreen(
    sources: List<SavedSourceMessageEntity>,
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    decryptSource: (SavedSourceMessageEntity?) -> String?,
    onDeleteBody: (String) -> Unit,
    onClearAll: () -> Unit,
    onRetentionChanged: (Int?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    val saved = sources.filter { it.encryptedBody != null }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var retention by remember { mutableStateOf(prefs.getInt("source_retention_days", 0).takeIf { it > 0 }) }
    var viewing by remember { mutableStateOf<SavedSourceMessageEntity?>(null) }
    var deleting by remember { mutableStateOf<SavedSourceMessageEntity?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val decrypted = remember(saved) { saved.associate { it.id to decryptSource(it).orEmpty() } }
    val visible = remember(saved, decrypted, query, filter) {
        saved.filter { source ->
            val typeMatches = filter == "all" || source.sourceType.startsWith(filter)
            val transaction = transactions.firstOrNull { it.id == source.transactionId }
            val queryMatches = query.isBlank() || source.sender.orEmpty().contains(query, true) ||
                source.packageName.orEmpty().contains(query, true) || decrypted[source.id].orEmpty().contains(query, true) ||
                transaction?.merchantRaw.orEmpty().contains(query, true)
            typeMatches && queryMatches
        }
    }
    val storageBytes = saved.sumOf { it.encryptedBody?.length?.toLong() ?: 0L }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Lucide.ArrowLeft(size = 22.dp, color = DSBridge.inkSoft()) }
            Column(Modifier.weight(1f)) {
                Text("Saved messages", style = DSTypography.headlineLarge, color = DSBridge.ink())
                Text("Only sources linked to captured transactions", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            }
            if (saved.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Lucide.Trash2(size = 19.dp, color = DS.Negative) }
        }

        Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(22.dp), color = DS.AccentDeep) {
            Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(saved.size.toString(), fontFamily = MonoFamily, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("SAVED SOURCES", fontFamily = MonoFamily, fontSize = 8.sp, color = Color.White.copy(alpha = .52f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatBytes(storageBytes), fontFamily = MonoFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DS.Signal)
                    Text("ENCRYPTED STORAGE", fontFamily = MonoFamily, fontSize = 8.sp, color = Color.White.copy(alpha = .52f))
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Lucide.Search(size = 18.dp, color = DSBridge.inkMute()) },
            placeholder = { Text("Search sender, merchant or message") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "All", "sms" to "SMS", "notification" to "Notifications").forEach { (id, label) ->
                FilterChip(selected = filter == id, onClick = { filter = id }, label = { Text(label, fontSize = 10.sp) })
            }
        }

        Text("AUTO DELETE", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp))
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(null to "Forever", 30 to "30d", 90 to "90d", 365 to "1y").forEach { (days, label) ->
                FilterChip(selected = retention == days, onClick = {
                    retention = days
                    prefs.edit().putInt("source_retention_days", days ?: 0).apply()
                    onRetentionChanged(days)
                }, label = { Text(label, fontSize = 10.sp) })
            }
        }

        if (visible.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Lucide.Message(size = 38.dp, color = DSBridge.inkMute())
                Spacer(Modifier.height(12.dp))
                Text(if (saved.isEmpty()) "No saved messages" else "Nothing matches", style = DSTypography.titleLarge, color = DSBridge.ink())
                Text(if (saved.isEmpty()) "Enable message saving for future automatic transactions" else "Try another search or filter", style = DSTypography.bodySmall, color = DSBridge.inkMute(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visible, key = { it.id }) { source ->
                    val transaction = transactions.firstOrNull { it.id == source.transactionId }
                    Surface(onClick = { viewing = source }, shape = RoundedCornerShape(19.dp), color = DSBridge.surface()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(DSBridge.accentBg(), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                if (source.sourceType.startsWith("sms")) Lucide.Message(size = 19.dp, color = DSBridge.accent())
                                else Lucide.Bell(size = 19.dp, color = DSBridge.accent())
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(transaction?.merchantRaw ?: source.sender ?: source.packageName ?: "Saved source", style = DSTypography.titleMedium, color = DSBridge.ink(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(decrypted[source.id].orEmpty(), style = DSTypography.bodySmall, color = DSBridge.inkMute(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(source.messageTimestamp)), fontFamily = MonoFamily, fontSize = 8.sp, color = DSBridge.inkMute())
                            }
                            IconButton(onClick = { deleting = source }) { Lucide.Trash2(size = 18.dp, color = DS.Negative) }
                        }
                    }
                }
            }
        }
    }

    viewing?.let { source ->
        transactions.firstOrNull { it.id == source.transactionId }?.let { transaction ->
            SourceMessageSheet(
                transaction,
                source,
                decrypted[source.id],
                accounts.firstOrNull { it.id == transaction.accountId },
                categories.firstOrNull { it.id == transaction.categoryId },
                onDismiss = { viewing = null }
            )
        }
    }

    deleting?.let { source ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete saved message?") },
            text = { Text("The transaction will stay. Only its encrypted saved message will be removed.") },
            confirmButton = { Button(onClick = { onDeleteBody(source.id); deleting = null }, colors = ButtonDefaults.buttonColors(containerColor = DS.Negative)) { Text("Delete message") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear all saved messages?") },
        text = { Text("Transactions will remain, but saved message text cannot be recovered.") },
        confirmButton = { Button(onClick = { onClearAll(); confirmClear = false }, colors = ButtonDefaults.buttonColors(containerColor = DS.Negative)) { Text("Clear all") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(Locale.US, bytes / 1024.0)
    else -> "%.1f MB".format(Locale.US, bytes / (1024.0 * 1024.0))
}
