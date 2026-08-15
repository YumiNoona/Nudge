package com.nudge.android.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nudge.android.data.NudgeDatabase
import com.nudge.android.ui.MainActivity
import com.nudge.android.ui.theme.formatCompactCentsPlain
import java.util.Calendar
import kotlin.math.max

private data class WidgetCategory(
    val name: String,
    val amountCents: Long,
    val percent: Int,
    val color: Int
)

private data class WidgetSnapshot(
    val spentCents: Long,
    val previousSpentCents: Long,
    val categories: List<WidgetCategory>,
    val hideAmounts: Boolean
)

class NudgeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 76.dp),  // 2 x 1 quick glance
            DpSize(250.dp, 140.dp), // 3 x 2 snapshot
            DpSize(180.dp, 280.dp), // portrait breakdown
            DpSize(320.dp, 160.dp)  // landscape dashboard
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = NudgeDatabase.getInstance(context)
        val transactions = database.transactionDao().getAllOnce()
        val categories = database.categoryDao().getAllOnce().associateBy { it.id }
        val preferences = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
        val fourteenDaysAgo = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L
        val recentExpenses = transactions.filter { it.type == "debit" && it.timestampEpoch >= sevenDaysAgo }
        val previousExpenses = transactions.filter {
            it.type == "debit" && it.timestampEpoch >= fourteenDaysAgo && it.timestampEpoch < sevenDaysAgo
        }
        val total = recentExpenses.sumOf { it.amountCents }
        val previousTotal = previousExpenses.sumOf { it.amountCents }
        val palette = listOf(0xFF42BCA3.toInt(), 0xFFDEFF67.toInt(), 0xFFFFCC7A.toInt(), 0xFF5B8DEF.toInt())
        val grouped = recentExpenses.groupBy { it.categoryId }.map { (categoryId, entries) ->
            (categories[categoryId]?.name ?: "Other") to entries.sumOf { it.amountCents }
        }.sortedByDescending { it.second }
        val displayGroups = if (grouped.size <= 4) grouped else grouped.take(3) + ("Other" to grouped.drop(3).sumOf { it.second })
        val top = displayGroups.mapIndexed { index, (name, amount) ->
            WidgetCategory(
                name = name,
                amountCents = amount,
                percent = if (total == 0L) 0 else ((amount * 100f) / total).toInt().coerceIn(1, 100),
                color = palette[index % palette.size]
            )
        }
        val snapshot = WidgetSnapshot(
            spentCents = total,
            previousSpentCents = previousTotal,
            categories = top,
            hideAmounts = preferences.getBoolean("hide_widget_amounts", false)
        )
        val donut = createDonutBitmap(top)
        val openTransactions = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TRANSACTIONS, true)

        provideContent {
            val size = LocalSize.current
            when {
                size.height >= 200.dp -> PortraitWidget(snapshot, donut, openTransactions)
                size.width >= 290.dp -> LandscapeWidget(snapshot, donut, openTransactions)
                size.width >= 210.dp && size.height >= 100.dp -> SnapshotWidget(snapshot, donut, openTransactions)
                else -> CompactWidget(snapshot, openTransactions)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SnapshotWidget(
    snapshot: WidgetSnapshot,
    donut: Bitmap,
    openTransactions: Intent,
) {
    WidgetCard(openTransactions, padding = 12) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.width(72.dp).fillMaxHeight()) {
                StatusPill("EXPENSES")
                Spacer(GlanceModifier.height(5.dp))
                Text("Past 7 days", style = TextStyle(color = WidgetColors.primary, fontSize = 9.sp, fontWeight = FontWeight.Medium))
                Text(
                    widgetAmount(snapshot),
                    style = TextStyle(color = WidgetColors.primary, fontSize = 23.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                TrendText(snapshot, compact = true)
            }
            Spacer(GlanceModifier.width(5.dp))
            DonutAmount(
                donut = donut,
                amount = widgetAmount(snapshot),
                modifier = GlanceModifier.size(68.dp),
                amountSize = 13,
                subtitle = "TOTAL",
            )
            Spacer(GlanceModifier.width(5.dp))
            Column(GlanceModifier.defaultWeight()) {
                if (snapshot.categories.isEmpty()) {
                    Text("No expenses yet", style = TextStyle(color = WidgetColors.secondary, fontSize = 8.sp))
                } else {
                    snapshot.categories.take(3).forEach { LegendRow(it, showAmount = false, compact = true) }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PortraitWidget(snapshot: WidgetSnapshot, donut: Bitmap, openTransactions: Intent) {
    WidgetCard(openTransactions, padding = 14) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            StatusPill("EXPENSES")
        }
        Spacer(GlanceModifier.height(5.dp))
        Text("In the past 7 days", style = TextStyle(color = WidgetColors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(6.dp))
        DonutAmount(
            donut = donut,
            amount = widgetAmount(snapshot),
            modifier = GlanceModifier.fillMaxWidth().height(120.dp),
            amountSize = 27
        )
        Spacer(GlanceModifier.height(6.dp))
        if (snapshot.categories.isEmpty()) {
            EmptyBreakdown()
        } else {
            snapshot.categories.forEach { LegendRow(it, showAmount = false) }
        }
    }
}

@androidx.compose.runtime.Composable
private fun LandscapeWidget(
    snapshot: WidgetSnapshot,
    donut: Bitmap,
    openTransactions: Intent,
) {
    WidgetCard(openTransactions, padding = 14) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.width(132.dp).fillMaxHeight()) {
                StatusPill("7 DAY SPEND")
                Spacer(GlanceModifier.height(7.dp))
                DonutAmount(
                    donut = donut,
                    amount = widgetAmount(snapshot),
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    amountSize = 21
                )
            }
            Spacer(GlanceModifier.width(15.dp))
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                Text("TOP CATEGORIES", style = TextStyle(color = WidgetColors.secondary, fontSize = 8.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(7.dp))
                if (snapshot.categories.isEmpty()) {
                    EmptyBreakdown()
                } else {
                    snapshot.categories.take(3).forEach { LegendRow(it, showAmount = !snapshot.hideAmounts) }
                }
                Spacer(GlanceModifier.defaultWeight())
                TrendText(snapshot, compact = false)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CompactWidget(snapshot: WidgetSnapshot, openTransactions: Intent) {
    WidgetCard(openTransactions, padding = 9) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.width(82.dp).fillMaxHeight()) {
                StatusPill("EXPENSES")
                Spacer(GlanceModifier.height(2.dp))
                Text("Past 7 days", style = TextStyle(color = WidgetColors.secondary, fontSize = 7.sp))
                Text(
                    widgetAmount(snapshot),
                    style = TextStyle(color = WidgetColors.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(GlanceModifier.defaultWeight())
                TrendText(snapshot, compact = true)
            }
            Spacer(GlanceModifier.width(8.dp))
            Box(GlanceModifier.width(1.dp).fillMaxHeight().background(WidgetColors.divider)) {}
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                if (snapshot.categories.isEmpty()) {
                    Text("No expenses yet", style = TextStyle(color = WidgetColors.secondary, fontSize = 8.sp))
                } else {
                    snapshot.categories.take(3).forEach { LegendRow(it, showAmount = false, compact = true) }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetCard(openApp: Intent, padding: Int, content: @androidx.compose.runtime.Composable ColumnScope.() -> Unit) {
    Column(
        GlanceModifier.fillMaxSize().background(WidgetColors.background).cornerRadius(28.dp)
            .padding(padding.dp).clickable(actionStartActivity(openApp)),
        content = content
    )
}

@androidx.compose.runtime.Composable
private fun StatusPill(label: String) {
    Box(
        GlanceModifier.background(WidgetColors.signal).cornerRadius(50.dp).padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = TextStyle(color = WidgetColors.onSignal, fontSize = 8.sp, fontWeight = FontWeight.Bold))
    }
}

@androidx.compose.runtime.Composable
private fun DonutAmount(
    donut: Bitmap,
    amount: String,
    modifier: GlanceModifier,
    amountSize: Int,
    subtitle: String = "SPENT",
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(ImageProvider(donut), contentDescription = "Spending categories", modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(amount, style = TextStyle(color = WidgetColors.primary, fontSize = amountSize.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(subtitle, style = TextStyle(color = WidgetColors.secondary, fontSize = 7.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@androidx.compose.runtime.Composable
private fun LegendRow(category: WidgetCategory, showAmount: Boolean, compact: Boolean = false) {
    Row(GlanceModifier.fillMaxWidth().padding(vertical = if (compact) 2.dp else 3.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(GlanceModifier.size(if (compact) 6.dp else 7.dp).background(ColorProvider(Color(category.color), Color(category.color))).cornerRadius(50.dp)) {}
        Spacer(GlanceModifier.width(if (compact) 5.dp else 7.dp))
        Text(category.name, style = TextStyle(color = WidgetColors.primary, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        if (showAmount) {
            Text(formatCompactMoney(category.amountCents), style = TextStyle(color = WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.width(6.dp))
        }
        Text("${category.percent}%", style = TextStyle(color = WidgetColors.primary, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Bold))
    }
}

@androidx.compose.runtime.Composable
private fun TrendText(snapshot: WidgetSnapshot, compact: Boolean) {
    val previous = snapshot.previousSpentCents
    val delta = if (previous > 0L) (((snapshot.spentCents - previous) * 100f) / previous).toInt() else null
    val text = when {
        delta == null -> "FIRST 7 DAYS"
        delta > 0 -> "↑ ${delta}% VS PRIOR"
        delta < 0 -> "↓ ${-delta}% VS PRIOR"
        else -> "SAME AS PRIOR"
    }
    val color = when {
        delta == null || delta == 0 -> WidgetColors.secondary
        delta < 0 -> WidgetColors.positive
        else -> WidgetColors.negative
    }
    Text(text, style = TextStyle(color = color, fontSize = if (compact) 7.sp else 9.sp, fontWeight = FontWeight.Bold), maxLines = 1)
}

@androidx.compose.runtime.Composable
private fun EmptyBreakdown() {
    Column(GlanceModifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
        Text("NO EXPENSES YET", style = TextStyle(color = WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold))
        Text("Tap to open Transactions", style = TextStyle(color = WidgetColors.primary, fontSize = 10.sp))
    }
}

private object WidgetColors {
    val background = ColorProvider(Color(0xFF181B19), Color(0xFF181B19))
    val primary = ColorProvider(Color(0xFFF3F6F3), Color(0xFFF3F6F3))
    val secondary = ColorProvider(Color(0xFF939D96), Color(0xFF939D96))
    val signal = ColorProvider(Color(0xFFDEFF67), Color(0xFFDEFF67))
    val onSignal = ColorProvider(Color(0xFF111411), Color(0xFF111411))
    val divider = ColorProvider(Color(0xFF303632), Color(0xFF303632))
    val positive = ColorProvider(Color(0xFF42BCA3), Color(0xFF42BCA3))
    val negative = ColorProvider(Color(0xFFF36B5D), Color(0xFFF36B5D))
}

class NudgeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NudgeWidget()
}

class NudgeCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NudgeWidget()
}

class NudgeSnapshotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NudgeWidget()
}

private fun widgetAmount(snapshot: WidgetSnapshot): String =
    if (snapshot.hideAmounts) "₹ ••••" else formatCompactMoney(snapshot.spentCents)

private fun formatCompactMoney(cents: Long): String = "₹${formatCompactCentsPlain(cents)}"

private fun createDonutBitmap(categories: List<WidgetCategory>): Bitmap {
    val size = 320
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val stroke = 34f
    val inset = stroke / 2f + 10f
    val bounds = RectF(inset, inset, size - inset, size - inset)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    paint.color = 0xFF303632.toInt()
    canvas.drawArc(bounds, -90f, 360f, false, paint)
    if (categories.isEmpty()) return bitmap

    val total = max(1L, categories.sumOf { it.amountCents })
    var start = -90f
    categories.forEach { category ->
        val sweep = 360f * category.amountCents / total
        val gap = minOf(8f, sweep * .18f)
        paint.color = category.color
        canvas.drawArc(bounds, start + gap / 2f, max(1f, sweep - gap), false, paint)
        start += sweep
    }
    return bitmap
}
