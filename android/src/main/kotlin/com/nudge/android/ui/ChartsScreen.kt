package com.nudge.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.components.DonutChart
import com.nudge.android.ui.components.DonutSegment
import com.nudge.android.ui.components.NudgeHeroCard
import com.nudge.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun ChartsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onBack: (() -> Unit)? = null,
) {
    val now = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }

    val monthLabel = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
            .let { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(it.time) }
    }
    val monthTransactions = remember(transactions, selectedYear, selectedMonth) {
        transactions.filter { it.isInMonth(selectedYear, selectedMonth) }
    }
    val previous = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1); add(Calendar.MONTH, -1) }
    }
    val previousSpend = remember(transactions, previous) {
        transactions.filter { it.type == "debit" && it.isInMonth(previous.get(Calendar.YEAR), previous.get(Calendar.MONTH)) }
            .sumOf { it.amountCents }
    }
    val spent = monthTransactions.filter { it.type == "debit" }.sumOf { it.amountCents }
    val income = monthTransactions.filter { it.type == "credit" }.sumOf { it.amountCents }
    val spendDelta = if (previousSpend > 0) ((spent - previousSpend) * 100f / previousSpend).toInt() else 0
    val dailyExpense = remember(monthTransactions, selectedYear, selectedMonth) {
        computeDailyFlow(monthTransactions, selectedYear, selectedMonth, "debit")
    }
    val dailyIncome = remember(monthTransactions, selectedYear, selectedMonth) {
        computeDailyFlow(monthTransactions, selectedYear, selectedMonth, "credit")
    }
    val categorySpending = remember(monthTransactions, selectedYear, selectedMonth, categories) {
        computeCategorySpending(monthTransactions, categories, selectedYear, selectedMonth)
    }
    val expenseTransactions = monthTransactions.filter { it.type == "debit" }
    val largestExpense = expenseTransactions.maxByOrNull { it.amountCents }
    val daysForAverage = if (selectedYear == now.get(Calendar.YEAR) && selectedMonth == now.get(Calendar.MONTH)) {
        now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    } else {
        Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(DSBridge.accentBg().copy(alpha = .72f), DSBridge.background(), DSBridge.background()),
            ),
        ),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AnalyticsHeader(
                monthLabel = monthLabel,
                onBack = onBack,
                onPrevious = {
                    if (selectedMonth == 0) { selectedMonth = 11; selectedYear-- } else selectedMonth--
                },
                onNext = {
                    val next = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1); add(Calendar.MONTH, 1) }
                    if (next.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                        (next.get(Calendar.YEAR) == now.get(Calendar.YEAR) && next.get(Calendar.MONTH) <= now.get(Calendar.MONTH))
                    ) {
                        selectedYear = next.get(Calendar.YEAR)
                        selectedMonth = next.get(Calendar.MONTH)
                    }
                },
            )

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                CategoryAnalyticsCard(categorySpending, spent)
                DailyCashFlowCard(dailyExpense, dailyIncome, spent, income, spendDelta)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InsightCard("AVG / DAY", formatCents(spent / daysForAverage), "daily pace", DS.Warning, Modifier.weight(1f))
                    InsightCard("LARGEST", formatCents(largestExpense?.amountCents ?: 0), largestExpense?.merchantRaw ?: "no expense", DS.Negative, Modifier.weight(1f))
                    InsightCard("ENTRIES", monthTransactions.size.toString(), "this month", DS.Positive, Modifier.weight(1f))
                }
                Spacer(Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun AnalyticsHeader(
    monthLabel: String,
    onBack: (() -> Unit)?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) {
                    Lucide.ChevronLeft(size = 21.dp, color = DSBridge.inkSoft())
                }
            }
            Text("Analytics", style = DSTypography.headlineLarge, color = DSBridge.ink(), modifier = Modifier.align(Alignment.Center))
        }
        Surface(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(15.dp),
            color = DSBridge.surface(),
            border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .13f)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) { Lucide.ChevronLeft(size = 17.dp, color = DSBridge.inkSoft()) }
                Text(monthLabel, style = DSTypography.labelMedium, color = DSBridge.ink(), maxLines = 1)
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) { Lucide.ChevronRight(size = 17.dp, color = DSBridge.inkSoft()) }
            }
        }
    }
}

@Composable
private fun CashFlowOverview(
    monthLabel: String,
    netFlow: Long,
    income: Long,
    spent: Long,
    refunds: Long,
    spendDelta: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = DSBridge.surface(),
        border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .11f)),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NET CASH FLOW", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = .9.sp, color = DSBridge.inkMute())
                    Text(monthLabel, style = DSTypography.bodySmall, color = DSBridge.inkSoft())
                }
                Surface(shape = RoundedCornerShape(10.dp), color = if (spendDelta <= 0) DSBridge.accentBg() else DS.Negative.copy(alpha = .10f)) {
                    Text(
                        if (spendDelta == 0) "STEADY" else "${spendDelta.absoluteValue}% ${if (spendDelta > 0) "MORE" else "LESS"} SPENT",
                        fontFamily = MonoFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (spendDelta <= 0) DSBridge.accent() else DS.Negative,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(netFlow, label = "netFlow") { value ->
                Text(
                    "${if (value > 0) "+" else if (value < 0) "−" else ""}${formatCents(value.absoluteValue)}",
                    fontFamily = MonoFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (value >= 0) DSBridge.ink() else DS.Negative,
                )
            }
            Text(if (netFlow >= 0) "money retained after expenses" else "expenses exceeded money in", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            Spacer(Modifier.height(17.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowMetric("MONEY IN", income, DS.Positive, Modifier.weight(1f))
                FlowMetric("SPENT", spent, DS.Negative, Modifier.weight(1f))
                FlowMetric("REFUNDS", refunds, DS.Warning, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FlowMetric(label: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = .08f)).padding(horizontal = 10.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(5.dp))
            Text(label, fontFamily = MonoFamily, fontSize = 7.sp, color = DSBridge.inkMute(), maxLines = 1)
        }
        Spacer(Modifier.height(5.dp))
        Text(formatCents(amount), fontFamily = MonoFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DSBridge.ink(), maxLines = 1)
    }
}

@Composable
private fun DailyCashFlowCard(
    expense: Map<Int, Long>,
    income: Map<Int, Long>,
    totalSpend: Long,
    totalIncome: Long,
    spendDelta: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        color = DSBridge.surface(),
        border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .11f)),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Spending rhythm", style = DSTypography.titleMedium, color = DSBridge.ink())
                    Text(formatCents(totalSpend), fontFamily = MonoFamily, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = DSBridge.ink())
                    Text(
                        if (spendDelta == 0) "No previous-month change" else "${spendDelta.absoluteValue}% ${if (spendDelta > 0) "above" else "below"} last month",
                        style = DSTypography.bodySmall,
                        color = if (spendDelta > 0) DS.Negative else DS.Positive,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MONEY IN", fontFamily = MonoFamily, fontSize = 7.sp, color = DSBridge.inkMute())
                    Text(formatCents(totalIncome), fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DS.Positive)
                    Spacer(Modifier.height(7.dp))
                    Row {
                        ChartLegend("In", DS.Positive)
                        Spacer(Modifier.width(9.dp))
                        ChartLegend("Out", DS.Negative)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            CashFlowLineChart(expense, income, Modifier.fillMaxWidth().height(188.dp))
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = MonoFamily, fontSize = 8.sp, color = DSBridge.inkMute())
    }
}

@Composable
private fun InsightCard(label: String, value: String, subtext: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = DSBridge.surface(),
        border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .10f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.height(9.dp))
            Text(label, fontFamily = MonoFamily, fontSize = 7.sp, color = DSBridge.inkMute(), maxLines = 1)
            Text(value, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DSBridge.ink(), maxLines = 1)
            Text(subtext, fontSize = 8.sp, color = DSBridge.inkMute(), maxLines = 1)
        }
    }
}

@Composable
private fun CategoryAnalyticsCard(categorySpending: List<Triple<String, Color, Long>>, totalSpend: Long) {
    NudgeHeroCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Surface(shape = RoundedCornerShape(50), color = DS.Signal) {
                Text("EXPENSE MIX", fontFamily = MonoFamily, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DS.InkPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Where your money went", style = DSTypography.titleMedium, color = DSBridge.ink())
            Text("Share of this month's spending", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            Spacer(Modifier.height(15.dp))
            if (categorySpending.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No expense data for this month", style = DSTypography.bodySmall, color = DSBridge.inkMute())
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DonutChart(
                        segments = categorySpending.take(6).map { (name, color, amount) ->
                            DonutSegment(name, amount.toFloat() / totalSpend.coerceAtLeast(1).toFloat(), color)
                        },
                        centerLabel = formatCompactCentsPlain(totalSpend),
                        centerSubtext = null,
                        size = 142.dp,
                        showLegend = false,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        categorySpending.take(5).forEach { (name, color, amount) ->
                            val share = if (totalSpend > 0) amount * 100f / totalSpend else 0f
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(6.dp))
                                    Text(name, style = DSTypography.labelSmall, color = DSBridge.ink(), modifier = Modifier.weight(1f), maxLines = 1)
                                    Text("${share.toInt()}%", fontFamily = MonoFamily, fontSize = 9.sp, color = DSBridge.inkSoft())
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { share / 100f },
                                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                                    color = color,
                                    trackColor = color.copy(alpha = .12f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun CashFlowLineChart(
    expense: Map<Int, Long>,
    income: Map<Int, Long>,
    modifier: Modifier = Modifier,
) {
    val hasData = expense.values.any { it > 0 } || income.values.any { it > 0 }
    if (!hasData) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Your trend will appear after the first transaction", style = DSTypography.bodySmall, color = DSBridge.inkMute())
        }
        return
    }

    val days = (expense.keys + income.keys).distinct().sorted()
    val maxAmount = ((expense.values + income.values).maxOrNull() ?: 1L).toFloat().coerceAtLeast(1f)
    val textMeasurer = rememberTextMeasurer()
    val gridColor = DSBridge.inkMute()

    Canvas(modifier) {
        val left = 8.dp.toPx()
        val right = 6.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 24.dp.toPx()
        val width = size.width - left - right
        val height = size.height - top - bottom
        val lastDay = (days.lastOrNull() ?: 1).coerceAtLeast(2)

        repeat(4) { index ->
            val y = top + height * index / 3f
            drawLine(gridColor.copy(alpha = .11f), Offset(left, y), Offset(left + width, y), 1f)
        }

        fun points(values: Map<Int, Long>): List<Offset> = days.map { day ->
            Offset(
                x = left + ((day - 1f) / (lastDay - 1f)) * width,
                y = top + height - ((values[day] ?: 0L) / maxAmount) * height,
            )
        }

        fun smoothPath(points: List<Offset>): Path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (index in 0 until points.lastIndex) {
                    val current = points[index]
                    val next = points[index + 1]
                    val midX = (current.x + next.x) / 2f
                    cubicTo(midX, current.y, midX, next.y, next.x, next.y)
                }
            }
        }

        val expensePoints = points(expense)
        val incomePoints = points(income)
        drawPath(smoothPath(incomePoints), DS.Positive, style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(smoothPath(expensePoints), DS.Negative, style = Stroke(2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        days.forEachIndexed { index, day ->
            if ((income[day] ?: 0) > 0) drawCircle(DS.Positive, 2.5.dp.toPx(), incomePoints[index])
            if ((expense[day] ?: 0) > 0) drawCircle(DS.Negative, 2.5.dp.toPx(), expensePoints[index])
        }

        val labelStyle = TextStyle(fontSize = 8.sp, color = gridColor)
        listOf(1, 8, 15, 22, lastDay).distinct().filter { it <= lastDay }.forEach { day ->
            val x = left + ((day - 1f) / (lastDay - 1f)) * width
            val layout = textMeasurer.measure(AnnotatedString(day.toString()), labelStyle)
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, top + height + 8.dp.toPx()))
        }
    }
}

private fun TransactionEntity.isInMonth(year: Int, month: Int): Boolean = Calendar.getInstance().apply {
    timeInMillis = timestampEpoch
}.let { it.get(Calendar.YEAR) == year && it.get(Calendar.MONTH) == month }

private fun computeDailyFlow(
    transactions: List<TransactionEntity>,
    year: Int,
    month: Int,
    type: String,
): Map<Int, Long> {
    val calendar = Calendar.getInstance().apply { set(year, month, 1) }
    val daily = (1..calendar.getActualMaximum(Calendar.DAY_OF_MONTH)).associateWith { 0L }.toMutableMap()
    transactions.filter { it.type == type }.forEach { transaction ->
        val day = Calendar.getInstance().apply { timeInMillis = transaction.timestampEpoch }.get(Calendar.DAY_OF_MONTH)
        daily[day] = (daily[day] ?: 0L) + transaction.amountCents
    }
    return daily
}

private fun computeCategorySpending(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    year: Int,
    month: Int,
): List<Triple<String, Color, Long>> {
    val totals = transactions.filter { it.type == "debit" && it.isInMonth(year, month) }
        .groupBy { it.categoryId ?: "__uncategorized__" }
        .mapValues { (_, entries) -> entries.sumOf { it.amountCents } }

    return totals.map { (categoryId, amount) ->
        val category = categories.find { it.id == categoryId }
        Triple(category?.name ?: "Uncategorized", parseCategoryColor(category?.color, categories.indexOfFirst { it.id == categoryId }), amount)
    }.sortedByDescending { it.third }
}

private fun parseCategoryColor(colorString: String?, index: Int): Color {
    if (colorString != null) {
        runCatching {
            val value = colorString.removePrefix("#").toLong(16)
            return Color(
                red = ((value shr 16) and 0xFF) / 255f,
                green = ((value shr 8) and 0xFF) / 255f,
                blue = (value and 0xFF) / 255f,
                alpha = 1f,
            )
        }
    }
    return Nc.catColors[index.coerceAtLeast(0) % Nc.catColors.size]
}
