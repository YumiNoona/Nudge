package com.nudge.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.NudgeColors

private data class WelcomePage(val symbol: String, val eyebrow: String, val title: String, val body: String)

private val welcomePages = listOf(
    WelcomePage("◌", "WELCOME TO NUDGE", "Money clarity,\nwithout the cloud.", "A calm, private home for every expense—automatically captured and always owned by you."),
    WelcomePage("✦", "AUTOMATIC CAPTURE", "Your spending finds\nits way home.", "Bank SMS and UPI notifications are understood on your phone. Your messages never leave this device."),
    WelcomePage("↗", "A GENTLER ROUTINE", "Review in seconds.\nLearn for life.", "Swipe to confirm a category. Nudge learns quietly and turns your activity into useful patterns—not guilt."),
    WelcomePage("✓", "YOU'RE READY", "Your private money\nspace is ready.", "Start locally. Add encrypted backups or another device only when you choose.")
)

@Composable
fun OnboardingScreen(isDark: Boolean, onComplete: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val ink = if (isDark) NudgeColors.InkDark else NudgeColors.Ink
    val muted = if (isDark) NudgeColors.InkSoftDark else NudgeColors.InkSoft
    val surface = if (isDark) NudgeColors.DarkBg else Color(0xFFF8F7FC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(NudgeColors.Purple.copy(alpha = .22f), surface),
                    radius = 1100f
                )
            )
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(NudgeColors.Purple, NudgeColors.PurpleDeep))), contentAlignment = Alignment.Center) {
                    Text("N", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text("Nudge", color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text("Skip", color = muted, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onComplete).padding(8.dp))
        }

        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(220)) },
            modifier = Modifier.align(Alignment.Center)
        ) { index ->
            val item = welcomePages[index]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(42.dp))
                        .background((if (isDark) NudgeColors.SurfaceDark else Color.White).copy(alpha = .78f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.symbol, color = NudgeColors.Purple, fontSize = 58.sp, fontWeight = FontWeight.Light)
                }
                Spacer(Modifier.height(42.dp))
                Text(item.eyebrow, color = NudgeColors.Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(12.dp))
                Text(item.title, color = ink, fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(item.body, color = muted, fontSize = 15.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            }
        }

        Row(Modifier.align(Alignment.BottomStart).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            welcomePages.indices.forEach { i ->
                Box(Modifier.width(if (i == page) 28.dp else 7.dp).height(7.dp).clip(CircleShape).background(if (i <= page) NudgeColors.Purple else NudgeColors.InkMute.copy(alpha = .28f)))
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(NudgeColors.Purple, NudgeColors.PurpleDeep)))
                .clickable { if (page == welcomePages.lastIndex) onComplete() else page++ }
                .padding(horizontal = 20.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (page == welcomePages.lastIndex) "Enter Nudge" else "Continue", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("→", color = Color.White, fontSize = 18.sp)
        }
    }
}
