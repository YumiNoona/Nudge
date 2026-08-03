package com.nudge.android.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.DS
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.DSTypography
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.MonoFamily

private data class CoachStep(
    val title: String,
    val body: String,
    val target: TourTarget,
    val fallback: (Dp, Dp) -> TourRect,
    val forceLowCard: Boolean = false,
    val highlightHeightLimit: Dp? = null,
)

private data class TourRect(val x: Dp, val y: Dp, val width: Dp, val height: Dp)

private val coachSteps = listOf(
    CoachStep(
        "Your control center",
        "Tap your profile to manage capture, accounts, categories, widgets, backups and appearance.",
        TourTarget.Profile,
        { _, _ -> TourRect(10.dp, 32.dp, 58.dp, 58.dp) },
    ),
    CoachStep(
        "Start with the month",
        "This live summary shows money in, spending, refunds and net cash flow at a glance.",
        TourTarget.MonthSummary,
        { width, _ -> TourRect(10.dp, 93.dp, width - 20.dp, 188.dp) },
    ),
    CoachStep(
        "Your transaction timeline",
        "Automatic and manual entries meet here. Tap any row to correct its merchant, category or account.",
        TourTarget.TransactionFilters,
        { width, _ -> TourRect(10.dp, 305.dp, width - 20.dp, 52.dp) },
        forceLowCard = true,
    ),
    CoachStep(
        "Return to transactions",
        "This tab always brings you back to the complete chronological record of your money.",
        TourTarget.TransactionsTab,
        { width, height -> TourRect(18.dp, height - 112.dp, width * .32f, 86.dp) },
    ),
    CoachStep(
        "Add in seconds",
        "Tap +, enter the amount, then choose a type, category and account. Nudge handles the rest.",
        TourTarget.AddButton,
        { width, height -> TourRect(width / 2 - 48.dp, height - 120.dp, 96.dp, 96.dp) },
    ),
    CoachStep(
        "Open Analytics",
        "Use Analytics when you want patterns and comparisons instead of a list of individual entries.",
        TourTarget.AnalyticsTab,
        { width, height -> TourRect(width - (width * .34f) - 16.dp, height - 112.dp, width * .34f, 86.dp) },
    ),
    CoachStep(
        "Understand where money went",
        "The expense mix, daily rhythm and key insights turn your transaction history into useful decisions.",
        TourTarget.AnalyticsHero,
        { width, _ -> TourRect(10.dp, 126.dp, width - 20.dp, 220.dp) },
        forceLowCard = true,
        highlightHeightLimit = 220.dp,
    ),
)

@Composable
fun AppTourOverlay(
    step: Int,
    targetRegistry: TourTargetRegistry,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val safeStep = step.coerceIn(coachSteps.indices)
    val coach = coachSteps[safeStep]
    val pulse = rememberInfiniteTransition(label = "tourPulse").animateFloat(
        initialValue = .55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tourPulseAlpha",
    ).value

    BoxWithConstraints(
        Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } },
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val fallback = coach.fallback(maxWidth, maxHeight)
        val measured = targetRegistry.bounds[coach.target]
        val targetCenterY = measured?.center?.y?.let { with(density) { it.toDp() } }
            ?: (fallback.y + fallback.height / 2)
        val placeCardAtBottom = coach.forceLowCard || targetCenterY < maxHeight / 2
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val inset = 4.dp.toPx()
            val fallbackLeft = with(density) { fallback.x.toPx() }
            val fallbackTop = with(density) { fallback.y.toPx() }
            val fallbackRight = fallbackLeft + with(density) { fallback.width.toPx() }
            val fallbackBottom = fallbackTop + with(density) { fallback.height.toPx() }
            val left = ((measured?.left ?: fallbackLeft) - inset).coerceAtLeast(0f)
            val top = ((measured?.top ?: fallbackTop) - inset).coerceAtLeast(0f)
            val right = ((measured?.right ?: fallbackRight) + inset).coerceAtMost(size.width)
            val measuredBottom = measured?.let { bounds ->
                coach.highlightHeightLimit?.let { limit ->
                    minOf(bounds.bottom, bounds.top + with(density) { limit.toPx() })
                } ?: bounds.bottom
            }
            val bottom = ((measuredBottom ?: fallbackBottom) + inset).coerceAtMost(size.height)
            val scrim = Color.Black.copy(alpha = .76f)
            drawRect(scrim)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = DS.Signal.copy(alpha = pulse),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Surface(
            modifier = Modifier
                .align(if (placeCardAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(horizontal = 18.dp)
                .padding(
                    top = if (placeCardAtBottom) 0.dp else 88.dp,
                    bottom = if (placeCardAtBottom) {
                        if (coach.forceLowCard) 22.dp else 112.dp
                    } else 0.dp,
                ),
            shape = RoundedCornerShape(24.dp),
            color = DSBridge.surface(),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = DSBridge.accentBg()) {
                        Text(
                            "${safeStep + 1} / ${coachSteps.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = DSBridge.accent(),
                            fontFamily = MonoFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onSkip,
                        colors = ButtonDefaults.textButtonColors(contentColor = DSBridge.inkMute()),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) { Text("Skip", fontSize = 11.sp) }
                }
                Spacer(Modifier.height(10.dp))
                Text(coach.title, style = DSTypography.headlineMedium, color = DSBridge.ink())
                Spacer(Modifier.height(7.dp))
                Text(coach.body, style = DSTypography.bodyMedium, color = DSBridge.inkSoft(), lineHeight = 19.sp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (safeStep > 0) {
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DSBridge.surfaceVariant(), contentColor = DSBridge.ink()),
                        ) {
                            Lucide.ChevronLeft(size = 17.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("Back")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onNext,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent()),
                    ) {
                        Text(if (safeStep == coachSteps.lastIndex) "Finish" else "Next")
                        Spacer(Modifier.width(5.dp))
                        if (safeStep < coachSteps.lastIndex) Lucide.ChevronRight(size = 17.dp)
                        else Lucide.Check(size = 17.dp)
                    }
                }
            }
        }
    }
}
