package com.nudge.android.ui

import android.Manifest
import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nudge.android.BuildConfig
import com.nudge.android.service.ExpenseReminderWorker
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
    onSavedMessages: () -> Unit,
    onDonate: () -> Unit,
    onAppTour: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateStatus: String?,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    var saveMessages by remember { mutableStateOf(prefs.getBoolean("save_transaction_messages", false)) }
    var hideWidgetAmounts by remember { mutableStateOf(prefs.getBoolean("hide_widget_amounts", false)) }
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean(ExpenseReminderWorker.PREF_ENABLED, false)) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(prefs.getString("display_name", "You") ?: "You") }
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(name) }
    var photoPath by remember { mutableStateOf(prefs.getString("profile_photo_path", null)) }
    val profileBitmap = remember(photoPath) { photoPath?.let(BitmapFactory::decodeFile) }
    val reminderPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        remindersEnabled = granted
        ExpenseReminderWorker.setEnabled(context, granted)
        if (!granted) Toast.makeText(context, "Notification permission is needed for reminders", Toast.LENGTH_SHORT).show()
    }
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
        Box(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) { Lucide.ChevronLeft(size = 22.dp, color = DSBridge.inkSoft()) }
            Text("Settings", style = DSTypography.headlineLarge, color = DSBridge.ink(), modifier = Modifier.align(Alignment.Center))
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
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { nameDraft = name; editName = true }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(name, fontWeight = FontWeight.SemiBold, color = DSBridge.ink())
                    }
                    IconButton(onClick = { nameDraft = name; editName = true }) { Lucide.Edit(size = 18.dp, color = DSBridge.inkMute()) }
                }
            }

            Text("AUTOMATIC CAPTURE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = DSBridge.accentBg()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(DSBridge.accent().copy(alpha = .12f)), contentAlignment = Alignment.Center) { Lucide.Sparkles(size = 18.dp, color = DSBridge.accent()) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Log transactions automatically", color = DSBridge.ink(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Processed only on this device", color = DSBridge.inkMute(), fontSize = 9.sp) }
                        CompactSwitch(checked = captureEnabled, onCheckedChange = {
                            onCaptureChanged(it)
                            scope.launch { NudgeWidget().updateAll(context) }
                        })
                    }
                    if (scanState != null) { Spacer(Modifier.height(8.dp)); Text(scanState, fontFamily = MonoFamily, fontSize = 9.sp, color = DSBridge.accent()) }
                }
            }
            Text("REMINDERS", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Bell(size = 18.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Expense reminders", color = DSBridge.ink(), fontSize = 13.sp)
                        Text("Gentle daily check-ins", fontSize = 9.sp, color = DSBridge.inkMute())
                    }
                    CompactSwitch(checked = remindersEnabled, onCheckedChange = { enabled ->
                        if (!enabled) {
                            remindersEnabled = false
                            ExpenseReminderWorker.setEnabled(context, false)
                        } else if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            reminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            remindersEnabled = true
                            ExpenseReminderWorker.setEnabled(context, true)
                        }
                    })
                }
            }

            SettingsGroup {
                SettingsRow({ m, c, s, sw -> Lucide.Bell(m, c, s, sw) }, "Notification access", if (notificationEnabled) "Enabled" else "Tap to enable", notificationEnabled, onNotificationSettings)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.FileText(m, c, s, sw) }, "SMS access", if (smsGranted) "Granted" else "Tap to grant", smsGranted, if (smsGranted) onScanSms else onRequestSms)
                if (smsGranted) {
                    HorizontalDivider(color = DSBridge.background())
                    SettingsRow({ m, c, s, sw -> Lucide.RefreshCw(m, c, s, sw) }, "Scan message history", "All SMS & MMS", true, onScanSms)
                }
            }

            Text("TRANSACTION MESSAGE SAVING", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Message(size = 18.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Save transaction messages", color = DSBridge.ink(), fontSize = 13.sp)
                        Text("Auto sources", fontSize = 9.sp, color = DSBridge.inkMute())
                    }
                    CompactSwitch(checked = saveMessages, onCheckedChange = {
                        saveMessages = it
                        prefs.edit().putBoolean("save_transaction_messages", it).apply()
                    })
                }
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.Database(m, c, s, sw) }, "Saved messages", "Manage sources", true, onSavedMessages)
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
                SettingsRow({ m, c, s, sw -> Lucide.LayoutDashboard(m, c, s, sw) }, "Home-screen widgets", "3 widget sizes", true) {
                    val manager = AppWidgetManager.getInstance(context)
                    if (manager.isRequestPinAppWidgetSupported) {
                        manager.requestPinAppWidget(ComponentName(context, NudgeWidgetReceiver::class.java), null, null)
                    } else {
                        Toast.makeText(context, "Open your launcher widgets and choose Nudge", Toast.LENGTH_LONG).show()
                    }
                }
                HorizontalDivider(color = DSBridge.background())
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lucide.Shield(size = 18.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hide widget amounts", color = DSBridge.ink(), fontSize = 13.sp)
                        Text("Keep launcher totals private", fontSize = 9.sp, color = DSBridge.inkMute())
                    }
                    CompactSwitch(checked = hideWidgetAmounts, onCheckedChange = {
                        hideWidgetAmounts = it
                        prefs.edit().putBoolean("hide_widget_amounts", it).apply()
                        scope.launch { NudgeWidget().updateAll(context) }
                    })
                }
            }

            Text("PRIVACY & APPEARANCE", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                Row(Modifier.fillMaxWidth().clickable(onClick = onToggleTheme).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isDark) Lucide.Moon(size = 18.dp, color = DSBridge.accent()) else Lucide.Sun(size = 18.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Theme", color = DSBridge.ink()); Text(if (isDark) "Dark" else "Light", fontSize = 10.sp, color = DSBridge.inkMute()) }
                }
            }
            Text("Nudge parses locally. Source bodies are Keystore-encrypted when saving is enabled and are excluded from normal backup exports.", fontSize = 10.sp, lineHeight = 15.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(8.dp))

            Text("ABOUT", fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = DSBridge.inkMute(), modifier = Modifier.padding(top = 10.dp))
            SettingsGroup {
                SettingsRow({ m, c, s, sw -> Lucide.Info(m, c, s, sw) }, "App tour", "Replay the 7-step guide", true, onAppTour)
                HorizontalDivider(color = DSBridge.background())
                SettingsRow(
                    { m, c, s, sw -> Lucide.RefreshCw(m, c, s, sw) },
                    "Check for updates",
                    updateStatus ?: "Version ${BuildConfig.VERSION_NAME}",
                    true,
                    onCheckUpdates,
                )
                HorizontalDivider(color = DSBridge.background())
                SettingsRow({ m, c, s, sw -> Lucide.QrCode(m, c, s, sw) }, "Support Nudge", "Donate with any UPI app", true, onDonate)
            }
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
                    prefs.edit().remove("profile_photo_path").apply()
                }) { Lucide.Trash2(size = 17.dp, color = DS.Negative); Spacer(Modifier.width(7.dp)); Text("Remove photo", color = DS.Negative) }
            }
        },
        confirmButton = {
            Button(onClick = {
                name = nameDraft.trim().ifBlank { "You" }
                prefs.edit().putString("display_name", name).apply()
                editName = false
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } }
    )
}

@Composable private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) = Surface(
    shape = RoundedCornerShape(18.dp),
    color = DSBridge.surface(),
    border = androidx.compose.foundation.BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .08f)),
) { Column(content = content) }

@Composable
private fun SettingsRow(icon: @Composable (Modifier, Color, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit, title: String, subtitle: String, positive: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        icon(Modifier, if (positive) DSBridge.accent() else DS.Warning, 18.dp, 1.8.dp)
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = DSBridge.ink(), fontSize = 13.sp); Text(subtitle, fontSize = 9.sp, color = DSBridge.inkMute(), maxLines = 2) }
        Lucide.ChevronRight(size = 16.dp, color = DSBridge.inkMute())
    }
}

@Composable
private fun CompactSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumbOffset by animateDpAsState(if (checked) 23.dp else 3.dp, label = "settingsSwitch")
    Box(
        Modifier.width(46.dp).height(26.dp).clip(CircleShape)
            .background(if (checked) DSBridge.accent() else DSBridge.inkMute().copy(alpha = .24f))
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier.offset { IntOffset(thumbOffset.roundToPx(), 3.dp.roundToPx()) }.size(20.dp).clip(CircleShape)
                .background(if (checked) MaterialTheme.colorScheme.onPrimary else DSBridge.surface()),
        )
    }
}
