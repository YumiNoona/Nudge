package com.nudge.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.R
import com.nudge.android.ui.theme.*

private const val DONATION_UPI_ID = "rushikeshingale2001@okicici"

@Composable
fun DonateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val paymentUri = "upi://pay?pa=$DONATION_UPI_ID&pn=Rushikesh%20Ingale&cu=INR"

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) {
                Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft())
            }
            Text("Support Nudge", style = DSTypography.headlineLarge, color = DSBridge.ink(), modifier = Modifier.align(Alignment.Center))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "If Nudge saves you time, you can support its independent development.",
                style = DSTypography.bodyMedium,
                color = DSBridge.inkSoft(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = DSBridge.surface(),
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.accent().copy(alpha = .16f)),
            ) {
                Image(
                    painter = painterResource(R.drawable.donate_upi_qr),
                    contentDescription = "UPI donation QR for Rushikesh Ingale",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1336f / 1848f).clip(RoundedCornerShape(26.dp)),
                )
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = DSBridge.accentBg()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(DSBridge.accent().copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Lucide.QrCode(size = 19.dp, color = DSBridge.accent())
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("UPI ID", fontFamily = MonoFamily, fontSize = 8.sp, color = DSBridge.inkMute(), letterSpacing = 1.sp)
                        Text(DONATION_UPI_ID, fontFamily = MonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DSBridge.ink())
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Nudge donation UPI ID", DONATION_UPI_ID))
                        Toast.makeText(context, "UPI ID copied", Toast.LENGTH_SHORT).show()
                    }) { Lucide.Copy(size = 18.dp, color = DSBridge.accent()) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(paymentUri))) }
                        .onFailure { Toast.makeText(context, "No UPI app found. Copy the UPI ID instead.", Toast.LENGTH_LONG).show() }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DSBridge.accent()),
            ) {
                Lucide.OpenInNew(size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text("Open a UPI app", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(34.dp).navigationBarsPadding())
        }
    }
}
