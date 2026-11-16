package com.nudge.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CaptureDiagnostics
import com.nudge.android.service.NudgeNotificationListener
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.MonoFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureHealthDialog(captureEnabled: Boolean, smsGranted: Boolean, notificationEnabled: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var events by remember { mutableStateOf(CaptureDiagnostics.read(context)) }
    var connected by remember { mutableStateOf(NudgeNotificationListener.isConnected()) }
    LaunchedEffect(Unit) {
        while (true) {
            events = CaptureDiagnostics.read(context)
            connected = NudgeNotificationListener.isConnected()
            delay(2000)
        }
    }
    val format = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = DSBridge.surface(),
        title = { Text("Capture health", fontFamily = MonoFamily) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Automatic capture: ${if (captureEnabled) "On" else "Paused"}")
                Text("SMS access: ${if (smsGranted) "Granted" else "Not granted"}")
                Text("Notifications: ${when { !notificationEnabled -> "Access not granted"; connected -> "Connected"; else -> "Waiting for Android to reconnect" }}")
                val last = events.firstOrNull { it.kind == "added" }
                Text("Last capture: ${last?.let { format.format(Date(it.at)) } ?: "No captures in recent activity"}")
                HorizontalDivider()
                Text("RECENT ACTIVITY", fontFamily = MonoFamily, fontSize = 11.sp)
                Text("Stored only on this device. No message text or account numbers are included.", fontSize = 11.sp)
                if (events.isEmpty()) Text("New capture activity will appear here.")
                events.forEach { event -> Column {
                    Text(event.reason, color = DSBridge.ink(), fontSize = 12.sp)
                    Text("${event.source.uppercase()} · ${format.format(Date(event.at))}", color = DSBridge.inkMute(), fontSize = 10.sp)
                } }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
