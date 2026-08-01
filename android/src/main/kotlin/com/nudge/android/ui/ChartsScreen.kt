package com.nudge.android.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.components.DonutChart
import com.nudge.android.ui.components.DonutSegment
import com.nudge.android.ui.theme.*
import java.util.Calendar
import java.util.Locale

@Composable
fun ChartsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onBack: (() -> Unit)? = null
) {
    val now = remember { Calendar.getInstance() }

    var selectedYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }

    val monthLabel = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        val fmt = java.text.SimpleDateFormat("MMM yyyy", Locale.getDefault())
        fmt.format(cal.time)
    }

    val dailySpending = remember(transactions, selectedYear, selectedMonth) {
        computeDailySpending(transactions, selectedYear, selectedMonth)
    }

    val categorySpending = remember(transactions, selectedYear, selectedMonth) {
        computeCategorySpending(transactions, categories, selectedYear, selectedMonth)
    }

    val totalMonthSpend = remember(dailySpending) { dailySpending.values.sum() }
    val monthIncome = remember(transactions, selectedYear, selectedMonth) {
        transactions.filter { txn ->
            if (txn.type != "credit") return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = txn.timestampEpoch }
            cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth
        }.sumOf { it.amountCents }
    }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        // ── Header ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DSSpace.lg, vertical = DSSpace.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) IconButton(onClick = onBack) { Lucide.ArrowLeft(size = 20.dp, strokeWidth = 2.dp, color = DSBridge.inkSoft()) }
            Text("Charts", style = DSTypography.headlineLarge, color = DSBridge.ink(), modifier = Modifier.weight(1f))
            // Month navigator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DSBridge.surface())
            ) {
                IconButton(
                    onClick = {
                        if (selectedMonth == 0) { selectedMonth = 11; selectedYear-- } else selectedMonth--
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("‹", fontSize = 22.sp, color = DSBridge.accent())
                }
                Text(monthLabel, style = DSTypography.titleSmall, color = DSBridge.ink())
                IconButton(
                    onClick = {
                        var newMonth = selectedMonth + 1
                        var newYear = selectedYear
                        if (newMonth > 11) { newMonth = 0; newYear++ }
                        if (newYear < now.get(Calendar.YEAR) ||
                            (newYear == now.get(Calendar.YEAR) && newMonth <= now.get(Calendar.MONTH))
                        ) {
                            selectedMonth = newMonth
                            selectedYear = newYear
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("›", fontSize = 22.sp, color = DSBridge.accent())
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DSSpace.lg),
            verticalArrangement = Arrangement.spacedBy(DSSpace.md)
        ) {
            // ── Total spend card ──
            DSHeroCard {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Total Spend", style = DSTypography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(DSSpace.xs))
                    Text(
                        formatCents(totalMonthSpend),
                        style = DSTypography.displayLarge, color = Color.White
                    )
                    Spacer(Modifier.height(DSSpace.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(DSSpace.sm)) {
                        HeroPill("Spent", formatCents(totalMonthSpend))
                        HeroPill("Income", formatCents(monthIncome))
                    }
                }
            }

            // ── Daily spending ──
            DSCard {
                Column(Modifier.fillMaxWidth().padding(DSSpace.base)) {
                    Text("Daily Spending", style = DSTypography.titleMedium, color = DSBridge.ink())
                    Spacer(Modifier.height(DSSpace.md))
                    DailySpendChart(
                        dailySpending = dailySpending,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
            }

            // ── By category donut ──
            DSCard {
                Column(Modifier.fillMaxWidth().padding(DSSpace.base)) {
                    Text("By Category", style = DSTypography.titleMedium, color = DSBridge.ink())
                    Spacer(Modifier.height(DSSpace.md))

                    if (categorySpending.isEmpty()) {
                        Text(
                            "No spending data for this month",
                            style = DSTypography.bodySmall, color = DSBridge.inkMute(),
                            modifier = Modifier.padding(vertical = DSSpace.base)
                        )
                    } else {
                        DonutChart(
                            segments = categorySpending.take(8).map { (_, color, amount) ->
                                DonutSegment("", amount.toFloat() / totalMonthSpend.coerceAtLeast(1).toFloat(), color)
                            },
                            centerLabel = formatCentsPlain(totalMonthSpend),
                            centerSubtext = "spent",
                            size = 150.dp,
                            showLegend = false,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(DSSpace.md))
                        categorySpending.take(8).forEach { (name, color, amount) ->
                            val fraction = if (totalMonthSpend > 0) amount * 100f / totalMonthSpend else 0f
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(DSSpace.sm))
                                Text(name, style = DSTypography.titleSmall, color = DSBridge.ink(), modifier = Modifier.weight(1f), maxLines = 1)
                                Text(
                                    "${formatCentsPlain(amount)} · ${"%.0f".format(fraction)}%",
                                    style = DSTypography.labelMedium, fontFamily = MonoFamily, color = DSBridge.inkSoft()
                                )
                            }
                        }
                        if (categorySpending.size > 8) {
                            Text(
                                "+ ${categorySpending.size - 8} more categories",
                                style = DSTypography.labelSmall, color = DSBridge.inkMute(),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(110.dp))
        }
    }
}

@Composable
private fun HeroPill(label: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.15f)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(label, style = DSTypography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(value, style = DSTypography.titleMedium, fontFamily = MonoFamily, color = Color.White)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun DailySpendChart(
    dailySpending: Map<Int, Long>,
    modifier: Modifier = Modifier
) {
    if (dailySpending.values.all { it == 0L }) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No data", style = DSTypography.bodySmall, color = DSBridge.inkMute())
        }
        return
    }

    val maxAmount = remember(dailySpending) { dailySpending.values.maxOrNull()?.toFloat() ?: 1f }
    val days = remember(dailySpending) { dailySpending.keys.sorted() }
    val textMeasurer = rememberTextMeasurer()

    val gridLineColor = DSBridge.inkMute()
    val lineColor = DSBridge.accent()

    Canvas(modifier = modifier) {
        if (days.isEmpty()) return@Canvas

        val paddingLeft = 44f
        val paddingBottom = 24f
        val paddingTop = 16f
        val paddingRight = 8f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val dayRange = (days.last() - days.first()).coerceAtLeast(1)

        val points = days.map { day ->
            val x = paddingLeft + ((day - days.first()).toFloat() / dayRange) * chartWidth
            val y = paddingTop + chartHeight - ((dailySpending[day]?.toFloat() ?: 0f) / maxAmount * chartHeight)
            Offset(x, y)
        }

        // Grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (chartHeight * i / gridLines)
            drawLine(
                color = gridLineColor.copy(alpha = 0.12f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
        }

        // Y-axis max label
        val yLabelStyle = TextStyle(fontSize = 9.sp, color = gridLineColor.copy(alpha = 0.7f))
        drawText(
            textMeasurer,
            AnnotatedString("₹${maxAmount.toInt()}"),
            topLeft = Offset(0f, paddingTop - 4f),
            style = yLabelStyle
        )

        // Fill area
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                points.forEach { point -> lineTo(point.x, point.y) }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }
            drawPath(fillPath, lineColor.copy(alpha = 0.1f))
        }

        // Smooth line
        if (points.size >= 2) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val midX = (p1.x + p2.x) / 2f
                    val midY = (p1.y + p2.y) / 2f
                    quadraticBezierTo(p1.x, p1.y, midX, midY)
                }
                lineTo(points.last().x, points.last().y)
            }
            drawPath(
                linePath, lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Dots
        points.forEach { point ->
            drawCircle(lineColor, radius = 3.dp.toPx(), center = point)
        }

        // X-axis day labels (5 across)
        val labelStep = (days.size / 5).coerceAtLeast(1)
        val labeledDays = days.filterIndexed { index, _ -> index % labelStep == 0 }
            .plus(days.last())
            .distinct()
        labeledDays.forEach { day ->
            val x = paddingLeft + ((day - days.first()).toFloat() / dayRange) * chartWidth
            val layout = textMeasurer.measure(
                AnnotatedString("$day"),
                style = yLabelStyle
            )
            drawText(
                layout,
                topLeft = Offset((x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width), paddingTop + chartHeight + 8f)
            )
        }
    }
}

private fun computeDailySpending(
    transactions: List<TransactionEntity>,
    year: Int,
    month: Int
): Map<Int, Long> {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dailyTotals = mutableMapOf<Int, Long>()
    for (day in 1..daysInMonth) {
        dailyTotals[day] = 0L
    }

    val cal = Calendar.getInstance()
    transactions.forEach { txn ->
        if (txn.type == "debit") {
            cal.timeInMillis = txn.timestampEpoch
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                dailyTotals[day] = (dailyTotals[day] ?: 0) + txn.amountCents
            }
        }
    }

    return dailyTotals
}

private fun computeCategorySpending(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    year: Int,
    month: Int
): List<Triple<String, Color, Long>> {
    val cal = Calendar.getInstance()
    val map = mutableMapOf<String, Long>()

    transactions.forEach { txn ->
        if (txn.type == "debit") {
            cal.timeInMillis = txn.timestampEpoch
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                val key = txn.categoryId ?: "__uncategorized__"
                map[key] = (map[key] ?: 0) + txn.amountCents
            }
        }
    }

    return map.entries.map { (catId, total) ->
        val cat = categories.find { it.id == catId }
        val color = parseCategoryColor(cat?.color, categories.indexOfFirst { it.id == catId })
        val name = cat?.name ?: "Uncategorized"
        Triple(name, color, total)
    }.sortedByDescending { it.third }
}

private fun parseCategoryColor(colorStr: String?, index: Int): Color {
    if (colorStr != null) {
        try {
            val hex = colorStr.removePrefix("#")
            val colorLong = hex.toLong(16)
            return Color(
                red = ((colorLong shr 16) and 0xFF) / 255f,
                green = ((colorLong shr 8) and 0xFF) / 255f,
                blue = (colorLong and 0xFF) / 255f,
                alpha = 1f
            )
        } catch (_: Exception) { }
    }
    return Nc.catColors[index % Nc.catColors.size]
}
