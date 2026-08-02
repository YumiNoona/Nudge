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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
    val pager = rememberPagerState(pageCount = { 4 })
    var name by remember { mutableStateOf(preferences.getString("display_name", "") ?: "") }
    var currency by remember { mutableStateOf(preferences.getString("currency_code", "INR") ?: "INR") }
    fun finish() {
        preferences.edit()
            .putBoolean("onboarding_complete", true)
            .putString("display_name", name.trim().ifBlank { "You" })
            .putString("currency_code", currency)
            .apply()
        onDone()
    }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${pager.currentPage + 1} / 4", fontFamily = MonoFamily, color = DSBridge.inkMute(), fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < 3) {
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
                    2 -> PersonalizePage(name, { name = it }, currency, { currency = it })
                    else -> ReadyPage()
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { index ->
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
                    if (pager.currentPage == 3) finish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                shape = RoundedCornerShape(17.dp),
                color = DS.AccentDeep,
                shadowElevation = 6.dp
            ) {
                Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (pager.currentPage == 3) "Start tracking" else "Continue", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
private fun PersonalizePage(name: String, onName: (String) -> Unit, currency: String, onCurrency: (String) -> Unit) {
    OnboardingFrame(
        eyebrow = "MAKE IT YOURS",
        title = "A money space\nthat feels personal.",
        body = "Choose your display name and how amounts appear. You can change both later."
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = .08f)),
            shape = RoundedCornerShape(28.dp),
            color = DSBridge.surface(),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(19.dp)).background(DSBridge.accentBg()), contentAlignment = Alignment.Center) {
                        Text(name.trim().take(1).ifBlank { "Y" }.uppercase(), color = DSBridge.accent(), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DISPLAY NAME", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = 1.sp, color = DSBridge.inkMute())
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = name,
                            onValueChange = { onName(it.take(32)) },
                            singleLine = true,
                            textStyle = TextStyle(color = DSBridge.ink(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = SolidColor(DSBridge.accent()),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DSBridge.background()).padding(horizontal = 12.dp, vertical = 10.dp),
                            decorationBox = { field ->
                                Box {
                                    if (name.isBlank()) Text("Your name", color = DSBridge.inkMute(), fontSize = 14.sp)
                                    field()
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(17.dp))
                androidx.compose.material3.HorizontalDivider(color = DSBridge.inkMute().copy(alpha = .12f))
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("DISPLAY CURRENCY", fontFamily = MonoFamily, fontSize = 8.sp, letterSpacing = 1.sp, color = DSBridge.inkMute())
                        Text("Used across your totals", fontSize = 10.sp, color = DSBridge.inkSoft())
                    }
                    Text(currency, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DSBridge.accent())
                }
                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("INR" to "₹", "USD" to "$", "EUR" to "€", "GBP" to "£").forEach { (code, symbol) ->
                        val selected = code == currency
                        Surface(
                            onClick = { onCurrency(code) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) DSBridge.accentBg() else DSBridge.background(),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) DSBridge.accent().copy(alpha = .42f) else Color.Transparent,
                            ),
                        ) {
                            Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(symbol, color = if (selected) DSBridge.accent() else DSBridge.ink(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(code, color = if (selected) DSBridge.accent() else DSBridge.inkMute(), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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
