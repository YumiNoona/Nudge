package com.nudge.android.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nudge.android.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { 4 })
    var name by remember { mutableStateOf(preferences.getString("display_name", "") ?: "") }
    var currency by remember { mutableStateOf(preferences.getString("currency_code", "INR") ?: "INR") }
    var smsGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }

    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        smsGranted = result.values.all { it }
    }

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
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(DS.AccentDeep), contentAlignment = Alignment.Center) {
                Text("N", color = DS.Signal, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(Modifier.width(9.dp))
            Text("Nudge", color = DSBridge.ink(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(180)) },
                label = "onboardingPage",
                modifier = Modifier.fillMaxSize()
            ) { current ->
                when (current) {
                    0 -> WelcomePage()
                    1 -> CapturePage(
                        smsGranted = smsGranted,
                        onSms = { smsPermission.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) },
                        onNotifications = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    )
                    2 -> PersonalizePage(name, { name = it }, currency, { currency = it })
                    else -> ReadyPage(name.trim().ifBlank { "You" })
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
                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (pager.currentPage == 3) "Start tracking" else "Continue", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Text("→", color = DS.Signal, fontSize = 18.sp)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
private fun WelcomePage() {
    OnboardingFrame(
        eyebrow = "PRIVATE BY DESIGN",
        title = "Every expense,\nsettled quietly.",
        body = "Nudge turns bank and UPI alerts into a clean money timeline—right on your phone."
    ) {
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.width(278.dp).shadow(18.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = .12f)),
                shape = RoundedCornerShape(30.dp),
                color = DS.AccentDeep
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("This month", color = Color.White.copy(alpha = .62f), fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(DS.Signal))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("₹24,860", color = Color.White, fontFamily = MonoFamily, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Text("12% less than last month", color = DS.Signal, fontSize = 10.sp)
                    Spacer(Modifier.height(22.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .1f)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(DS.Signal), contentAlignment = Alignment.Center) {
                                Lucide.Check(size = 18.dp, strokeWidth = 2.dp, color = DS.InkPrimary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Swiggy", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Food · captured now", color = Color.White.copy(alpha = .55f), fontSize = 9.sp)
                            }
                            Text("−₹640", color = Color.White, fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturePage(smsGranted: Boolean, onSms: () -> Unit, onNotifications: () -> Unit) {
    OnboardingFrame(
        eyebrow = "AUTOMATIC CAPTURE",
        title = "Your spending finds\nits own way in.",
        body = "Read only transaction alerts. Processing happens on-device and message content is never uploaded."
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PermissionCard(
                icon = { color -> Lucide.Shield(size = 20.dp, strokeWidth = 1.7.dp, color = color) },
                title = "Bank & UPI SMS",
                detail = if (smsGranted) "Ready to capture" else "Detect debits and credits",
                enabled = smsGranted,
                onClick = onSms
            )
            PermissionCard(
                icon = { color -> Lucide.Bell(size = 20.dp, strokeWidth = 1.7.dp, color = color) },
                title = "Payment notifications",
                detail = "GPay, PhonePe, Paytm and banks",
                enabled = false,
                onClick = onNotifications
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Lucide.Shield(size = 14.dp, strokeWidth = 1.7.dp, color = DSBridge.inkMute())
                Spacer(Modifier.width(6.dp))
                Text("No account required · works offline", color = DSBridge.inkMute(), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: @Composable (Color) -> Unit,
    title: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = DSBridge.surface()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (enabled) DS.Signal else DSBridge.accentBg()), contentAlignment = Alignment.Center) {
                icon(if (enabled) DS.InkPrimary else DSBridge.accent())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = DSBridge.ink(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = DSBridge.inkMute(), fontSize = 10.sp)
            }
            Text(if (enabled) "Ready" else "Enable", color = if (enabled) DSBridge.positive() else DSBridge.accent(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PersonalizePage(name: String, onName: (String) -> Unit, currency: String, onCurrency: (String) -> Unit) {
    OnboardingFrame(
        eyebrow = "MAKE IT YOURS",
        title = "A money space\nthat feels personal.",
        body = "Choose what Nudge calls you and how amounts appear. You can change these later."
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
            Text("YOUR NAME", fontSize = 9.sp, letterSpacing = 1.1.sp, color = DSBridge.inkMute())
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(17.dp), color = DSBridge.surface()) {
                BasicTextField(
                    value = name,
                    onValueChange = onName,
                    singleLine = true,
                    textStyle = TextStyle(color = DSBridge.ink(), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(DSBridge.accent()),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    decorationBox = { field ->
                        if (name.isBlank()) Text("What should we call you?", color = DSBridge.inkMute(), fontSize = 13.sp)
                        field()
                    }
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("CURRENCY", fontSize = 9.sp, letterSpacing = 1.1.sp, color = DSBridge.inkMute())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("INR" to "₹", "USD" to "$", "EUR" to "€", "GBP" to "£").forEach { (code, symbol) ->
                    val selected = code == currency
                    Surface(
                        onClick = { onCurrency(code) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) DS.AccentDeep else DSBridge.surface()
                    ) {
                        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(symbol, color = if (selected) DS.Signal else DSBridge.ink(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(code, color = if (selected) Color.White else DSBridge.inkMute(), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyPage(name: String) {
    OnboardingFrame(
        eyebrow = "READY WHEN YOU ARE",
        title = "Welcome home,\n$name.",
        body = "Add your first expense or let Nudge capture the next one. Everything stays editable."
    ) {
        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(118.dp).clip(CircleShape).background(DS.AccentSoft), contentAlignment = Alignment.Center) {
                Box(Modifier.size(82.dp).clip(CircleShape).background(DS.Signal), contentAlignment = Alignment.Center) {
                    Lucide.Check(size = 36.dp, strokeWidth = 2.2.dp, color = DS.InkPrimary)
                }
            }
        }
    }
}

@Composable
private fun OnboardingFrame(eyebrow: String, title: String, body: String, visual: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { visual() }
        Text(eyebrow, color = DSBridge.accent(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = DSBridge.ink(), fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, letterSpacing = (-1).sp)
        Spacer(Modifier.height(12.dp))
        Text(body, color = DSBridge.inkSoft(), fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 330.dp))
        Spacer(Modifier.height(24.dp))
    }
}
