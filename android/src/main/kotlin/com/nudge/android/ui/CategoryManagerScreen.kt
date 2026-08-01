package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.ui.theme.*
import com.nudge.model.CategoryType

@Composable
fun CategoryManagerScreen(
    categories: List<CategoryEntity>,
    onAdd: (String, CategoryType, String?, String?) -> Unit,
    onUpdate: (CategoryEntity) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }
    var typeFilter by remember { mutableStateOf("expense") }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Lucide.ArrowLeft(size = 22.dp, color = DSBridge.inkSoft()) }
            Column(Modifier.weight(1f)) {
                Text("Categories", style = DSTypography.headlineLarge, color = DSBridge.ink())
                Text("Teach Nudge how you spend", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            }
            Button(
                onClick = { creating = true },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DS.Signal, contentColor = DS.InkPrimary),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Lucide.Plus(size = 18.dp, color = DS.InkPrimary)
                Spacer(Modifier.width(7.dp))
                Text("Add", fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("expense" to "Expenses", "income" to "Income").forEach { (id, label) ->
                FilterChip(selected = typeFilter == id, onClick = { typeFilter = id }, label = { Text(label) })
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories.filter { it.type == typeFilter }, key = { it.id }) { category ->
                val tint = NudgeColors.parse(category.color, DSBridge.accent())
                Surface(
                    onClick = { editing = category },
                    shape = RoundedCornerShape(20.dp),
                    color = DSBridge.surface()
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                            CategoryGlyph(category.icon, category.name, tint, Modifier.size(21.dp))
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category.name, style = DSTypography.titleLarge, color = DSBridge.ink())
                            Text(if (category.isDefault) "BUILT IN · EDITABLE" else "CUSTOM", style = DSTypography.labelSmall, color = DSBridge.inkMute())
                        }
                        IconButton(onClick = { editing = category }) { Lucide.Edit(size = 18.dp, color = DSBridge.inkSoft()) }
                        IconButton(onClick = { deleting = category }) { Lucide.Trash2(size = 18.dp, color = DS.Negative) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (creating || editing != null) {
        CategoryEditorSheet(
            category = editing,
            defaultType = typeFilter,
            onDismiss = { creating = false; editing = null },
            onSave = { name, type, icon, color ->
                editing?.let { onUpdate(it.copy(name = name, type = type, icon = icon, color = color)) }
                    ?: onAdd(name, if (type == "income") CategoryType.INCOME else CategoryType.EXPENSE, icon, color)
                creating = false
                editing = null
            }
        )
    }

    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${category.name}?") },
            text = { Text("Existing transactions will stay safe and become uncategorized.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(category.id); deleting = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DS.Negative)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryEditorSheet(
    category: CategoryEntity?,
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var type by remember { mutableStateOf(category?.type ?: defaultType) }
    val inferred = remember(category) {
        CategoryIcons.all.firstOrNull { it.key == category?.icon }?.key
            ?: CategoryIcons.all.first { it.image == CategoryIcons.resolve(null, category?.name.orEmpty()) }.key
    }
    var iconKey by remember { mutableStateOf(inferred) }
    val palette = listOf("#365244", "#1E9E62", "#3E6F8E", "#149A8B", "#E38B42", "#C65D4B", "#E5A524", "#607D68")
    var color by remember { mutableStateOf(category?.color ?: palette.first()) }
    var iconSearch by remember { mutableStateOf("") }
    val localContext = LocalContext.current
    val haptics = remember(localContext) { NudgeHaptics(localContext) }
    val shownIcons = remember(iconSearch) {
        CategoryIcons.all.filter { iconSearch.isBlank() || it.label.contains(iconSearch, true) || it.key.contains(iconSearch, true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DSBridge.surface(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text(if (category == null) "New category" else "Edit category", style = DSTypography.headlineMedium, color = DSBridge.ink())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == "expense", { type = "expense" }, { Text("Expense") })
                FilterChip(type == "income", { type = "income" }, { Text("Income") })
            }
            Spacer(Modifier.height(12.dp))
            Text("Color", style = DSTypography.labelMedium, color = DSBridge.inkSoft())
            Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { hex ->
                    val swatch = NudgeColors.parse(hex)
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(swatch.copy(alpha = .18f))
                            .clickable { color = hex; haptics.selection() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(if (color == hex) 23.dp else 17.dp).clip(CircleShape).background(swatch))
                    }
                }
            }
            OutlinedTextField(
                value = iconSearch,
                onValueChange = { iconSearch = it },
                leadingIcon = { Lucide.Search(size = 18.dp, color = DSBridge.inkMute()) },
                placeholder = { Text("Search ${CategoryIcons.all.size} icons") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(58.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(shownIcons, key = { it.key }) { option ->
                    val selected = iconKey == option.key
                    Box(
                        Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (selected) DSBridge.accentBg() else DSBridge.background())
                            .clickable { iconKey = option.key; haptics.selection() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(option.image, option.label, tint = if (selected) DSBridge.accent() else DSBridge.inkSoft(), modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { haptics.success(); onSave(name.trim(), type, iconKey, color) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent())
            ) { Text(if (category == null) "Create category" else "Save changes") }
        }
    }
}
