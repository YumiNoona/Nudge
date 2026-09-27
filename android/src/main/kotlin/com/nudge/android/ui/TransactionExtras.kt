package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

@Composable
fun TransactionDateDialog(initialEpoch: Long, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val seed = remember(initialEpoch) { Calendar.getInstance().apply { timeInMillis = initialEpoch } }
    val today = remember { Calendar.getInstance() }
    var visibleMonth by remember(initialEpoch) {
        mutableStateOf(Calendar.getInstance().apply {
            clear()
            set(seed.get(Calendar.YEAR), seed.get(Calendar.MONTH), 1)
        })
    }
    var selectedDay by remember(initialEpoch) { mutableIntStateOf(seed.get(Calendar.DAY_OF_MONTH)) }
    var mode by remember { mutableStateOf(PickerMode.Day) }
    val daysInMonth = visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = (visibleMonth.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val monthLabel = remember(visibleMonth.timeInMillis) {
        SimpleDateFormat("MMM", Locale.getDefault()).format(visibleMonth.time)
    }
    val year = visibleMonth.get(Calendar.YEAR)
    val yearPageStart = (year / 12) * 12
    val shortYear = (year % 100).toString().padStart(2, '0')

    fun shift(amount: Int) {
        visibleMonth = (visibleMonth.clone() as Calendar).apply {
            when (mode) {
                PickerMode.Day -> add(Calendar.MONTH, amount)
                PickerMode.Month -> add(Calendar.YEAR, amount)
                PickerMode.Year -> add(Calendar.YEAR, amount * 12)
            }
        }
        selectedDay = selectedDay.coerceAtMost(visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
    }

    fun jumpToToday() {
        visibleMonth = Calendar.getInstance().apply {
            clear()
            set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), 1)
        }
        selectedDay = today.get(Calendar.DAY_OF_MONTH)
        mode = PickerMode.Day
    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.92f).widthIn(max = 350.dp),
            shape = RoundedCornerShape(28.dp),
            color = DSBridge.surface(),
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .10f)),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transaction date", fontSize = 12.sp, fontFamily = MonoFamily, color = DSBridge.inkSoft())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { shift(-1) },
                        modifier = Modifier.size(42.dp),
                    ) { Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft()) }
                    Row(
                        Modifier.weight(1f).height(52.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DateHeaderPart(selectedDay.toString(), 52.dp, mode == PickerMode.Day) { jumpToToday() }
                        DateHeaderPart(monthLabel, 78.dp, mode == PickerMode.Month) { mode = PickerMode.Month }
                        DateHeaderPart(shortYear, 52.dp, mode == PickerMode.Year) { mode = PickerMode.Year }
                    }
                    IconButton(
                        onClick = { shift(1) },
                        modifier = Modifier.size(42.dp),
                    ) { Lucide.ChevronRight(size = 22.dp, color = DSBridge.inkSoft()) }
                }
                var dragAmount by remember { mutableFloatStateOf(0f) }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(238.dp)
                        .pointerInput(mode) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAmount = 0f },
                                onHorizontalDrag = { change, drag ->
                                    dragAmount += drag
                                    change.consume()
                                },
                                onDragEnd = {
                                    when {
                                        dragAmount > 42f -> shift(-1)
                                        dragAmount < -42f -> shift(1)
                                    }
                                    dragAmount = 0f
                                },
                                onDragCancel = { dragAmount = 0f },
                            )
                        },
                ) {
                    when (mode) {
                        PickerMode.Day -> Column(Modifier.height(238.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                                    Text(label, Modifier.width(38.dp), textAlign = TextAlign.Center, fontSize = 10.sp, fontFamily = MonoFamily, color = DSBridge.inkMute())
                                }
                            }
                            (0 until 6).forEach { week ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    (0 until 7).forEach { dayOfWeek ->
                                        val number = week * 7 + dayOfWeek - firstDayOffset + 1
                                        val inMonth = number in 1..daysInMonth
                                        val selected = inMonth && number == selectedDay
                                        Surface(
                                            onClick = { if (inMonth) selectedDay = number },
                                            enabled = inMonth,
                                            modifier = Modifier.size(37.dp),
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (selected) DSBridge.accentBg() else Color.Transparent,
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    if (inMonth) number.toString() else "",
                                                    fontSize = 14.sp,
                                                    fontFamily = MonoFamily,
                                                    color = if (selected) DSBridge.accent() else DSBridge.ink(),
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PickerMode.Month -> Column(Modifier.height(238.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SimpleDateFormat("MMM", Locale.getDefault()).let { formatter ->
                                (0 until 4).forEach { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        (0 until 3).forEach { column ->
                                            val month = row * 3 + column
                                            val selected = month == visibleMonth.get(Calendar.MONTH)
                                            Surface(
                                                onClick = {
                                                    visibleMonth = (visibleMonth.clone() as Calendar).apply {
                                                        set(Calendar.MONTH, month)
                                                        selectedDay = selectedDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
                                                    }
                                                    mode = PickerMode.Day
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (selected) DSBridge.accentBg() else DSBridge.surfaceVariant(),
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(formatter.format(Calendar.getInstance().apply { set(Calendar.MONTH, month) }.time), fontFamily = MonoFamily, fontSize = 14.sp, color = if (selected) DSBridge.accent() else DSBridge.ink())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PickerMode.Year -> Column(Modifier.height(238.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0 until 4).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (0 until 3).forEach { column ->
                                        val option = yearPageStart + row * 3 + column
                                        val selected = option == year
                                        Surface(
                                            onClick = {
                                                visibleMonth = (visibleMonth.clone() as Calendar).apply {
                                                    set(Calendar.YEAR, option)
                                                    selectedDay = selectedDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
                                                }
                                                mode = PickerMode.Month
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (selected) DSBridge.accentBg() else DSBridge.surfaceVariant(),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(option.toString(), fontFamily = MonoFamily, fontSize = 14.sp, color = if (selected) DSBridge.accent() else DSBridge.ink())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, modifier = Modifier.height(42.dp)) { Text("Cancel", fontFamily = MonoFamily) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val picked = (visibleMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, selectedDay) }
                            onSelect(mergePickedDate(picked.timeInMillis, initialEpoch))
                            onDismiss()
                        },
                        modifier = Modifier.height(42.dp),
                    ) { Text("Choose", fontFamily = MonoFamily, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun DateHeaderPart(label: String, width: androidx.compose.ui.unit.Dp, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .width(width)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DSBridge.surfaceVariant() else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            maxLines = 1,
            textAlign = TextAlign.Center,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) DSBridge.accent() else DSBridge.ink(),
            fontFamily = MonoFamily,
        )
    }
}

private enum class PickerMode { Day, Month, Year }

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
    val picked = Calendar.getInstance().apply { timeInMillis = pickedUtcEpoch }
    return Calendar.getInstance().apply {
        timeInMillis = originalEpoch
        set(Calendar.YEAR, picked.get(Calendar.YEAR))
        set(Calendar.MONTH, picked.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
