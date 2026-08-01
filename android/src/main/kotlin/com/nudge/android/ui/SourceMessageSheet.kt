package com.nudge.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.SavedSourceMessageEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceMessageSheet(
    transaction: TransactionEntity,
    source: SavedSourceMessageEntity?,
    decryptedBody: String?,
    account: AccountEntity? = null,
    category: CategoryEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val legacyBody = transaction.sourceRawText
    val body = decryptedBody ?: legacyBody
    val messageDate = source?.messageTimestamp ?: transaction.timestampEpoch
    val formatter = SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault())
    val originalAvailable = remember(source?.originalMessageUri) {
        source?.originalMessageUri?.let { originalMessageExists(context, it) } == true
    }
    val sourceLabel = when {
        transaction.source.startsWith("sms") -> "SMS"
        transaction.source == "notification" -> "Notification"
        else -> transaction.source.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DSBridge.surface(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(DSBridge.accentBg(), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                    Lucide.Message(size = 21.dp, color = DSBridge.accent())
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Transaction source", style = DSTypography.headlineMedium, color = DSBridge.ink())
                    Text("$sourceLabel · ${formatter.format(Date(messageDate))}", style = DSTypography.bodySmall, color = DSBridge.inkMute())
                }
                IconButton(onClick = onDismiss) { Lucide.X(size = 20.dp, color = DSBridge.inkSoft()) }
            }

            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = DSBridge.background()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    DetailRow("Sender", source?.sender ?: source?.packageName ?: "Unavailable")
                    DetailRow("Amount", formatCents(transaction.amountCents))
                    DetailRow("Merchant", transaction.merchantRaw)
                    DetailRow("Type", transaction.type.replaceFirstChar { it.uppercase() })
                    DetailRow("Category", category?.name ?: "Uncategorized")
                    DetailRow("Account", account?.name ?: "Unknown")
                    DetailRow("Confidence", "${((source?.confidence ?: transaction.confidenceScore) * 100).toInt()}%")
                    if (transaction.source.startsWith("sms")) DetailRow("Original", if (originalAvailable) "Available in Messages" else "Deleted or unavailable")
                    source?.capturedAt?.let { DetailRow("Captured", formatter.format(Date(it))) }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("MESSAGE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute())
            Spacer(Modifier.height(7.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = DSBridge.background()) {
                Text(
                    body ?: "The original message was deleted or is no longer available. Enable transaction message saving to keep future sources.",
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = DSTypography.bodyMedium,
                    color = if (body == null) DSBridge.inkMute() else DSBridge.ink()
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (source?.originalMessageUri != null && originalAvailable) {
                    OutlinedButton(
                        onClick = { openOriginalMessage(context, source.originalMessageUri) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Lucide.OpenInNew(size = 17.dp)
                        Spacer(Modifier.width(7.dp))
                        Text("Open original", fontSize = 11.sp)
                    }
                }
                if (body != null) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Transaction source", body))
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Lucide.Copy(size = 17.dp)
                        Spacer(Modifier.width(7.dp))
                        Text("Copy", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = DSTypography.labelSmall, color = DSBridge.inkMute(), modifier = Modifier.width(84.dp))
        Text(value, style = DSTypography.bodySmall, color = DSBridge.ink(), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

private fun openOriginalMessage(context: Context, rawUri: String) {
    val opened = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(rawUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
    if (!opened) Toast.makeText(context, "The original message is no longer available", Toast.LENGTH_LONG).show()
}

private fun originalMessageExists(context: Context, rawUri: String): Boolean = runCatching {
    context.contentResolver.query(Uri.parse(rawUri), arrayOf("_id"), null, null, null)?.use { it.moveToFirst() } == true
}.getOrDefault(false)
