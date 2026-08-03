package com.nudge.android.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class TourStep(val eyebrow: String, val title: String, val body: String)

private val tourSteps = listOf(
    TourStep("YOUR TIMELINE", "See every money move", "Transactions keeps manual and automatic expenses together, organized by date and easy to edit."),
    TourStep("QUICK ADD", "Add one in seconds", "Tap the center + button, enter the amount, choose a category and account, then save."),
    TourStep("AUTO CAPTURE", "Let alerts do the work", "Enable SMS and notification access. Bank and UPI transaction alerts are parsed privately on your device."),
    TourStep("SMART REVIEW", "Teach Nudge once", "Open items that need review, confirm the right category or reject false detections. Nudge learns locally."),
    TourStep("ANALYTICS", "Notice useful patterns", "Analytics separates category mix, daily cash flow, largest expenses and your spending pace."),
    TourStep("SOURCE & PRIVACY", "Every capture stays explainable", "Use View source to inspect where an automatic transaction came from. Saved messages remain encrypted on-device."),
    TourStep("MAKE IT YOURS", "Shape your own system", "Create accounts and categories, add widgets, choose a theme, and export a private backup whenever you want."),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductTourScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { tourSteps.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${pager.currentPage + 1} / ${tourSteps.size}", fontFamily = MonoFamily, fontSize = 10.sp, color = DSBridge.inkMute())
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < tourSteps.lastIndex) {
                Text("Skip tour", color = DSBridge.inkSoft(), fontSize = 11.sp, modifier = Modifier.clip(CircleShape).clickable(onClick = onDone).padding(9.dp))
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val step = tourSteps[page]
            val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp).graphicsLayer {
                    alpha = 1f - offset * .25f
                    scaleX = 1f - offset * .04f
                    scaleY = 1f - offset * .04f
                    translationY = offset * 18.dp.toPx()
                },
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                TourVisual(page)
                Spacer(Modifier.height(28.dp))
                Text(step.eyebrow, fontFamily = MonoFamily, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = DSBridge.accent())
                Spacer(Modifier.height(9.dp))
                Text(step.title, style = DSTypography.displayMedium, color = DSBridge.ink())
                Spacer(Modifier.height(11.dp))
                Text(step.body, style = DSTypography.bodyMedium, color = DSBridge.inkSoft(), lineHeight = 20.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(tourSteps.size) { index ->
                    Box(
                        Modifier.width(if (pager.currentPage == index) 20.dp else 6.dp).height(6.dp).clip(CircleShape)
                            .background(if (index <= pager.currentPage) DSBridge.accent() else DSBridge.inkMute().copy(alpha = .2f)),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (pager.currentPage > 0) {
                Surface(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1, animationSpec = spring()) } },
                    shape = RoundedCornerShape(15.dp),
                    color = DSBridge.surfaceVariant(),
                ) { Lucide.ChevronLeft(Modifier.padding(14.dp), color = DSBridge.inkSoft(), size = 19.dp) }
                Spacer(Modifier.width(8.dp))
            }
            Button(
                onClick = {
                    if (pager.currentPage == tourSteps.lastIndex) onDone()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = spring()) }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent()),
                contentPadding = PaddingValues(horizontal = 17.dp, vertical = 13.dp),
            ) {
                Text(if (pager.currentPage == tourSteps.lastIndex) "Start using Nudge" else "Next", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Lucide.ChevronRight(size = 18.dp)
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

@Composable
private fun TourVisual(step: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(280.dp).shadow(18.dp, RoundedCornerShape(30.dp), spotColor = DSBridge.accent().copy(alpha = .24f)),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(DSBridge.accentBg(), DSBridge.surface(), DSBridge.accentBg()), Offset.Zero, Offset(900f, 650f)),
            ).padding(22.dp),
        ) {
            Box(Modifier.size(94.dp).align(Alignment.Center).background(DS.Signal, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                when (step) {
                    0 -> Lucide.ListTodo(size = 43.dp, color = DS.InkPrimary)
                    1 -> Lucide.Plus(size = 46.dp, color = DS.InkPrimary)
                    2 -> Lucide.Sparkles(size = 43.dp, color = DS.InkPrimary)
                    3 -> Lucide.Check(size = 46.dp, color = DS.InkPrimary)
                    4 -> Lucide.ChartBar(size = 44.dp, color = DS.InkPrimary)
                    5 -> Lucide.Message(size = 42.dp, color = DS.InkPrimary)
                    else -> Lucide.Settings(size = 43.dp, color = DS.InkPrimary)
                }
            }
            Surface(shape = RoundedCornerShape(50), color = DSBridge.surface(), modifier = Modifier.align(Alignment.TopStart)) {
                Text("STEP ${step + 1}", fontFamily = MonoFamily, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DSBridge.accent(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
            }
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { index ->
                    Box(Modifier.width((150 - index * 24).dp).height(7.dp).clip(CircleShape).background(DSBridge.inkMute().copy(alpha = .14f)))
                }
            }
            Text("NUDGE GUIDE", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = 1.2.sp, color = DSBridge.inkMute(), modifier = Modifier.align(Alignment.BottomEnd), textAlign = TextAlign.End)
        }
    }
}
