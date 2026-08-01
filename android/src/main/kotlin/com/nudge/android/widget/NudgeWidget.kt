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
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import kotlin.math.max

private data class WidgetCategory(
    val name: String,
    val amountCents: Long,
    val percent: Int,
    val color: Int
)

private data class WidgetSnapshot(
    val spentCents: Long,
    val categories: List<WidgetCategory>,
    val needsReview: Int,
    val captureOn: Boolean,
    val hideAmounts: Boolean
)

class NudgeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp), // compact
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
        val recentExpenses = transactions.filter { it.type == "debit" && it.timestampEpoch >= sevenDaysAgo }
        val total = recentExpenses.sumOf { it.amountCents }
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
            categories = top,
            needsReview = transactions.count { !it.isReviewed },
            captureOn = preferences.getBoolean("auto_capture_enabled", true),
            hideAmounts = preferences.getBoolean("hide_widget_amounts", false)
        )
        val donut = createDonutBitmap(top)
        val openApp = Intent(context, MainActivity::class.java)
        val quickAdd = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ADD, true)
        val openReview = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_REVIEW, true)

        provideContent {
            val size = LocalSize.current
            when {
                size.height >= 220.dp && size.height > size.width -> PortraitWidget(snapshot, donut, openApp, quickAdd)
                size.width >= 270.dp -> LandscapeWidget(snapshot, donut, openApp, quickAdd, openReview)
                else -> CompactWidget(snapshot, openApp, quickAdd)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PortraitWidget(snapshot: WidgetSnapshot, donut: Bitmap, openApp: Intent, quickAdd: Intent) {
    WidgetCard(openApp, padding = 14) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            StatusPill("EXPENSES")
            Spacer(GlanceModifier.defaultWeight())
            AddButton(quickAdd, compact = true)
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
    openApp: Intent,
    quickAdd: Intent,
    openReview: Intent
) {
    WidgetCard(openApp, padding = 14) {
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
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text("NUDGE", style = TextStyle(color = WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                        Text(
                            if (snapshot.captureOn) "AUTO CAPTURE ON" else "AUTO CAPTURE OFF",
                            style = TextStyle(color = if (snapshot.captureOn) WidgetColors.signal else WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                    AddButton(quickAdd, compact = true)
                }
                Spacer(GlanceModifier.height(7.dp))
                if (snapshot.categories.isEmpty()) {
                    EmptyBreakdown()
                } else {
                    snapshot.categories.take(3).forEach { LegendRow(it, showAmount = !snapshot.hideAmounts) }
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    when {
                        snapshot.needsReview == 0 -> "ALL CAUGHT UP"
                        snapshot.needsReview == 1 -> "1 ITEM TO REVIEW →"
                        else -> "${snapshot.needsReview} ITEMS TO REVIEW →"
                    },
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(actionStartActivity(openReview)),
                    style = TextStyle(
                        color = if (snapshot.needsReview == 0) WidgetColors.secondary else WidgetColors.signal,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun CompactWidget(snapshot: WidgetSnapshot, openApp: Intent, quickAdd: Intent) {
    WidgetCard(openApp, padding = 14) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text("NUDGE · ${if (snapshot.captureOn) "CAPTURE ON" else "CAPTURE OFF"}", style = TextStyle(color = WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(6.dp))
                Text(widgetAmount(snapshot), style = TextStyle(color = WidgetColors.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                Text("past 7 days", style = TextStyle(color = WidgetColors.secondary, fontSize = 10.sp))
            }
            AddButton(quickAdd, compact = false)
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
private fun AddButton(intent: Intent, compact: Boolean) {
    Box(
        GlanceModifier.size(if (compact) 38.dp else 48.dp).background(WidgetColors.signal)
            .cornerRadius(if (compact) 13.dp else 16.dp).clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center
    ) {
        Text("+", style = TextStyle(color = WidgetColors.onSignal, fontSize = if (compact) 20.sp else 25.sp, fontWeight = FontWeight.Bold))
    }
}

@androidx.compose.runtime.Composable
private fun DonutAmount(donut: Bitmap, amount: String, modifier: GlanceModifier, amountSize: Int) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(ImageProvider(donut), contentDescription = "Spending categories", modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(amount, style = TextStyle(color = WidgetColors.primary, fontSize = amountSize.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text("SPENT", style = TextStyle(color = WidgetColors.secondary, fontSize = 8.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@androidx.compose.runtime.Composable
private fun LegendRow(category: WidgetCategory, showAmount: Boolean) {
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(GlanceModifier.size(7.dp).background(ColorProvider(Color(category.color), Color(category.color))).cornerRadius(50.dp)) {}
        Spacer(GlanceModifier.width(7.dp))
        Text(category.name, style = TextStyle(color = WidgetColors.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        if (showAmount) {
            Text(formatCompactMoney(category.amountCents), style = TextStyle(color = WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.width(6.dp))
        }
        Text("${category.percent}%", style = TextStyle(color = WidgetColors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
    }
}

@androidx.compose.runtime.Composable
private fun EmptyBreakdown() {
    Column(GlanceModifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
        Text("NO EXPENSES YET", style = TextStyle(color = WidgetColors.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold))
        Text("Tap + to add one", style = TextStyle(color = WidgetColors.primary, fontSize = 10.sp))
    }
}

private object WidgetColors {
    val background = ColorProvider(Color(0xFF181B19), Color(0xFF181B19))
    val primary = ColorProvider(Color(0xFFF3F6F3), Color(0xFFF3F6F3))
    val secondary = ColorProvider(Color(0xFF939D96), Color(0xFF939D96))
    val signal = ColorProvider(Color(0xFFDEFF67), Color(0xFFDEFF67))
    val onSignal = ColorProvider(Color(0xFF111411), Color(0xFF111411))
}

class NudgeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NudgeWidget()
}

private fun widgetAmount(snapshot: WidgetSnapshot): String =
    if (snapshot.hideAmounts) "₹ ••••" else formatCompactMoney(snapshot.spentCents)

private fun formatCompactMoney(cents: Long): String {
    val whole = cents / 100.0
    return when {
        whole >= 10_000_000 -> "₹${trimDecimal(whole / 10_000_000)}Cr"
        whole >= 100_000 -> "₹${trimDecimal(whole / 100_000)}L"
        whole >= 1_000 -> "₹${trimDecimal(whole / 1_000)}K"
        else -> formatWidgetMoney(cents)
    }
}

private fun trimDecimal(value: Double): String = if (value >= 10 || value % 1.0 == 0.0) "%.0f".format(Locale.US, value) else "%.1f".format(Locale.US, value)

private fun formatWidgetMoney(cents: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
        currency = Currency.getInstance("INR")
        maximumFractionDigits = if (cents % 100 == 0L) 0 else 2
    }
    return format.format(cents / 100.0)
}

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
