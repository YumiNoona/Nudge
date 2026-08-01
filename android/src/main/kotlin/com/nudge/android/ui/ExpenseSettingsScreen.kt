package com.nudge.android.ui

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.*
import com.nudge.android.widget.NudgeWidgetReceiver
import com.nudge.android.widget.NudgeWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import java.io.File

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
    onBackup: () -> Unit,
    onSavedMessages: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    var saveMessages by remember { mutableStateOf(prefs.getBoolean("save_transaction_messages", false)) }
    var hideWidgetAmounts by remember { mutableStateOf(prefs.getBoolean("hide_widget_amounts", false)) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(prefs.getString("display_name", "You") ?: "You") }
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(name) }
    var photoPath by remember { mutableStateOf(prefs.getString("profile_photo_path", null)) }
    val profileBitmap = remember(photoPath) { photoPath?.let(BitmapFactory::decodeFile) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val profileDir = File(context.filesDir, "profile").apply { mkdirs() }
                val destination = File(profileDir, "avatar.jpg")
                val original = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                    ?: error("Unable to decode selected image")
                val maxSide = maxOf(original.width, original.height)
                val scale = (512f / maxSide).coerceAtMost(1f)
                val resized = if (scale < 1f) Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                ) else original
                destination.outputStream().use { resized.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                if (resized !== original) resized.recycle()
                original.recycle()
                photoPath = destination.absolutePath
                prefs.edit().putString("profile_photo_path", destination.absolutePath).commit()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(DSBridge.background()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft()) }
            Column(Modifier.weight(1f)) {
                Text("Settings", style = DSTypography.headlineLarge, color = DSBridge.ink())
                Text("Capture, organize and protect", style = DSTypography.bodySmall, color = DSBridge.inkMute())
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = DSBridge.surface()) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(DSBridge.accentBg())
                            .clickable { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) Image(profileBitmap.asImageBitmap(), "Profile photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = DSBridge.accent())
                        Box(Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(DS.Signal), contentAlignment = Alignment.Center) {
                            Lucide.Camera(size = 12.dp, color = DS.InkPrimary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).clickable { nameDraft = name; editName = true }.padding(vertical = 6.dp)) {
                        Text(name, fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
                        Text("Tap name to edit · photo stays on device", fontSize = 10.sp, color = DSBridge.inkMute())
                    }
                    IconButton(onClick = { nameDraft = name; editName = true }) { Lucide.Edit(size = 18.dp, color = DSBridge.inkMute()) }
                }
            }

            Text("AUTOMATIC CAPTURE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = DS.AccentDeep) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(DS.Signal.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Lucide.Sparkles(size = 20.dp, color = DS.Signal) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Log transactions automatically", color = Color.White, fontWeight = FontWeight.SemiBold); Text("Processed only on this device", color = Color.White.copy(alpha = .55f), fontSize = 11.sp) }
                        Switch(checked = captureEnabled, onCheckedChange = {
                            onCaptureChanged(it)
                            scope.launch { NudgeWidget().updateAll(context) }
                        }, colors = SwitchDefaults.colors(checkedThumbColor = DS.InkPrimary, checkedTrackColor = DS.Signal))
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

            Text("TRANSACTION MESSAGE SAVING", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Message(size = 20.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Save transaction messages", color = DSBridge.ink())
                        Text("Only messages that create automatic transactions", fontSize = 10.sp, color = DSBridge.inkMute())
                    }
                    Switch(checked = saveMessages, onCheckedChange = {
                        saveMessages = it
                        prefs.edit().putBoolean("save_transaction_messages", it).apply()
                    })
                }
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.Database(m, c, s, sw) }, "Saved messages", "View, search, retain or delete encrypted sources", true, onSavedMessages)
            }
            Text(
                "When enabled, Nudge privately saves messages that create automatic transactions. Unrelated messages are never saved. This affects future captures.",
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = DSBridge.inkMute(),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Text("ORGANIZATION", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                SettingsRow({ m, c, s, sw -> Lucide.Wallet(m, c, s, sw) }, "Accounts", "Cards, UPI and cash sources", true, onAccounts)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.Tag(m, c, s, sw) }, "Categories", "Organize expenses your way", true, onCategories)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.Database(m, c, s, sw) }, "Backup & data", "Export, import or delete", true, onBackup)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.LayoutDashboard(m, c, s, sw) }, "Home-screen widget", "Spend, review status and quick add", true) {
                    val manager = AppWidgetManager.getInstance(context)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(ComponentName(context, NudgeWidgetReceiver::class.java), null, null)
                    } else {
                        Toast.makeText(context, "Open your launcher widgets and choose Nudge", Toast.LENGTH_LONG).show()
                    }
                }
                HorizontalDivider(color = DSBridge.background())
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Shield(size = 20.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hide widget amounts", color = DSBridge.ink())
                        Text("Keep balances private on your launcher", fontSize = 10.sp, color = DSBridge.inkMute())
                    }
                    Switch(checked = hideWidgetAmounts, onCheckedChange = {
                        hideWidgetAmounts = it
                        prefs.edit().putBoolean("hide_widget_amounts", it).apply()
                        scope.launch { NudgeWidget().updateAll(context) }
                    })
                }
            }

            Text("PRIVACY & APPEARANCE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                Row(Modifier.fillMaxWidth().clickable(onClick = onToggleTheme).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isDark) Lucide.Moon(size = 20.dp, color = DSBridge.accent()) else Lucide.Sun(size = 20.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Theme", color = DSBridge.ink()); Text(if (isDark) "Dark" else "Light", fontSize = 10.sp, color = DSBridge.inkMute()) }; Lucide.ChevronRight(size = 17.dp, color = DSBridge.inkMute())
                }
            }
            Text("Nudge parses locally. Source bodies are Keystore-encrypted when saving is enabled and are excluded from normal backup exports.", fontSize = 10.sp, lineHeight = 15.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(8.dp))
            Spacer(Modifier.height(30.dp))
        }
    }

    if (editName) AlertDialog(
        onDismissRequest = { editName = false },
        title = { Text("Local profile") },
        text = {
            Column {
                OutlinedTextField(nameDraft, { nameDraft = it.take(32) }, label = { Text("Display name") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { photoPicker.launch("image/*") }) { Lucide.Image(size = 17.dp); Spacer(Modifier.width(7.dp)); Text("Choose profile photo") }
                if (photoPath != null) TextButton(onClick = {
                    runCatching { File(photoPath!!).delete() }
                    photoPath = null
                    prefs.edit().remove("profile_photo_path").commit()
                }) { Lucide.Trash2(size = 17.dp, color = DS.Negative); Spacer(Modifier.width(7.dp)); Text("Remove photo", color = DS.Negative) }
            }
        },
        confirmButton = {
            Button(onClick = {
                name = nameDraft.trim().ifBlank { "You" }
                prefs.edit().putString("display_name", name).commit()
                editName = false
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } }
    )
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
