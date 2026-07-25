package com.nudge.android.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val prefs = remember { ctx.getSharedPreferences("nudge_prefs", android.content.Context.MODE_PRIVATE) }

    // Step state
    var displayName by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCurrency by remember { mutableStateOf(prefs.getString("currency", "INR") ?: "INR") }
    val smsGranted = remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) avatarUri = uri
    }

    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        smsGranted.value = grants.values.all { it }
    }

    fun completeOnboarding() {
        prefs.edit()
            .putBoolean("onboarding_complete", true)
            .putString("currency", selectedCurrency)
            .apply()
        if (displayName.isNotBlank()) prefs.edit().putString("display_name", displayName).apply()
        if (avatarUri != null) prefs.edit().putString("avatar_uri", avatarUri.toString()).apply()
        onDone()
    }

    Column(modifier = Modifier.fillMaxSize().background(NudgeColors.Bone)) {
        // Progress dots
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == i) 28.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (pagerState.currentPage == i) NudgeColors.Emerald
                            else NudgeColors.InkMute.copy(alpha = 0.3f)
                        )
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = pagerState.currentPage < 4 // can't swipe past last page
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> {
                        // Welcome
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(
                                Brush.linearGradient(listOf(NudgeColors.Emerald, NudgeColors.EmeraldDeep))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Lucide.Wallet(size = 36.dp, strokeWidth = 1.5.dp, color = Color.White)
                        }
                        Spacer(Modifier.height(32.dp))
                        Text("Welcome to Nudge", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                        Spacer(Modifier.height(12.dp))
                        Text("Track expenses automatically. No spreadsheets, no guilt.", fontSize = 15.sp, color = NudgeColors.InkSoft, textAlign = TextAlign.Center, lineHeight = 22.sp)
                    }
                    1 -> {
                        // Permissions explainer
                        Lucide.Shield(size = 48.dp, strokeWidth = 1.5.dp, color = NudgeColors.Emerald)
                        Spacer(Modifier.height(24.dp))
                        Text("Stay private, stay smart", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "We read bank & UPI SMS on-device to auto-log expenses. Nothing leaves your phone. No account required. You control what we access.",
                            fontSize = 14.sp, color = NudgeColors.InkSoft, textAlign = TextAlign.Center, lineHeight = 22.sp
                        )
                    }
                    2 -> {
                        // Currency
                        Lucide.Wallet(size = 48.dp, strokeWidth = 1.5.dp, color = NudgeColors.Emerald)
                        Spacer(Modifier.height(24.dp))
                        Text("Your currency", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                        Spacer(Modifier.height(16.dp))
                        val currencies = listOf("INR" to "₹", "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "AUD" to "A$", "CAD" to "C$", "SGD" to "S$")
                        currencies.forEach { (code, symbol) ->
                            val sel = selectedCurrency == code
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCurrency = code },
                                shape = RoundedCornerShape(14.dp),
                                color = if (sel) NudgeColors.EmeraldBg else NudgeColors.Surface
                            ) {
                                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("$symbol  $code", fontSize = 16.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) NudgeColors.Emerald else NudgeColors.Ink)
                                    if (sel) Text("✓", color = NudgeColors.Emerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    3 -> {
                        // Profile
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(NudgeColors.InkMute.copy(alpha = 0.15f)).clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                // Would load via Coil/Glide in production — show checkmark for now
                                Lucide.Check(size = 32.dp, strokeWidth = 2.dp, color = NudgeColors.Emerald)
                            } else {
                                Lucide.User(size = 32.dp, strokeWidth = 1.5.dp, color = NudgeColors.InkMute)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Tap to add photo", fontSize = 12.sp, color = NudgeColors.InkSoft)
                        Spacer(Modifier.height(24.dp))
                        Text("What should we call you?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = { Text("Your name", color = NudgeColors.InkMute) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    4 -> {
                        // Done
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(NudgeColors.EmeraldBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Lucide.Check(size = 36.dp, strokeWidth = 2.5.dp, color = NudgeColors.Emerald)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("You're all set!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nudge will now track your expenses as they happen. Open the app anytime to review, budget, and earn rewards.",
                            fontSize = 14.sp, color = NudgeColors.InkSoft, textAlign = TextAlign.Center, lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Bottom button
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp)) {
            when (pagerState.currentPage) {
                0 -> Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
                ) { Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                1 -> {
                    Column {
                        Button(
                            onClick = {
                                smsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
                        ) { Text("Grant SMS Access", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Skip for now", fontSize = 14.sp, color = NudgeColors.InkSoft) }
                    }
                }
                2 -> Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(3) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
                ) { Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                3 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(4) } },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Skip", fontSize = 14.sp, color = NudgeColors.InkSoft) }
                        Button(
                            onClick = { scope.launch { pagerState.animateScrollToPage(4) } },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
                        ) { Text("Save & Continue", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
                4 -> Button(
                    onClick = { completeOnboarding() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
                ) { Text("Start Tracking", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
