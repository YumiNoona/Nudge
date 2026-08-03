package com.nudge.android.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.R
import com.nudge.android.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { 3 })
    fun finish() {
        preferences.edit()
            .putBoolean("onboarding_complete", true)
            .apply()
        onDone()
    }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${pager.currentPage + 1} / 3", fontFamily = MonoFamily, color = DSBridge.inkMute(), fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < 2) {
                Text(
                    "Skip",
                    color = DSBridge.inkSoft(),
                    fontSize = 12.sp,
                    modifier = Modifier.clip(CircleShape).clickable { finish() }.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            userScrollEnabled = true
        ) { page ->
            val pageOffset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    alpha = 1f - (pageOffset * .24f)
                    scaleX = 1f - (pageOffset * .045f)
                    scaleY = 1f - (pageOffset * .045f)
                    translationY = pageOffset * 18.dp.toPx()
                }
            ) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> CapturePage()
                    else -> ReadyPage()
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(
                        Modifier.width(if (pager.currentPage == index) 24.dp else 7.dp).height(7.dp)
                            .clip(CircleShape)
                            .background(if (index <= pager.currentPage) DS.Accent else DSBridge.inkMute().copy(alpha = .22f))
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = {
                    if (pager.currentPage == 2) finish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                shape = RoundedCornerShape(17.dp),
                color = DS.AccentDeep,
                shadowElevation = 6.dp
            ) {
                Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (pager.currentPage == 2) "Start tracking" else "Continue", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(DS.Signal), contentAlignment = Alignment.Center) {
                        Lucide.ChevronRight(size = 17.dp, strokeWidth = 2.2.dp, color = DS.InkPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
private fun WelcomePage() {
    OnboardingFrame(
        eyebrow = "EFFORTLESS EXPENSES",
        title = "Your money,\nin one calm place.",
        body = "Bank and UPI alerts become a clean expense timeline—privately, right on your phone."
    ) {
        val motion = rememberInfiniteTransition(label = "welcomeIllustrationMotion")
        val floatY by motion.animateFloat(
            initialValue = -3f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "welcomeIllustrationFloat"
        )
        Box(Modifier.fillMaxWidth().height(258.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(250.dp).shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = .10f)),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFFF5F1E8)
            ) {
                Image(
                    painter = painterResource(R.drawable.onboarding_finance_story_v2),
                    contentDescription = "A person comfortably reviewing expenses on their phone",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.04f
                        translationY = floatY.dp.toPx()
                    }
                )
            }
        }
    }
}

@Composable
private fun CapturePage() {
    OnboardingFrame(
        eyebrow = "AUTOMATIC CAPTURE",
        title = "Your spending finds\nits own way in.",
        body = "Read-only transaction alerts. Processing happens on-device and message content is never uploaded."
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(292.dp)
                .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = .10f)),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFFF5F1E8),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_auto_capture_v2),
                contentDescription = "A private transaction alert becoming an organized expense",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ReadyPage() {
    OnboardingFrame(
        eyebrow = "READY WHEN YOU ARE",
        title = "Everything is ready.\nStart with clarity.",
        body = "Add your first expense or let automatic capture handle the next one. Everything stays editable."
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(250.dp).shadow(14.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = .09f)),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFFF5F1E8),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_ready_v2),
                contentDescription = "A person calmly reviewing an organized expense overview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun OnboardingFrame(eyebrow: String, title: String, body: String, visual: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { visual() }
        Text(eyebrow, color = DSBridge.accent(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = DSBridge.ink(), fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start, letterSpacing = (-1).sp)
        Spacer(Modifier.height(12.dp))
        Text(body, color = DSBridge.inkSoft(), fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Start, modifier = Modifier.widthIn(max = 330.dp))
        Spacer(Modifier.height(24.dp))
    }
}
