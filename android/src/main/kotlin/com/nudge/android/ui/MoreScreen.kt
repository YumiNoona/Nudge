package com.nudge.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.DSIconChip
import com.nudge.android.ui.theme.DSSpace
import com.nudge.android.ui.theme.DSTypography
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.NudgeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun MoreScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    notificationEnabled: Boolean,
    smsGranted: Boolean,
    onNavigate: (NavScreen) -> Unit,
    onRequestSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(prefs.getString("display_name", "You") ?: "You") }
    var photoPath by remember { mutableStateOf(prefs.getString("profile_photo_path", null)) }
    var photoUri by remember { mutableStateOf(prefs.getString("profile_photo_uri", null)) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoPath, photoUri) {
        photoBitmap = if (photoPath != null) {
            BitmapFactory.decodeFile(photoPath)
        } else if (photoUri != null) {
            try {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) { null }
        } else null
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var nameEdit by remember { mutableStateOf(displayName) }

    val currencyCode by remember { mutableStateOf(prefs.getString("currency_code", "INR") ?: "INR") }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    runCatching {
                        val destination = File(context.filesDir, "profile-avatar.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destination).use { output -> input.copyTo(output) }
                        } ?: error("Unable to read selected image")
                        destination.absolutePath
                    }.getOrNull()
                }
                if (savedPath != null) {
                    photoPath = savedPath
                    photoUri = null
                    photoBitmap = BitmapFactory.decodeFile(savedPath)
                    prefs.edit()
                        .putString("profile_photo_path", savedPath)
                        .remove("profile_photo_uri")
                        .commit()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Nc.background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = DSSpace.lg, vertical = DSSpace.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", style = DSTypography.headlineLarge, color = Nc.ink)
            Spacer(Modifier.weight(1f))
            DSIconChip(
                { Lucide.Settings(size = 18.dp, strokeWidth = 1.8.dp, color = Nc.accent) },
                size = 40.dp
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Profile card ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        nameEdit = displayName
                        showProfileDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Nc.surface)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Nc.accentBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap!!.asImageBitmap(),
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Lucide.User(size = 32.dp, strokeWidth = 1.8.dp, color = Nc.accent)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Nc.ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Tap to edit profile",
                                fontSize = 12.sp,
                                color = Nc.inkMute
                            )
                        }
                        Lucide.ChevronRight(size = 18.dp, strokeWidth = 1.8.dp, color = Nc.inkMute)
                    }
                }
            }

            // ── Preferences section ──
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Preferences")
            }
            item {
                GroupedCard(top = true, bottom = true) {
                    // Currency
                    ScalableRow(
                        icon = { Lucide.Tag(size = 18.dp, strokeWidth = 1.8.dp, color = Nc.accent) },
                        tint = Nc.accent,
                        label = "Currency",
                        subtitle = currencyCode,
                        trailing = {
                            Lucide.ChevronRight(size = 16.dp, strokeWidth = 1.8.dp, color = Nc.inkMute)
                        },
                        onClick = { showCurrencyDialog = true }
                    )
                    Divider(color = Nc.background, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    // Dark mode
                    DarkModeRow(isDark = isDark, onToggle = onToggleTheme)
                }
            }

            // ── Data section ──
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Data")
            }
            item {
                GroupedCard(top = true, bottom = false) {
                    ScalableRow(
                        icon = { Lucide.Wallet(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.CatBlue) },
                        tint = NudgeColors.CatBlue,
                        label = "Manage Accounts",
                        onClick = onNavigateToAccounts
                    )
                }
            }
            item {
                GroupedCard(top = false, bottom = false) {
                    ScalableRow(
                        icon = { Lucide.Database(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.CatTeal) },
                        tint = NudgeColors.CatTeal,
                        label = "Backup & Data",
                        onClick = { onNavigate(NavScreen.Backup) }
                    )
                }
            }
            item {
                GroupedCard(top = false, bottom = true) {
                    ScalableRow(
                        icon = { Lucide.RefreshCw(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.CatViolet) },
                        tint = NudgeColors.CatViolet,
                        label = "Sync Settings",
                        onClick = { onNavigate(NavScreen.Sync) }
                    )
                }
            }

            // ── Permissions section ──
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Permissions")
            }
            item {
                GroupedCard(top = true, bottom = false) {
                    ScalableRow(
                        icon = { Lucide.Shield(size = 18.dp, strokeWidth = 1.8.dp, color = if (smsGranted) Nc.accent else Nc.negative) },
                        tint = if (smsGranted) Nc.accent else Nc.negative,
                        label = "SMS Access",
                        subtitle = if (smsGranted) "Granted" else "Not granted",
                        trailing = {
                            if (!smsGranted) {
                                SmallActionButton("Grant", Nc.accent, onRequestSms)
                            } else {
                                Text("✓", fontSize = 16.sp, color = Nc.accent)
                            }
                        }
                    )
                }
            }
            item {
                GroupedCard(top = false, bottom = true) {
                    ScalableRow(
                        icon = { Lucide.Bell(size = 18.dp, strokeWidth = 1.8.dp, color = if (notificationEnabled) Nc.accent else Nc.negative) },
                        tint = if (notificationEnabled) Nc.accent else Nc.negative,
                        label = "Notifications",
                        subtitle = if (notificationEnabled) "Enabled" else "Not enabled",
                        trailing = {
                            if (!notificationEnabled) {
                                SmallActionButton("Open Settings", Nc.accent, onOpenNotificationSettings)
                            } else {
                                Text("✓", fontSize = 16.sp, color = Nc.accent)
                            }
                        }
                    )
                }
            }

            // ── Insights section ──
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Insights")
            }
            item {
                GroupedCard(top = true, bottom = false) {
                    NavRow({ m, c, s, sw -> Lucide.Wallet(m, c, s, sw) }, Nc.accent, "Budgets") { onNavigate(NavScreen.Budget) }
                }
            }
            item {
                GroupedCard(top = false, bottom = false) {
                    NavRow({ m, c, s, sw -> Lucide.CreditCard(m, c, s, sw) }, NudgeColors.CatBlue, "Envelope Budget") { onNavigate(NavScreen.Envelope) }
                }
            }
            item {
                GroupedCard(top = false, bottom = false) {
                    NavRow({ m, c, s, sw -> Lucide.ChartBar(m, c, s, sw) }, NudgeColors.CatTeal, "Charts & Analytics") { onNavigate(NavScreen.Charts) }
                }
            }
            item {
                GroupedCard(top = false, bottom = false) {
                    NavRow({ m, c, s, sw -> Lucide.PiggyBank(m, c, s, sw) }, NudgeColors.CatOrange, "Savings Goals") { onNavigate(NavScreen.Goals) }
                }
            }
            item {
                GroupedCard(top = false, bottom = false) {
                    NavRow({ m, c, s, sw -> Lucide.Flame(m, c, s, sw) }, NudgeColors.CatPink, "Challenges") { onNavigate(NavScreen.Challenges) }
                }
            }
            item {
                GroupedCard(top = false, bottom = false) {
                    NavRow({ m, c, s, sw -> Lucide.Trophy(m, c, s, sw) }, NudgeColors.CatViolet, "Achievements") { onNavigate(NavScreen.Achievements) }
                }
            }
            item {
                GroupedCard(top = false, bottom = true) {
                    NavRow({ m, c, s, sw -> Lucide.Calendar(m, c, s, sw) }, NudgeColors.CatRose, "Subscriptions") { onNavigate(NavScreen.Subscriptions) }
                }
            }

            item { Spacer(Modifier.height(130.dp)) }
        }
    }

    // ── Dialogs ──

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit profile", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(Nc.accentBg)
                            .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap!!.asImageBitmap(),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else Lucide.User(size = 38.dp, color = Nc.accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Tap photo to change", fontSize = 11.sp, color = Nc.inkMute)
                    Spacer(Modifier.height(18.dp))
                    BasicTextField(
                        value = nameEdit,
                        onValueChange = { nameEdit = it.take(32) },
                        textStyle = TextStyle(fontSize = 15.sp, color = Nc.ink),
                        cursorBrush = SolidColor(Nc.accent),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Nc.background).padding(horizontal = 14.dp, vertical = 13.dp),
                        decorationBox = { field ->
                            if (nameEdit.isBlank()) Text("Your name", color = Nc.inkMute)
                            field()
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        displayName = nameEdit.ifBlank { "You" }
                        prefs.edit().putString("display_name", displayName).commit()
                        showProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Nc.accent)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showCurrencyDialog) {
        val currencies = listOf(
            "INR" to "₹  Indian Rupee",
            "USD" to "$   US Dollar",
            "EUR" to "€   Euro",
            "GBP" to "£   British Pound",
            "JPY" to "¥   Japanese Yen",
            "AUD" to "A$  Australian Dollar",
            "CAD" to "C$  Canadian Dollar",
            "SGD" to "S$  Singapore Dollar"
        )
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Currency", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    currencies.forEach { (code, label) ->
                        val isSel = code == currencyCode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    prefs.edit().putString("currency_code", code).apply()
                                    showCurrencyDialog = false
                                },
                            color = if (isSel) Nc.accentBg else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                color = if (isSel) Nc.accent else Nc.ink,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Reusable components ──

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Nc.inkMute,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun GroupedCard(top: Boolean, bottom: Boolean, content: @Composable () -> Unit) {
    val shape = when {
        top && bottom -> RoundedCornerShape(16.dp)
        top -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        bottom -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(0.dp)
    }
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Nc.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun ScalableClickBox(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "press")

    Box(
        modifier = Modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        content()
    }
}

@Composable
private fun ScalableRow(
    icon: @Composable () -> Unit,
    tint: Color,
    label: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "press")

    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Nc.ink)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = Nc.inkMute)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Lucide.ChevronRight(size = 16.dp, strokeWidth = 1.8.dp, color = Nc.inkMute)
        }
    }
}

@Composable
private fun NavRow(
    iconFn: @Composable (modifier: Modifier, color: Color, size: androidx.compose.ui.unit.Dp, strokeWidth: androidx.compose.ui.unit.Dp) -> Unit,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    ScalableRow(
        icon = { iconFn(Modifier, tint, 18.dp, 1.8.dp) },
        tint = tint,
        label = label,
        onClick = onClick
    )
}

@Composable
private fun DarkModeRow(isDark: Boolean, onToggle: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "press")

    Row(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = { onToggle(!isDark) })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Nc.warning.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (isDark) {
                Lucide.Moon(size = 18.dp, strokeWidth = 1.8.dp, color = Nc.warning)
            } else {
                Lucide.Sun(size = 18.dp, strokeWidth = 1.8.dp, color = Nc.warning)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Dark mode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Nc.ink)
            Text(if (isDark) "OLED dark" else "Light", fontSize = 11.sp, color = Nc.inkMute)
        }
        Switch(
            checked = isDark,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Nc.accent,
                checkedTrackColor = Nc.accentBg
            )
        )
    }
}

@Composable
private fun SmallActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
