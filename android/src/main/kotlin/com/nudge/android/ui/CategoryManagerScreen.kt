package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.ui.theme.*
import com.nudge.model.CategoryType

@Composable
fun CategoryManagerScreen(categories: List<CategoryEntity>, onAdd: (String, CategoryType, String?, String?) -> Unit, onArchive: (String) -> Unit, onBack: () -> Unit) {
    var addDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft()) }
            Column(Modifier.weight(1f)) { Text("Categories", style = DSTypography.headlineLarge, color = DSBridge.ink()); Text("Teach Nudge how you spend", fontSize = 11.sp, color = DSBridge.inkMute()) }
            Surface(onClick = { addDialog = true }, shape = RoundedCornerShape(13.dp), color = DS.Signal) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Lucide.Plus(size = 17.dp, color = DS.InkPrimary); Spacer(Modifier.width(4.dp)); Text("ADD", fontFamily = MonoFamily, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                Surface(shape = RoundedCornerShape(17.dp), color = DSBridge.surface()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { Lucide.Tag(size = 19.dp, color = DSBridge.accent()) }
                        Column(Modifier.weight(1f)) { Text(category.name, fontWeight = FontWeight.Medium, color = DSBridge.ink()); Text(category.type.uppercase(), fontFamily = MonoFamily, fontSize = 8.sp, color = DSBridge.inkMute()) }
                        if (!category.isDefault) IconButton(onClick = { onArchive(category.id) }) { Lucide.Trash2(size = 17.dp, color = DS.Negative) }
                    }
                }
            }
        }
    }
    if (addDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(CategoryType.EXPENSE) }
        AlertDialog(onDismissRequest = { addDialog = false }, title = { Text("New category") }, text = { Column { OutlinedTextField(name, { name = it.take(28) }, label = { Text("Name") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(type == CategoryType.EXPENSE, { type = CategoryType.EXPENSE }, { Text("Expense") }); FilterChip(type == CategoryType.INCOME, { type = CategoryType.INCOME }, { Text("Income") }) } } }, confirmButton = { Button(onClick = { if (name.isNotBlank()) { onAdd(name.trim(), type, null, null); addDialog = false } }) { Text("Create") } }, dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } })
    }
}
