package com.nudge.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.*
import com.nudge.android.ui.theme.*
import com.nudge.model.CategoryType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun NeedsReviewSwipeScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    sources: List<SavedSourceMessageEntity>,
    onCategorize: (transactionId: String, categoryId: String) -> Unit,
    onCreateCategory: (transactionId: String, name: String, type: CategoryType, icon: String?, color: String?) -> Unit,
    decryptSource: (SavedSourceMessageEntity?) -> String?,
    onDismiss: (transactionId: String) -> Unit,
    onBack: () -> Unit
) {
    val queue = remember { mutableStateListOf<TransactionEntity>().apply { addAll(transactions) } }
    val seenIds = remember { transactions.mapTo(mutableSetOf()) { it.id } }
    var currentIndex by remember { mutableIntStateOf(0) }
    var transitionDirection by remember { mutableIntStateOf(-1) }
    var processing by remember { mutableStateOf(false) }
    var cardDrag by remember { mutableFloatStateOf(0f) }
    var cardWidth by remember { mutableFloatStateOf(1f) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = remember(context) { NudgeHaptics(context) }

    LaunchedEffect(transactions.map { it.id }) {
        val activeIds = transactions.mapTo(hashSetOf()) { it.id }
        queue.removeAll { it.id !in activeIds }
        transactions.filter { seenIds.add(it.id) }.forEach {
            queue.add(it)
        }
        if (queue.isNotEmpty()) currentIndex = currentIndex.coerceIn(0, queue.lastIndex)
    }

    val current = queue.getOrNull(currentIndex)
    fun advance(confirmed: Boolean, persist: () -> Unit) {
        if (processing || current == null) return
        processing = true
        transitionDirection = if (confirmed) -1 else 1
        persist()
        scope.launch {
            delay(190)
            val removedIndex = queue.indexOfFirst { it.id == current.id }
            if (removedIndex >= 0) queue.removeAt(removedIndex)
            if (queue.isNotEmpty()) currentIndex = currentIndex.coerceAtMost(queue.lastIndex)
            showCategoryPicker = false
            showCreateCategory = false
            showSource = false
            processing = false
        }
    }
    fun browse(direction: Int) {
        if (processing || queue.size < 2) {
            scope.launch { androidx.compose.animation.core.animate(cardDrag, 0f) { value, _ -> cardDrag = value } }
            return
        }
        processing = true
        transitionDirection = if (direction > 0) -1 else 1
        val target = if (direction > 0) -cardWidth else cardWidth
        scope.launch {
            androidx.compose.animation.core.animate(cardDrag, target) { value, _ -> cardDrag = value }
            currentIndex = Math.floorMod(currentIndex + direction, queue.size)
            cardDrag = 0f
            processing = false
        }
    }

    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val source = sources.firstOrNull { it.transactionId == current.id }
    val account = accounts.firstOrNull { it.id == current.accountId }
    val category = categories.firstOrNull { it.id == current.categoryId }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Box(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Lucide.ChevronLeft(size = 21.dp, color = DSBridge.inkSoft())
            }
            Text(
                "${currentIndex + 1} / ${queue.size.coerceAtLeast(1)}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                color = DSBridge.inkMute()
            )
            Spacer(Modifier.width(48.dp))
        }

        Box(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().heightIn(min = 470.dp),
            contentAlignment = Alignment.Center,
        ) {
            repeat(minOf(2, queue.size - 1)) { index ->
                val depth = 2 - index
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (27 + depth * 5).dp)
                        .height(420.dp)
                        .graphicsLayer {
                            translationY = (depth * 10).dp.toPx()
                            rotationZ = if (depth % 2 == 0) -2.2f else 2.2f
                            scaleX = 1f - depth * .025f
                            scaleY = 1f - depth * .025f
                        },
                    shape = RoundedCornerShape(30.dp),
                    color = DSBridge.surface().copy(alpha = if (depth == 1) .92f else .72f),
                    shadowElevation = (3 + depth).dp,
                ) {}
            }
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = 420f, dampingRatio = .82f)) { transitionDirection * -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { transitionDirection * it } + fadeOut())
            },
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { cardWidth = it.width.toFloat().coerceAtLeast(1f) }
                .graphicsLayer {
                    translationX = cardDrag
                    rotationZ = (cardDrag / cardWidth) * 4.5f
                }
                .pointerInput(queue.size, current.id, cardWidth) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            cardDrag = (cardDrag + amount).coerceIn(-cardWidth, cardWidth)
                        },
                        onDragCancel = {
                            scope.launch { androidx.compose.animation.core.animate(cardDrag, 0f) { value, _ -> cardDrag = value } }
                        },
                        onDragEnd = {
                            when {
                                cardDrag <= -cardWidth * .18f -> browse(1)
                                cardDrag >= cardWidth * .18f -> browse(-1)
                                else -> scope.launch { androidx.compose.animation.core.animate(cardDrag, 0f) { value, _ -> cardDrag = value } }
                            }
                        },
                    )
                },
            label = "reviewQueue"
        ) { transaction ->
            val transactionAccount = accounts.firstOrNull { it.id == transaction.accountId }
            val transactionCategory = categories.firstOrNull { it.id == transaction.categoryId }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).heightIn(min = 420.dp),
                shape = RoundedCornerShape(30.dp),
                color = DSBridge.surface(),
                shadowElevation = 10.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val amountSign = when (transaction.type) {
                        "debit" -> "−"
                        else -> ""
                    }
                    val amountColor = when (transaction.type) {
                        "debit" -> DS.Negative
                        "credit" -> DS.Positive
                        "refund" -> DS.Warning
                        else -> DSBridge.ink()
                    }
                    Text(
                        "$amountSign₹${fmt.format(transaction.amountCents / 100.0)}",
                        fontFamily = MonoFamily,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        transaction.merchantRaw,
                        style = DSTypography.headlineMedium,
                        color = DSBridge.ink(),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Text(
                        listOfNotNull(transactionAccount?.name, transactionCategory?.name).joinToString(" · ").ifBlank { "Needs your confirmation" },
                        style = DSTypography.bodySmall,
                        color = DSBridge.inkMute(),
                        textAlign = TextAlign.Center
                    )
                    if (transaction.confidenceScore < .7f) {
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(50.dp), color = DSBridge.warningBg()) {
                            Text(
                                "LOW CONFIDENCE · ${(transaction.confidenceScore * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                fontFamily = MonoFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = DS.Warning
                            )
                        }
                    }

                    if (transaction.source != "manual") {
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showSource = true },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Lucide.Message(size = 17.dp, color = DSBridge.accent())
                            Spacer(Modifier.width(7.dp))
                            Text("View source", fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        ReviewAction(
                            label = "Reject",
                            background = DS.Negative.copy(alpha = .13f),
                            onClick = {
                                haptics.warning()
                                advance(false) { onDismiss(transaction.id) }
                            }
                        ) { Lucide.X(size = 25.dp, color = DS.Negative) }
                        ReviewAction(
                            label = "Confirm",
                            background = DSBridge.accentBg(),
                            onClick = {
                                haptics.confirm()
                                if (transaction.categoryId != null) {
                                    advance(true) { onCategorize(transaction.id, transaction.categoryId) }
                                } else showCategoryPicker = true
                            }
                        ) { Lucide.Check(size = 25.dp, color = DSBridge.accent()) }
                    }
                }
            }
        }
        }

        if (processing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .05f)))
        }
    }

    if (showCategoryPicker) {
        ReviewCategoryPicker(
            transaction = current,
            categories = categories,
            onSelect = { categoryId ->
                haptics.success()
                advance(true) { onCategorize(current.id, categoryId) }
            },
            onCreate = { showCategoryPicker = false; showCreateCategory = true },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showCreateCategory) {
        val categoryType = if (current.type == "credit") CategoryType.INCOME else CategoryType.EXPENSE
        CategoryEditorSheet(
            category = null,
            defaultType = categoryType.name.lowercase(),
            onDismiss = { showCreateCategory = false; showCategoryPicker = true },
            onSave = { name, type, icon, color ->
                haptics.success()
                advance(true) {
                    onCreateCategory(
                        current.id,
                        name,
                        if (type == "income") CategoryType.INCOME else CategoryType.EXPENSE,
                        icon,
                        color
                    )
                }
            }
        )
    }

    if (showSource) {
        SourceMessageSheet(current, source, decryptSource(source), account, category, onDismiss = { showSource = false })
    }
}

@Composable
private fun ReviewAction(
    label: String,
    background: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(58.dp).semantics { contentDescription = "$label transaction" },
            shape = CircleShape,
            color = background
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { icon() }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontFamily = MonoFamily, fontSize = 9.sp, color = DSBridge.inkMute())
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReviewCategoryPicker(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val categoryType = if (transaction.type == "credit") "income" else "expense"
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DSBridge.surface(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Choose category", style = DSTypography.headlineMedium, color = DSBridge.ink())
                    Text(transaction.merchantRaw, style = DSTypography.bodySmall, color = DSBridge.inkMute())
                }
                IconButton(onClick = onDismiss) { Lucide.X(size = 20.dp, color = DSBridge.inkSoft()) }
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(78.dp),
                modifier = Modifier.heightIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.filter { it.type == categoryType }, key = { it.id }) { category ->
                    Column(
                        Modifier.height(74.dp).clip(RoundedCornerShape(17.dp)).background(DSBridge.background())
                            .clickable { onSelect(category.id) }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CategoryGlyph(category.icon, category.name, DSBridge.accent(), Modifier.size(21.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(category.name, style = DSTypography.labelSmall, color = DSBridge.inkSoft(), maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
                item(key = "new_category") {
                    Column(
                        Modifier.height(74.dp).clip(RoundedCornerShape(17.dp)).background(DSBridge.background())
                            .semantics { contentDescription = "Add category" }
                            .clickable(onClick = onCreate).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Lucide.Tag(size = 21.dp, color = DSBridge.accent())
                        Spacer(Modifier.height(6.dp))
                        Text("New category", style = DSTypography.labelSmall, color = DSBridge.inkSoft(), maxLines = 1)
                    }
                }
            }
        }
    }
}
