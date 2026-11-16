package com.nudge.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.DSBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CaptureRulesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val db = remember { NudgeDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<MerchantAliasEntity>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var editing by remember { mutableStateOf<MerchantAliasEntity?>(null) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        rules = withContext(Dispatchers.IO) { db.captureRuleDao().getAliases() }
        categories = withContext(Dispatchers.IO) { db.categoryDao().getAllOnce() }
        ready = true
    }
    AlertDialog(onDismissRequest = onDismiss, containerColor = DSBridge.surface(), title = { Text("Learned rules") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your corrections teach Nudge on this device. Edit a category or forget a rule to stop applying it.", fontSize = 12.sp)
            if (!ready) Text("Loading…") else if (rules.isEmpty()) Text("No rules yet. Review a captured transaction to teach Nudge.")
            rules.forEach { rule ->
                Column {
                    Text(rule.normalizedName, color = DSBridge.ink())
                    Text(if (CaptureLearning.isRejectedSuggestion(rule.suggestedCategoryId)) "Ignore matching captures" else categories.firstOrNull { it.id == rule.suggestedCategoryId }?.name ?: "Merchant name correction", color = DSBridge.inkSoft(), fontSize = 11.sp)
                    Row {
                        if (!CaptureLearning.isRejectedSuggestion(rule.suggestedCategoryId)) TextButton(onClick = { editing = rule }) { Text("Category") }
                        TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { db.captureRuleDao().deleteAlias(rule.id) }; rules = rules.filterNot { it.id == rule.id } } }) { Text("Forget rule") }
                    }
                    HorizontalDivider()
                }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
    editing?.let { rule ->
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Category for ${rule.normalizedName}") },
            text = { Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                categories.filter { !it.isArchived }.forEach { category -> TextButton(onClick = {
                    scope.launch {
                        val updated = rule.copy(suggestedCategoryId = category.id)
                        withContext(Dispatchers.IO) { db.captureRuleDao().upsertAlias(updated) }
                        rules = rules.map { if (it.id == rule.id) updated else it }
                        editing = null
                    }
                }) { Text("${category.name} · ${category.type}") } }
            } }, confirmButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
}
