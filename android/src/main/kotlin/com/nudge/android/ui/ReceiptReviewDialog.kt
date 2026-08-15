package com.nudge.android.ui

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.CategoryEntity
import com.nudge.android.importer.DetailedReceiptDraft
import com.nudge.android.importer.ReceiptLineDraft
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptReviewDialog(
    initial: DetailedReceiptDraft,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (DetailedReceiptDraft, String, String?, Boolean, (Boolean, String) -> Unit) -> Unit,
) {
    var merchant by remember { mutableStateOf(initial.merchant) }
    val items = remember { initial.items.toMutableStateList() }
    var itemized by remember { mutableStateOf(false) }
    var selectedAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val detectedItemSubtotal = items.sumOf { it.lineTotalCents }
    // A structured GST table's taxable subtotal is more authoritative than an OCR sum of product
    // rows. The latter may include MRP/rate columns or a damaged line and is shown for review only.
    val expected = (initial.subtotalCents ?: detectedItemSubtotal) - initial.discountCents + initial.taxCents + initial.feeCents + initial.tipCents + initial.roundingCents
    val mismatch = initial.printedTotalCents - expected
    val displayedSavings = initial.savingsCents + initial.discountCents
    val expenseCategories = remember(categories) { categories.filter { it.type == "expense" && !it.isArchived } }
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive && !it.isArchived } }
    val previewPath = initial.pages.firstOrNull()?.localUri?.let { Uri.parse(it).path }
    val previewBitmap by produceState<android.graphics.Bitmap?>(null, previewPath) {
        value = withContext(Dispatchers.IO) {
            previewPath?.let { path ->
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, bounds)
                    val largest = maxOf(bounds.outWidth, bounds.outHeight)
                    val sample = generateSequence(1) { it * 2 }.takeWhile { largest / it > 1200 }.lastOrNull() ?: 1
                    decodeOrientedBitmap(path, sample)
                }.getOrNull()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier.fillMaxSize().background(DSBridge.background().copy(alpha = .72f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
        Surface(
            modifier = Modifier.fillMaxHeight().widthIn(max = 900.dp).fillMaxWidth().padding(top = 10.dp),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), color = DSBridge.surface(),
        ) {
            // Keep the entire footer above the physical screen edge. Some OEM gesture-nav
            // implementations report no bottom inset inside a Dialog, so relying only on
            // WindowInsets can still clip the button's lower corners.
            Column(Modifier.fillMaxSize().padding(bottom = 40.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = onDismiss, modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = DSBridge.background()) {
                        Box(contentAlignment = Alignment.Center) { Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft()) }
                    }
                    Text("Review receipt", Modifier.weight(1f), color = DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = DSBridge.accentBg()) {
                        Box(contentAlignment = Alignment.Center) { Lucide.ListTodo(size = 21.dp, color = DSBridge.accent()) }
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(shape = RoundedCornerShape(24.dp), color = DSBridge.accentBg()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            previewBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Captured receipt preview",
                                    modifier = Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            BasicTextField(
                                value = merchant, onValueChange = { merchant = it }, singleLine = true,
                                textStyle = TextStyle(color = DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                cursorBrush = SolidColor(DSBridge.accent()), modifier = Modifier.fillMaxWidth(),
                            )
                            Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(initial.purchaseTimestamp)), color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Column(Modifier.weight(1f)) {
                                    Text("PRINTED TOTAL", color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp)
                                    Text("₹${money(initial.printedTotalCents)}", color = DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                                }
                                Text("${items.size} items · ${initial.pages.size} ${if (initial.pages.size == 1) "scan" else "scans"}", color = DSBridge.accent(), fontFamily = MonoFamily, fontSize = 10.sp)
                            }
                        }
                    }

                    if (displayedSavings > 0L) Surface(shape = RoundedCornerShape(18.dp), color = DS.Positive.copy(alpha = .12f)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Sparkles(size = 20.dp, color = DS.Positive)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("You saved ₹${money(displayedSavings)}", color = DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("MRP savings already included in the printed total", color = DSBridge.inkSoft(), fontSize = 10.sp)
                            }
                            if (initial.taxCents > 0L) Text("GST ₹${money(initial.taxCents)}", color = DSBridge.inkSoft(), fontFamily = MonoFamily, fontSize = 10.sp)
                        }
                    }

                    if (abs(mismatch) > 100) Surface(shape = RoundedCornerShape(18.dp), color = DS.Warning.copy(alpha = .14f)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Info(size = 20.dp, color = DS.Warning)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Total mismatch · ₹${money(abs(mismatch))}", color = DSBridge.ink(), fontWeight = FontWeight.Bold, fontFamily = MonoFamily, fontSize = 12.sp)
                                Text("Printed total will be preserved. Review highlighted or missing rows.", color = DSBridge.inkSoft(), fontSize = 10.sp)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SaveModeCard("One expense", "Keep items inside receipt", !itemized, Modifier.weight(1f)) { itemized = false }
                        SaveModeCard("Separate items", "Linked under this receipt", itemized, Modifier.weight(1f)) { itemized = true }
                    }

                    Text("DETECTED ITEMS", color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    if (items.isEmpty()) Surface(shape = RoundedCornerShape(18.dp), color = DSBridge.background()) {
                        Text("No reliable product rows were detected. You can still save the printed total as one expense.", Modifier.padding(16.dp), color = DSBridge.inkSoft(), fontSize = 11.sp)
                    }
                    items.forEachIndexed { index, item ->
                        ReceiptItemEditor(item, index, expenseCategories, onChange = { items[index] = it }, onRemove = { items.removeAt(index) })
                    }
                    Surface(
                        onClick = { items += ReceiptLineDraft("New item", lineTotalCents = 0L, confidence = 1f) },
                        shape = RoundedCornerShape(16.dp), color = DSBridge.background(), modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Plus(size = 18.dp, color = DSBridge.accent()); Spacer(Modifier.width(7.dp)); Text("Add missing item", color = DSBridge.accent(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Text("ACCOUNT", color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeAccounts.forEach { account -> ReviewChip(account.name, selectedAccount == account.id) { selectedAccount = account.id } }
                    }
                    if (!itemized) {
                        Text("CATEGORY", color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.2.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            expenseCategories.forEach { category -> ReviewChip(category.name, selectedCategory == category.id) { selectedCategory = category.id } }
                        }
                    }

                    Surface(shape = RoundedCornerShape(18.dp), color = DSBridge.background()) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            TotalRow("Detected products", detectedItemSubtotal)
                            initial.subtotalCents?.takeIf { it != detectedItemSubtotal }?.let { TotalRow("Taxable subtotal", it) }
                            if (initial.savingsCents > 0) TotalRow("MRP savings", initial.savingsCents)
                            if (initial.discountCents > 0) TotalRow("Applied discount", -initial.discountCents)
                            if (initial.taxCents > 0) TotalRow("Total GST", initial.taxCents)
                            if (initial.feeCents > 0) TotalRow("Fees", initial.feeCents)
                            if (initial.tipCents > 0) TotalRow("Tip", initial.tipCents)
                            HorizontalDivider(color = DSBridge.inkMute().copy(.12f))
                            TotalRow("Printed total", initial.printedTotalCents, true)
                        }
                    }
                    status?.let { Text(it, color = if (it.startsWith("This receipt")) DS.Signal else DSBridge.inkSoft(), fontSize = 11.sp, fontFamily = MonoFamily) }
                    Spacer(Modifier.height(8.dp))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DSBridge.surface(),
                    shadowElevation = 10.dp,
                ) {
                Button(
                    onClick = {
                        val account = selectedAccount ?: return@Button
                        saving = true; status = null
                        val updated = initial.copy(merchant = merchant.trim().ifBlank { initial.merchant }, items = items.toList())
                        onSave(updated, account, selectedCategory, itemized) { success, message ->
                            saving = false; status = message
                            if (success) onDismiss()
                        }
                    },
                    enabled = !saving && selectedAccount != null && initial.printedTotalCents > 0 && (!itemized || items.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DS.Signal, contentColor = DS.InkPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = DS.InkPrimary)
                    else Text(
                        if (itemized) "Add ${items.size} linked items" else "Add as one expense",
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                }
            }
        }
        }
    }
}

@Composable private fun SaveModeCard(title: String, subtitle: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = if (selected) DSBridge.accentBg() else DSBridge.background(), border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, DSBridge.accent()) else null) {
        Column(Modifier.padding(14.dp)) { Text(title, color = if (selected) DSBridge.accent() else DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp); Spacer(Modifier.height(4.dp)); Text(subtitle, color = DSBridge.inkMute(), fontSize = 9.sp, maxLines = 2) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ReceiptItemEditor(item: ReceiptLineDraft, index: Int, categories: List<CategoryEntity>, onChange: (ReceiptLineDraft) -> Unit, onRemove: () -> Unit) {
    var categoryMenu by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == item.selectedCategoryId }
    Surface(shape = RoundedCornerShape(18.dp), color = DSBridge.background()) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = DSBridge.accentBg()) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = DSBridge.accent(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold) } }
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    item.name,
                    { onChange(item.copy(name = it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    cursorBrush = SolidColor(DSBridge.accent()),
                    modifier = Modifier.weight(1f),
                )
                Surface(onClick = onRemove, modifier = Modifier.size(36.dp), shape = RoundedCornerShape(11.dp), color = DS.Negative.copy(.10f)) {
                    Box(contentAlignment = Alignment.Center) { Lucide.X(size = 17.dp, color = DS.Negative) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptNumberField("Qty", formatQuantity(item.quantity), Modifier.weight(.75f)) { raw ->
                    raw.toDoubleOrNull()?.takeIf { it > 0 }?.let { quantity ->
                        val unit = item.unitPriceCents ?: (item.lineTotalCents / item.quantity.coerceAtLeast(1.0)).roundToLong()
                        onChange(item.copy(quantity = quantity, unitPriceCents = unit, lineTotalCents = (quantity * unit).roundToLong()))
                    }
                }
                ReceiptNumberField("Unit ₹", money(item.unitPriceCents ?: item.lineTotalCents), Modifier.weight(1f)) { raw ->
                    raw.toDoubleOrNull()?.takeIf { it >= 0 }?.let { value ->
                        val unit = (value * 100).roundToLong()
                        onChange(item.copy(unitPriceCents = unit, lineTotalCents = (item.quantity * unit).roundToLong()))
                    }
                }
                ReceiptNumberField("Total ₹", money(item.lineTotalCents), Modifier.weight(1f)) { raw ->
                    raw.toDoubleOrNull()?.takeIf { it >= 0 }?.let { value ->
                        val total = (value * 100).roundToLong()
                        onChange(item.copy(lineTotalCents = total, unitPriceCents = (total / item.quantity.coerceAtLeast(1.0)).roundToLong()))
                    }
                }
            }
            Box {
                Surface(onClick = { categoryMenu = true }, shape = RoundedCornerShape(12.dp), color = DSBridge.accentBg()) {
                    Text(
                        category?.name ?: item.categoryHint ?: "Choose category",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        color = DSBridge.accent(), fontSize = 10.sp, fontFamily = MonoFamily, maxLines = 1,
                    )
                }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name, fontFamily = MonoFamily, fontSize = 11.sp) },
                            onClick = { onChange(item.copy(selectedCategoryId = option.id)); categoryMenu = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptNumberField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(DSBridge.surfaceVariant()).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 8.sp, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
            singleLine = true,
            textStyle = TextStyle(color = DSBridge.ink(), fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp),
            cursorBrush = SolidColor(DSBridge.accent()),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable private fun ReviewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = if (selected) DSBridge.accentBg() else DSBridge.background(), border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, DSBridge.accent()) else null) { Text(label, Modifier.padding(horizontal = 13.dp, vertical = 10.dp), color = if (selected) DSBridge.accent() else DSBridge.inkSoft(), fontFamily = MonoFamily, fontSize = 10.sp) }
}
@Composable private fun TotalRow(label: String, cents: Long, bold: Boolean = false) { Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f), color = DSBridge.inkSoft(), fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal); Text("${if (cents < 0) "−" else ""}₹${money(abs(cents))}", color = DSBridge.ink(), fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal) } }
private fun money(cents: Long): String = if (cents % 100L == 0L) (cents / 100L).toString() else "%.2f".format(Locale.US, cents / 100.0)
private fun formatQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale.US, value)

private fun decodeOrientedBitmap(path: String, sample: Int): android.graphics.Bitmap? {
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val orientation = runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return bitmap
    return runCatching {
        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
            .also { if (it !== bitmap) bitmap.recycle() }
    }.getOrElse { bitmap }
}
