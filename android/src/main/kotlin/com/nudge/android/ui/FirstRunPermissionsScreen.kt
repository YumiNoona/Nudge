package com.nudge.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nudge.android.ui.theme.*

private const val PRIVACY_POLICY_URL = "https://github.com/YumiNoona/Nudge/blob/main/PRIVACY.md"

/**
 * Prominent disclosure shown before Nudge asks for restricted financial-message access.
 * Camera access is also offered here so receipt capture never interrupts a scanning flow.
 * Files continue to use Android's privacy-preserving system picker and need no broad access.
 */
@Composable
fun FirstRunPermissionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    @Suppress("UNUSED_VARIABLE") val refreshSnapshot = refresh

    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    val smsGranted = granted(Manifest.permission.READ_SMS) && granted(Manifest.permission.RECEIVE_SMS)
    val cameraGranted = granted(Manifest.permission.CAMERA)
    val notificationAccessGranted =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    val smsRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }
    val notificationAccess = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh++
    }
    val cameraRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()
            .navigationBarsPadding().padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(22.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = DSBridge.accentBg()) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Lucide.Shield(size = 25.dp, color = DSBridge.accent())
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Private automatic capture", color = DSBridge.ink(), fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Choose the sources Nudge may inspect on this device. You can skip either option and keep using manual expenses.",
            color = DSBridge.inkSoft(), fontSize = 12.sp, lineHeight = 18.sp,
        )
        Spacer(Modifier.height(22.dp))

        Surface(shape = RoundedCornerShape(24.dp), color = DSBridge.surface()) {
            Column {
                DisclosurePermissionRow(
                    title = "Bank & UPI messages",
                    disclosure = "Reads SMS/MMS on-device to find debits, credits and refunds. Only matched financial entries are added; unrelated messages are not saved.",
                    granted = smsGranted,
                    icon = { Lucide.Message(size = 20.dp, color = DSBridge.accent()) },
                    onClick = {
                        smsRequest.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                    },
                )
                HorizontalDivider(color = DSBridge.background())
                DisclosurePermissionRow(
                    title = "Payment notifications",
                    disclosure = "Inspects notification text on-device to recognize payment alerts. Non-financial notifications are ignored and never retained.",
                    granted = notificationAccessGranted,
                    icon = { Lucide.Bell(size = 20.dp, color = DSBridge.accent()) },
                    onClick = { notificationAccess.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                )
                HorizontalDivider(color = DSBridge.background())
                DisclosurePermissionRow(
                    title = "Receipt camera",
                    disclosure = "Captures bills inside Nudge for on-device text recognition. Photos are only kept when you choose to save the receipt.",
                    granted = cameraGranted,
                    icon = { Lucide.Camera(size = 20.dp, color = DSBridge.accent()) },
                    onClick = { cameraRequest.launch(Manifest.permission.CAMERA) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Processing is local. Nudge does not upload message content, sell financial data, use ads, or request broad file access.",
            color = DSBridge.inkMute(), fontSize = 10.sp, lineHeight = 15.sp,
        )
        TextButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
        ) { Text("Read privacy policy", color = DSBridge.accent(), fontSize = 10.sp) }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(19.dp),
        ) {
            Text(if (smsGranted || notificationAccessGranted || cameraGranted) "Continue" else "Continue without capture", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(9.dp))
            Lucide.ChevronRight(size = 18.dp)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DisclosurePermissionRow(
    title: String,
    disclosure: String,
    granted: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(DSBridge.accentBg(), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = DSBridge.ink(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(disclosure, color = DSBridge.inkMute(), fontSize = 9.sp, lineHeight = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(27.dp).background(if (granted) DSBridge.accentBg() else DSBridge.background(), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (granted) Lucide.Check(size = 15.dp, color = DSBridge.accent())
                else Lucide.ChevronRight(size = 15.dp, color = DSBridge.inkMute())
            }
        }
    }
}
