package com.nudge.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.DSBridge

@Composable
fun LinkedSourcesSheet(transaction: TransactionEntity, sources: List<SavedSourceMessageEntity>,
    account: AccountEntity?, category: CategoryEntity?, decrypt: (SavedSourceMessageEntity?) -> String?, onDismiss: () -> Unit) {
    var selected by remember(transaction.id) { mutableStateOf<SavedSourceMessageEntity?>(sources.firstOrNull()) }
    var choosing by remember(transaction.id) { mutableStateOf(sources.size > 1) }
    if (choosing) {
        AlertDialog(onDismissRequest = onDismiss, containerColor = DSBridge.surface(), title = { Text("Linked sources") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("These messages describe the same payment. Choose a source to inspect.")
                sources.forEach { source -> TextButton(onClick = { selected = source; choosing = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("${source.sourceType.uppercase()} · ${source.sender ?: source.packageName ?: "Captured message"}")
                } }
            } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
    } else SourceMessageSheet(transaction, selected, decrypt(selected), account, category, onDismiss)
}
