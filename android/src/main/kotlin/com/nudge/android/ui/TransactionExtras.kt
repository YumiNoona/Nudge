package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.FriendEntity
import com.nudge.android.data.RecurrenceDraft
import com.nudge.android.data.SplitDraft
import com.nudge.android.data.SplitMemberDraft
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.components.NudgeModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun TransactionExtrasRow(
    timestampEpoch: Long,
    split: SplitDraft?,
    recurrence: RecurrenceDraft?,
    onDateClick: () -> Unit,
    onSplitClick: () -> Unit,
    onRepeatClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExtraChip(onDateClick, { Lucide.Calendar(size = 16.dp) }, formatFriendlyDate(timestampEpoch), timestampEpoch != 0L)
        ExtraChip(onSplitClick, { Lucide.User(size = 16.dp) }, split?.let { "Split · ${it.members.size}" } ?: "Split", split != null)
        ExtraChip(
            onRepeatClick,
            { Lucide.RefreshCw(size = 16.dp) },
            recurrence?.interval?.let(::recurrenceLabel) ?: "Repeat",
            recurrence != null,
        )
    }
}

@Composable
private fun ExtraChip(onClick: () -> Unit, icon: @Composable () -> Unit, label: String, active: Boolean) {
    val container by animateColorAsState(
        if (active) DSBridge.accentBg() else DSBridge.background(),
        label = "extra-chip-color",
    )
    val scale by animateFloatAsState(if (active) 1f else .98f, label = "extra-chip-scale")
    Surface(
        onClick = onClick,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .14f)),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontFamily = MonoFamily, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDateDialog(initialEpoch: Long, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSelect(mergePickedDate(it, initialEpoch)) }
                onDismiss()
            }) { Text("Choose") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state, title = { Text("Transaction date", Modifier.padding(24.dp)) }) }
}

@Composable
fun RecurrenceDialog(current: RecurrenceDraft?, onDismiss: () -> Unit, onSelect: (RecurrenceDraft?) -> Unit) {
    val options = listOf(null to "Does not repeat", "weekly" to "Every week", "monthly" to "Every month", "yearly" to "Every year")
    var interval by remember(current) { mutableStateOf(current?.interval) }
    var endEpoch by remember(current) { mutableStateOf(current?.endEpoch) }
    var showEndDate by remember { mutableStateOf(false) }
    NudgeModal(
        title = "Repeat transaction",
        subtitle = "Create the next entry automatically on schedule.",
        onDismiss = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (value, label) ->
                    val selected = interval == value
                    Surface(
                        onClick = { interval = value; if (value == null) endEpoch = null },
                        shape = RoundedCornerShape(15.dp),
                        color = if (selected) DSBridge.accentBg() else DSBridge.background(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) DSBridge.accent().copy(.35f) else DSBridge.inkMute().copy(.10f)),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = DSBridge.ink())
                        }
                    }
                }
                if (interval != null) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        onClick = { showEndDate = true },
                        shape = RoundedCornerShape(15.dp),
                        color = DSBridge.background(),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Lucide.Calendar(size = 18.dp, color = DSBridge.accent())
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Ends", style = MaterialTheme.typography.labelMedium, color = DSBridge.inkSoft())
                                Text(endEpoch?.let(::formatFriendlyDate) ?: "No end date", style = MaterialTheme.typography.bodyMedium, color = DSBridge.ink())
                            }
                            if (endEpoch != null) TextButton(onClick = { endEpoch = null }) { Text("Clear") }
                            else Lucide.ChevronRight(size = 18.dp, color = DSBridge.inkMute())
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Cancel") }
            Button(
                onClick = { onSelect(interval?.let { RecurrenceDraft(it, endEpoch) }); onDismiss() },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Save schedule") }
        },
    )
    if (showEndDate) TransactionDateDialog(
        initialEpoch = endEpoch ?: System.currentTimeMillis(),
        onDismiss = { showEndDate = false },
        onSelect = { endEpoch = it },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitExpenseDialog(
    amountCents: Long,
    friends: List<FriendEntity>,
    initial: SplitDraft?,
    onCreateFriend: (String) -> FriendEntity,
    onDismiss: () -> Unit,
    onSave: (SplitDraft?) -> Unit,
) {
    var selectedIds by remember(initial) {
        mutableStateOf(initial?.members.orEmpty().mapNotNull { it.friendId }.toSet())
    }
    var method by remember(initial) { mutableStateOf(initial?.method ?: "equal") }
    var payerId by remember(initial) {
        mutableStateOf(initial?.members?.firstOrNull { it.paidCents > 0 }?.friendId)
    }
    var custom by remember(initial) {
        mutableStateOf(initial?.members.orEmpty().associate { (it.friendId ?: "me") to (it.shareCents / 100.0).toString() })
    }
    var newFriend by remember { mutableStateOf("") }
    val selectedFriends = friends.filter { it.id in selectedIds }
    val people = listOf(null to "You") + selectedFriends.map { it.id to it.name }
    val parsed = people.associate { (id, _) -> (id ?: "me") to (custom[id ?: "me"]?.toDoubleOrNull() ?: 0.0) }
    val validCustom = amountCents > 0 && parsed.values.all { it >= 0.0 } && when (method) {
        "exact" -> kotlin.math.abs(parsed.values.sum() * 100 - amountCents) < 1.0
        "percentage" -> kotlin.math.abs(parsed.values.sum() - 100.0) < .01
        else -> true
    }
    val cleanFriendName = newFriend.trim().replace(Regex("\\s+"), " ")
    val canAddFriend = cleanFriendName.length >= 2 && friends.none { it.name.equals(cleanFriendName, ignoreCase = true) }
    val equalShare = if (people.isNotEmpty()) amountCents / people.size else amountCents
    val enteredAmountCents = when (method) {
        "exact" -> (parsed.values.sum() * 100).roundToLong()
        "percentage" -> (amountCents * parsed.values.sum() / 100.0).roundToLong()
        else -> amountCents
    }
    val remainingCents = amountCents - enteredAmountCents
    LaunchedEffect(selectedIds) {
        if (payerId != null && payerId !in selectedIds) payerId = null
    }

    NudgeModal(
        title = "Split expense",
        subtitle = "${formatMoney(amountCents)} · you plus ${selectedFriends.size} ${if (selectedFriends.size == 1) "friend" else "friends"}",
        onDismiss = onDismiss,
        modifier = Modifier.heightIn(max = 760.dp),
        content = {
            Column(Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = DSBridge.accentBg(),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Bill total", color = DSBridge.inkSoft(), fontSize = 11.sp)
                            Text(formatMoney(amountCents), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Each", color = DSBridge.inkSoft(), fontSize = 11.sp)
                            Text(
                                if (method == "equal") formatMoney(equalShare) else "Custom",
                                color = DSBridge.accent(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
                Text("Who shared it?", color = DSBridge.ink(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Surface(color = DSBridge.surfaceVariant(), shape = RoundedCornerShape(15.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SplitAvatar("You", selected = true)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("You", fontWeight = FontWeight.SemiBold)
                            Text("Always included", color = DSBridge.inkMute(), fontSize = 11.sp)
                        }
                        CompositionLocalProvider(LocalContentColor provides DSBridge.accent()) {
                            Lucide.Check(size = 18.dp)
                        }
                    }
                }
                if (friends.isEmpty()) Text("Add a friend to begin", color = DSBridge.inkMute(), fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    friends.forEach { friend ->
                        FilterChip(
                            selected = friend.id in selectedIds,
                            onClick = { selectedIds = if (friend.id in selectedIds) selectedIds - friend.id else selectedIds + friend.id },
                            leadingIcon = { SplitAvatar(friend.name, friend.id in selectedIds, 24.dp) },
                            label = { Text(friend.name, maxLines = 1) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newFriend,
                        onValueChange = { newFriend = it },
                        placeholder = { Text("Friend's name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = {
                        if (canAddFriend) {
                            val friend = onCreateFriend(cleanFriendName)
                            selectedIds = selectedIds + friend.id
                            newFriend = ""
                        }
                    }, enabled = canAddFriend) { Lucide.Plus(size = 18.dp) }
                }
                if (selectedFriends.isNotEmpty()) {
                    Text("How should it be split?", color = DSBridge.ink(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        listOf(
                            Triple("equal", "Equal", "Same share"),
                            Triple("exact", "Amounts", "Enter ₹"),
                            Triple("percentage", "Percent", "Must total 100%"),
                        ).forEach { (id, label, hint) ->
                            SplitMethodCard(
                                label = label,
                                hint = hint,
                                selected = method == id,
                                onClick = { method = id },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text("Who paid?", color = DSBridge.ink(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        people.forEach { (id, name) ->
                            FilterChip(
                                selected = payerId == id,
                                onClick = { payerId = id },
                                leadingIcon = { SplitAvatar(name, payerId == id, 24.dp) },
                                label = { Text(name, maxLines = 1) },
                            )
                        }
                    }
                    if (method != "equal") {
                        Text("Shares", color = DSBridge.ink(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        people.forEach { (id, name) ->
                            val key = id ?: "me"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SplitAvatar(name, selected = true, size = 34.dp)
                                Spacer(Modifier.width(10.dp))
                                OutlinedTextField(
                                    value = custom[key].orEmpty(),
                                    onValueChange = { custom = custom + (key to it.filter { char -> char.isDigit() || char == '.' }) },
                                    label = { Text(name) },
                                    suffix = { Text(if (method == "percentage") "%" else "₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                            }
                        }
                        Surface(
                            color = if (validCustom) DSBridge.accentBg() else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (validCustom) "Ready to split" else "Remaining", fontSize = 12.sp)
                                Text(
                                    if (method == "percentage") "${100.0 - parsed.values.sum()}%" else formatMoney(remainingCents),
                                    color = if (validCustom) DSBridge.accent() else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = {
                if (initial != null) onSave(null)
                onDismiss()
            }, modifier = Modifier.height(48.dp)) { Text(if (initial == null) "Cancel" else "Remove") }
            TextButton(
                enabled = selectedFriends.isNotEmpty() && validCustom,
                onClick = {
                    val count = people.size
                    var distributed = 0L
                    val members = people.mapIndexed { index, (id, name) ->
                        val key = id ?: "me"
                        val share = when (method) {
                            "exact" -> ((custom[key]?.toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                            "percentage" -> if (index == count - 1) amountCents - distributed else (amountCents * ((custom[key]?.toDoubleOrNull() ?: 0.0) / 100.0)).roundToLong()
                            else -> if (index == count - 1) amountCents - distributed else amountCents / count
                        }
                        distributed += share
                        SplitMemberDraft(id, name, share, if (payerId == id) amountCents else 0)
                    }
                    onSave(SplitDraft(method, members))
                    onDismiss()
                },
                modifier = Modifier.height(48.dp),
            ) { Text("Save split") }
        },
    )
}

@Composable
private fun SplitAvatar(name: String, selected: Boolean, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 2)).background(
            if (selected) DSBridge.accentBg() else DSBridge.background(),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            color = if (selected) DSBridge.accent() else DSBridge.inkSoft(),
            fontWeight = FontWeight.Bold,
            fontSize = if (size <= 24.dp) 10.sp else 13.sp,
        )
    }
}

@Composable
private fun SplitMethodCard(
    label: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 72.dp),
        color = if (selected) DSBridge.accentBg() else DSBridge.surfaceVariant(),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) DSBridge.accent() else MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
            Text(hint, color = DSBridge.inkMute(), fontSize = 9.sp, maxLines = 2)
        }
    }
}

private fun recurrenceLabel(interval: String): String = when (interval.lowercase()) {
    "weekly" -> "Weekly"
    "monthly" -> "Monthly"
    "yearly" -> "Yearly"
    else -> interval.replaceFirstChar(Char::uppercase)
}

private fun formatMoney(cents: Long): String = "₹" + java.text.NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = if (cents % 100L == 0L) 0 else 2
    maximumFractionDigits = 2
}.format(cents / 100.0)

private fun formatFriendlyDate(epoch: Long): String {
    val date = Calendar.getInstance().apply { timeInMillis = epoch }
    val today = Calendar.getInstance()
    if (date.get(Calendar.YEAR) == today.get(Calendar.YEAR) && date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) return "Today"
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date.time)
}

private fun mergePickedDate(pickedUtcEpoch: Long, originalEpoch: Long): Long {
    val picked = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtcEpoch }
    return Calendar.getInstance().apply {
        timeInMillis = originalEpoch
        set(Calendar.YEAR, picked.get(Calendar.YEAR))
        set(Calendar.MONTH, picked.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
