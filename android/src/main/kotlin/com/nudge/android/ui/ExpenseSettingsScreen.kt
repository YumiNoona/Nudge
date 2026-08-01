package com.nudge.android.ui

import android.content.Context
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.*

@Composable
fun ExpenseSettingsScreen(
    isDark: Boolean,
    captureEnabled: Boolean,
    notificationEnabled: Boolean,
    smsGranted: Boolean,
    scanState: String?,
    onBack: () -> Unit,
    onToggleTheme: () -> Unit,
    onCaptureChanged: (Boolean) -> Unit,
    onRequestSms: () -> Unit,
    onNotificationSettings: () -> Unit,
    onScanSms: () -> Unit,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onBackup: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    var retainExcerpt by remember { mutableStateOf(prefs.getBoolean("retain_source_excerpt", false)) }
    var name by remember { mutableStateOf(prefs.getString("display_name", "You") ?: "You") }
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(name) }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft()) }
            Column(Modifier.weight(1f)) {
                Text("Settings", style = DSTypography.headlineLarge, color = DSBridge.ink())
                Text("Capture, organize and protect", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = { nameDraft = name; editName = true }, shape = RoundedCornerShape(22.dp), color = DSBridge.surface()) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(DSBridge.accentBg()), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = DSBridge.accent())
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.SemiBold, color = DSBridge.ink()); Text("Local profile", fontSize = 11.sp, color = DSBridge.inkMute()) }
                    Lucide.ChevronRight(size = 18.dp, color = DSBridge.inkMute())
                }
            }

            Text("AUTOMATIC CAPTURE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = DS.AccentDeep) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(DS.Signal.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Lucide.Sparkles(size = 20.dp, color = DS.Signal) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Log transactions automatically", color = Color.White, fontWeight = FontWeight.SemiBold); Text("Processed only on this device", color = Color.White.copy(alpha = .55f), fontSize = 11.sp) }
                        Switch(checked = captureEnabled, onCheckedChange = onCaptureChanged, colors = SwitchDefaults.colors(checkedThumbColor = DS.InkPrimary, checkedTrackColor = DS.Signal))
                    }
                    if (scanState != null) { Spacer(Modifier.height(10.dp)); Text(scanState, fontFamily = MonoFamily, fontSize = 10.sp, color = DS.Signal) }
                }
            }
            SettingsGroup {
                SettingsRow({ m, c, s, sw -> Lucide.Bell(m, c, s, sw) }, "Notification access", if (notificationEnabled) "Enabled" else "Tap to enable", notificationEnabled, onNotificationSettings)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.FileText(m, c, s, sw) }, "SMS access", if (smsGranted) "Granted" else "Tap to grant", smsGranted, if (smsGranted) onScanSms else onRequestSms)
                if (smsGranted) {
                    HorizontalDivider(color = DSBridge.background())
                    SettingsRow({ m, c, s, sw -> Lucide.RefreshCw(m, c, s, sw) }, "Scan recent messages", "Import up to 500 financial messages", true, onScanSms)
                }
            }

            Text("ORGANIZATION", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                SettingsRow({ m, c, s, sw -> Lucide.Wallet(m, c, s, sw) }, "Accounts", "Cards, UPI and cash sources", true, onAccounts)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.Tag(m, c, s, sw) }, "Categories", "Organize expenses your way", true, onCategories)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.Database(m, c, s, sw) }, "Backup & data", "Export, import or delete", true, onBackup)
            }

            Text("PRIVACY & APPEARANCE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Shield(size = 20.dp, color = DSBridge.accent()); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Keep source excerpts", color = DSBridge.ink()); Text("Off is most private; dedup still works", fontSize = 10.sp, color = DSBridge.inkMute()) }
                    Switch(checked = retainExcerpt, onCheckedChange = { retainExcerpt = it; prefs.edit().putBoolean("retain_source_excerpt", it).apply() })
                }
                HorizontalDivider(color = DSBridge.background())
                Row(Modifier.fillMaxWidth().clickable(onClick = onToggleTheme).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isDark) Lucide.Moon(size = 20.dp, color = DSBridge.accent()) else Lucide.Sun(size = 20.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Theme", color = DSBridge.ink()); Text(if (isDark) "Dark" else "Light", fontSize = 10.sp, color = DSBridge.inkMute()) }; Lucide.ChevronRight(size = 17.dp, color = DSBridge.inkMute())
                }
            }
            Text("Nudge parses locally. Unrelated messages are ignored and source text is not retained unless you enable it above.", fontSize = 10.sp, lineHeight = 15.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(8.dp))
            Spacer(Modifier.height(30.dp))
        }
    }

    if (editName) AlertDialog(onDismissRequest = { editName = false }, title = { Text("Display name") }, text = { OutlinedTextField(nameDraft, { nameDraft = it.take(32) }, singleLine = true) }, confirmButton = { Button(onClick = { name = nameDraft.ifBlank { "You" }; prefs.edit().putString("display_name", name).commit(); editName = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } })
}

@Composable private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) = Surface(shape = RoundedCornerShape(20.dp), color = DSBridge.surface()) { Column(content = content) }

@Composable
private fun SettingsRow(icon: @Composable (Modifier, Color, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit, title: String, subtitle: String, positive: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        icon(Modifier, if (positive) DSBridge.accent() else DS.Warning, 20.dp, 1.8.dp)
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = DSBridge.ink()); Text(subtitle, fontSize = 10.sp, color = DSBridge.inkMute()) }
        Lucide.ChevronRight(size = 17.dp, color = DSBridge.inkMute())
    }
}
