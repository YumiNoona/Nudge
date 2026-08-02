package com.nudge.android.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeRadius
import kotlinx.coroutines.launch

/**
 * Sync settings screen for managing E2E encrypted cross-device sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    onBack: () -> Unit,
    isSyncConfigured: Boolean = false,
    serverUrl: String = "",
    deviceId: String = "",
    pairingCode: String = "",
    onRegister: (serverUrl: String, deviceName: String) -> Unit = { _, _ -> },
    onPair: (serverUrl: String, code: String, deviceName: String) -> Unit = { _, _, _ -> },
    onSyncNow: () -> Unit = {},
    onToggleAutoSync: (Boolean) -> Unit = {},
    onDisconnect: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var localServerUrl by remember { mutableStateOf(serverUrl) }
    var deviceName by remember { mutableStateOf("") }
    var pairCode by remember { mutableStateOf("") }
    var pairDeviceName by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isPairing by remember { mutableStateOf(false) }
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Nc.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Lucide.ChevronLeft(size = 21.dp, color = Nc.inkSoft) }
            Text("Sync Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Nc.ink)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status indicator
            Card(
                shape = RoundedCornerShape(NudgeRadius.LG),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSyncConfigured)
                        Nc.positive.copy(alpha = 0.1f)
                    else
                        Nc.inkMute.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (isSyncConfigured) Nc.positive else Nc.inkMute,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                    Column {
                        Text(
                            if (isSyncConfigured) "Sync is active" else "Sync not configured",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Nc.ink
                        )
                        Text(
                            if (isSyncConfigured) "Your data syncs across devices" else "Set up sync to access your data on all devices",
                            fontSize = 12.sp,
                            color = Nc.inkSoft
                        )
                    }
                }
            }

            if (!isSyncConfigured) {
                // Setup section
                SectionHeader("Set Up Sync")

                // Server URL
                OutlinedTextField(
                    value = localServerUrl,
                    onValueChange = { localServerUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://sync.example.com:3741") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NudgeRadius.MD),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Register new device
                Card(
                    shape = RoundedCornerShape(NudgeRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = Nc.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Register New Device", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Nc.ink)
                        Text("Create a new sync identity", fontSize = 12.sp, color = Nc.inkSoft)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            label = { Text("Device name") },
                            placeholder = { Text("My Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(NudgeRadius.SM),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (localServerUrl.isNotBlank() && deviceName.isNotBlank()) {
                                    isRegistering = true
                                    scope.launch {
                                        try {
                                            onRegister(localServerUrl, deviceName)
                                            Toast.makeText(context, "Device registered!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        isRegistering = false
                                    }
                                }
                            },
                            enabled = localServerUrl.isNotBlank() && deviceName.isNotBlank() && !isRegistering,
                            shape = RoundedCornerShape(NudgeRadius.MD),
                            colors = ButtonDefaults.buttonColors(containerColor = Nc.accent)
                        ) {
                            Text(if (isRegistering) "Registering..." else "Register")
                        }
                    }
                }

                // Pair with existing device
                Card(
                    shape = RoundedCornerShape(NudgeRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = Nc.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pair with Existing Device", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Nc.ink)
                        Text("Enter a pairing code from another device", fontSize = 12.sp, color = Nc.inkSoft)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = pairCode,
                            onValueChange = { if (it.length <= 7) pairCode = it.uppercase() },
                            label = { Text("Pairing Code") },
                            placeholder = { Text("ABC-123") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(NudgeRadius.SM),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = pairDeviceName,
                            onValueChange = { pairDeviceName = it },
                            label = { Text("Device name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(NudgeRadius.SM),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (localServerUrl.isNotBlank() && pairCode.isNotBlank() && pairDeviceName.isNotBlank()) {
                                    isPairing = true
                                    scope.launch {
                                        try {
                                            onPair(localServerUrl, pairCode, pairDeviceName)
                                            Toast.makeText(context, "Paired successfully!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        isPairing = false
                                    }
                                }
                            },
                            enabled = localServerUrl.isNotBlank() && pairCode.isNotBlank() && pairDeviceName.isNotBlank() && !isPairing,
                            shape = RoundedCornerShape(NudgeRadius.MD),
                            colors = ButtonDefaults.buttonColors(containerColor = Nc.accent)
                        ) {
                            Text(if (isPairing) "Pairing..." else "Pair")
                        }
                    }
                }
            } else {
                // Configured state — show connection details and actions
                SectionHeader("Connection")

                Card(
                    shape = RoundedCornerShape(NudgeRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = Nc.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow("Server", serverUrl)
                        DetailRow("Device ID", deviceId.take(12) + "...")
                        DetailRow("Pairing Code", pairingCode)
                    }
                }

                SectionHeader("Actions")

                // Sync Now
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(NudgeRadius.MD),
                    colors = ButtonDefaults.buttonColors(containerColor = Nc.accent)
                ) {
                    Text("Sync Now", fontSize = 15.sp)
                }

                // Auto-sync toggle
                Card(
                    shape = RoundedCornerShape(NudgeRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = Nc.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-sync", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Nc.ink)
                            Text("Sync automatically in background", fontSize = 12.sp, color = Nc.inkSoft)
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = {
                                autoSyncEnabled = it
                                onToggleAutoSync(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Nc.accent,
                                checkedTrackColor = Nc.accent.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Disconnect
                SectionHeader("Danger Zone", color = Nc.negative)

                Card(
                    shape = RoundedCornerShape(NudgeRadius.LG),
                    colors = CardDefaults.cardColors(containerColor = Nc.negative.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!showDisconnectConfirm) {
                            Text("Disconnect from sync", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Nc.negative)
                            Text("Your data stays on device. You can reconnect later.", fontSize = 12.sp, color = Nc.inkSoft)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showDisconnectConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Nc.negative)
                            ) {
                                Text("Disconnect")
                            }
                        } else {
                            Text("Are you sure?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Nc.negative)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onDisconnect()
                                        showDisconnectConfirm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Nc.negative),
                                    shape = RoundedCornerShape(NudgeRadius.SM)
                                ) {
                                    Text("Yes, Disconnect")
                                }
                                OutlinedButton(
                                    onClick = { showDisconnectConfirm = false },
                                    shape = RoundedCornerShape(NudgeRadius.SM)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color = Nc.ink) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Nc.inkSoft)
        Text(value, fontSize = 13.sp, color = Nc.ink, fontFamily = MonoFamily)
    }
}
