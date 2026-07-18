package com.nudge.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.NudgeColors
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private object NudgeRadius {
    const val SM = 8
    const val MD = 14
    const val LG = 20
    const val XL = 28
}

@Composable
fun ChartsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    val now = remember { Calendar.getInstance() }

    var selectedYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(now.get(Calendar.MONTH)) }

    val monthLabel = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        val fmt = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        fmt.format(cal.time)
    }

    val dailySpending = remember(transactions, selectedYear, selectedMonth) {
        computeDailySpending(transactions, selectedYear, selectedMonth)
    }

    val categorySpending = remember(transactions, selectedYear, selectedMonth) {
        computeCategorySpending(transactions, categories, selectedYear, selectedMonth)
    }

    val totalMonthSpend = remember(dailySpending) {
        dailySpending.values.sum()
    }

    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val totalFormatted = remember(totalMonthSpend) { formatter.format(totalMonthSpend / 100.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Charts",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190", fontSize = 18.sp, color = NudgeColors.ContentSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) NudgeColors.DarkSurfaceBase else NudgeColors.SurfaceBase
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    if (isDark) NudgeColors.DarkSurfaceBase
                    else NudgeColors.SurfaceBase
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total spend card
            Card(
                shape = RoundedCornerShape(NudgeRadius.XL),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) NudgeColors.DarkSurfaceRaised else NudgeColors.SurfaceRaised
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Spend",
                        fontSize = 13.sp,
                        color = NudgeColors.ContentSecondary
                    )
                    Text(
                        "\u20B9$totalFormatted",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        monthLabel,
                        fontSize = 13.sp,
                        color = NudgeColors.ContentTertiary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (selectedMonth == 0) {
                                selectedMonth = 11
                                selectedYear--
                            } else {
                                selectedMonth--
                            }
                        }) {
                            Text(
                                "\u2039",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = NudgeColors.AccentPrimary
                            )
                        }

                        Text(
                            monthLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                        )

                        IconButton(onClick = {
                            var newMonth = selectedMonth + 1
                            var newYear = selectedYear
                            if (newMonth > 11) {
                                newMonth = 0
                                newYear++
                            }
                            if (newYear < now.get(Calendar.YEAR) ||
                                (newYear == now.get(Calendar.YEAR) && newMonth <= now.get(Calendar.MONTH))
                            ) {
                                selectedMonth = newMonth
                                selectedYear = newYear
                            }
                        }) {
                            Text(
                                "\u203A",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = NudgeColors.AccentPrimary
                            )
                        }
                    }
                }
            }

            // Daily spending line chart
            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) NudgeColors.DarkSurfaceRaised else NudgeColors.SurfaceRaised
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Daily Spending",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DailySpendChart(
                        dailySpending = dailySpending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // Category breakdown
            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) NudgeColors.DarkSurfaceRaised else NudgeColors.SurfaceRaised
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "By Category",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) NudgeColors.DarkContentPrimary else NudgeColors.ContentPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categorySpending.isEmpty()) {
                        Text(
                            "No spending data for this month",
                            fontSize = 13.sp,
                            color = NudgeColors.ContentTertiary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        CategoryBarChart(
                            categorySpending = categorySpending,
                            totalSpend = totalMonthSpend,
                            formatter = formatter,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DailySpendChart(
    dailySpending: Map<Int, Long>,
    modifier: Modifier = Modifier
) {
    if (dailySpending.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data",
                fontSize = 13.sp,
                color = NudgeColors.ContentTertiary
            )
        }
        return
    }

    val maxAmount = remember(dailySpending) { dailySpending.values.maxOrNull()?.toFloat() ?: 1f }
    val days = remember(dailySpending) { dailySpending.keys.sorted() }

    Canvas(modifier = modifier) {
        if (days.isEmpty()) return@Canvas

        val paddingLeft = 40f
        val paddingBottom = 30f
        val paddingTop = 16f
        val paddingRight = 16f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val dayRange = (days.last() - days.first()).coerceAtLeast(1)

        val points = days.map { day ->
            val x = paddingLeft + ((day - days.first()).toFloat() / dayRange) * chartWidth
            val y = paddingTop + chartHeight - ((dailySpending[day]?.toFloat() ?: 0f) / maxAmount * chartHeight)
            Offset(x, y)
        }

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (chartHeight * i / gridLines)
            drawLine(
                color = NudgeColors.ContentTertiary.copy(alpha = 0.15f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
        }

        // Draw fill area
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                color = NudgeColors.AccentPrimary.copy(alpha = 0.1f)
            )
        }

        // Draw smooth line
        if (points.size >= 2) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val midX = (p1.x + p2.x) / 2f
                    val midY = (p1.y + p2.y) / 2f
                    quadraticTo(p1.x, p1.y, midX, midY)
                }
                lineTo(points.last().x, points.last().y)
            }

            drawPath(
                path = linePath,
                color = NudgeColors.AccentPrimary,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw dots
        points.forEach { point ->
            drawCircle(
                color = NudgeColors.AccentPrimary,
                radius = 3.dp.toPx(),
                center = point
            )
        }

        // X axis labels
        val labelStep = (days.size / 5).coerceAtLeast(1)
        days.filterIndexed { index, _ -> index % labelStep == 0 || index == days.size - 1 }.forEach { day ->
            val x = paddingLeft + ((day - days.first()).toFloat() / dayRange) * chartWidth
            // Y axis labels (max only)
        }
    }
}

@Composable
private fun CategoryBarChart(
    categorySpending: List<Triple<String, Color, Long>>,
    totalSpend: Long,
    formatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val maxSpend = remember(categorySpending) {
            categorySpending.maxOfOrNull { it.third }?.toFloat() ?: 1f
        }

        categorySpending.take(8).forEach { (name, color, amount) ->
            val fraction = if (maxSpend > 0) amount.toFloat() / maxSpend else 0f
            val formattedAmount = remember(amount) { formatter.format(amount / 100.0) }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = NudgeColors.ContentPrimary
                    )
                    Text(
                        "\u20B9$formattedAmount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = NudgeColors.ContentSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(NudgeRadius.SM))
                        .background(NudgeColors.ContentTertiary.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(NudgeRadius.SM))
                            .background(color)
                    )
                }
            }
        }

        if (categorySpending.size > 8) {
            Text(
                "+ ${categorySpending.size - 8} more categories",
                fontSize = 12.sp,
                color = NudgeColors.ContentTertiary
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
    return NudgeColors.CategoryColors[index % NudgeColors.CategoryColors.size]
}
