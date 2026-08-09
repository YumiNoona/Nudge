package com.nudge.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.*

private const val PUBLIC_PRIVACY_URL = "https://github.com/YumiNoona/Nudge/blob/main/PRIVACY.md"

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) {
                Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft())
            }
            Text("Privacy", style = DSTypography.headlineLarge, color = DSBridge.ink(), modifier = Modifier.align(Alignment.Center))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(24.dp), color = DSBridge.accentBg()) {
                Column(Modifier.padding(18.dp)) {
                    Lucide.Shield(size = 24.dp, color = DSBridge.accent())
                    Spacer(Modifier.height(12.dp))
                    Text("Your money stays yours", color = DSBridge.ink(), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Nudge processes financial alerts, statements and receipts on your device. It has no account system, ads, analytics SDK or cloud financial database.", color = DSBridge.inkSoft(), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            PrivacySection("Messages & notifications", "Nudge inspects permitted SMS/MMS and notifications locally to identify transactions. Unrelated content is ignored. A matched source is retained only when message saving is enabled, and then it is encrypted with Android Keystore.")
            PrivacySection("Camera & files", "Camera access is requested only when you scan a receipt. Files and images are selected through Android's private system picker. OCR and parsing happen on-device.")
            PrivacySection("Network access", "The Google Play edition uses the network for Play-managed app delivery and updates. Nudge does not upload your transaction records, statements, receipts, contact data or message content.")
            PrivacySection("Your controls", "Disable automatic capture, revoke permissions, delete saved sources, export a backup, or erase all app data at any time from Settings.")
            Surface(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PUBLIC_PRIVACY_URL))) },
                shape = RoundedCornerShape(18.dp),
                color = DSBridge.surface(),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.FileText(size = 19.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Full privacy policy", color = DSBridge.ink(), fontSize = 13.sp)
                        Text("View the current public version", color = DSBridge.inkMute(), fontSize = 9.sp)
                    }
                    Lucide.ChevronRight(size = 17.dp, color = DSBridge.inkMute())
                }
            }
            Text("Effective 9 August 2026", color = DSBridge.inkMute(), fontFamily = MonoFamily, fontSize = 9.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Surface(shape = RoundedCornerShape(19.dp), color = DSBridge.surface()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = DSBridge.ink(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = DSBridge.inkSoft(), fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}
