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
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors

@Composable
fun MoreScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    notificationEnabled: Boolean,
    smsGranted: Boolean,
    onBack: () -> Unit,
    onNavigate: (NavScreen) -> Unit,
    onRequestSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE) }

    var displayName by remember { mutableStateOf(prefs.getString("display_name", "You") ?: "You") }
    var photoUri by remember { mutableStateOf(prefs.getString("profile_photo_uri", null)) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUri) {
        photoBitmap = if (photoUri != null) {
            try {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) { null }
        } else null
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var nameEdit by remember { mutableStateOf(displayName) }

    val currencyCode by remember { mutableStateOf(prefs.getString("currency_code", "INR") ?: "INR") }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriStr = uri.toString()
            photoUri = uriStr
            prefs.edit().putString("profile_photo_uri", uriStr).apply()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(NudgeColors.Bone)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = NudgeColors.InkSoft)
            }
            Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
            Spacer(Modifier.width(64.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Profile card ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScalableClickBox(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NudgeColors.EmeraldBg),
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
                                    Lucide.User(size = 32.dp, strokeWidth = 1.8.dp, color = NudgeColors.Emerald)
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            ScalableClickBox(onClick = {
                                nameEdit = displayName
                                showNameDialog = true
                            }) {
                                Text(
                                    displayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NudgeColors.Ink
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Tap to edit profile",
                                fontSize = 12.sp,
                                color = NudgeColors.InkMute
                            )
                        }
                        Lucide.ChevronRight(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.InkMute)
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
                        icon = { Lucide.Tag(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.Emerald) },
                        tint = NudgeColors.Emerald,
                        label = "Currency",
                        subtitle = currencyCode,
                        trailing = {
                            Lucide.ChevronRight(size = 16.dp, strokeWidth = 1.8.dp, color = NudgeColors.InkMute)
                        },
                        onClick = { showCurrencyDialog = true }
                    )
                    Divider(color = NudgeColors.Bone, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
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
                        icon = { Lucide.Shield(size = 18.dp, strokeWidth = 1.8.dp, color = if (smsGranted) NudgeColors.Emerald else NudgeColors.Coral) },
                        tint = if (smsGranted) NudgeColors.Emerald else NudgeColors.Coral,
                        label = "SMS Access",
                        subtitle = if (smsGranted) "Granted" else "Not granted",
                        trailing = {
                            if (!smsGranted) {
                                SmallActionButton("Grant", NudgeColors.Emerald, onRequestSms)
                            } else {
                                Text("✓", fontSize = 16.sp, color = NudgeColors.Emerald)
                            }
                        }
                    )
                }
            }
            item {
                GroupedCard(top = false, bottom = true) {
                    ScalableRow(
                        icon = { Lucide.Bell(size = 18.dp, strokeWidth = 1.8.dp, color = if (notificationEnabled) NudgeColors.Emerald else NudgeColors.Coral) },
                        tint = if (notificationEnabled) NudgeColors.Emerald else NudgeColors.Coral,
                        label = "Notifications",
                        subtitle = if (notificationEnabled) "Enabled" else "Not enabled",
                        trailing = {
                            if (!notificationEnabled) {
                                SmallActionButton("Open Settings", NudgeColors.Emerald, onOpenNotificationSettings)
                            } else {
                                Text("✓", fontSize = 16.sp, color = NudgeColors.Emerald)
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
                    NavRow({ m, c, s, sw -> Lucide.Wallet(m, c, s, sw) }, NudgeColors.Emerald, "Budgets") { onNavigate(NavScreen.Budget) }
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

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // ── Dialogs ──

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Display Name", fontWeight = FontWeight.SemiBold) },
            text = {
                BasicTextField(
                    value = nameEdit,
                    onValueChange = { nameEdit = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = NudgeColors.Ink),
                    cursorBrush = SolidColor(NudgeColors.Emerald),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NudgeColors.Bone)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        displayName = nameEdit.ifBlank { "You" }
                        prefs.edit().putString("display_name", displayName).apply()
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NudgeColors.Emerald)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
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
                            color = if (isSel) NudgeColors.EmeraldBg else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                color = if (isSel) NudgeColors.Emerald else NudgeColors.Ink,
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
        color = NudgeColors.InkMute,
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
        colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface),
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
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NudgeColors.Ink)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = NudgeColors.InkMute)
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Lucide.ChevronRight(size = 16.dp, strokeWidth = 1.8.dp, color = NudgeColors.InkMute)
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
                .background(NudgeColors.Amber.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (isDark) {
                Lucide.Moon(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.Amber)
            } else {
                Lucide.Sun(size = 18.dp, strokeWidth = 1.8.dp, color = NudgeColors.Amber)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Dark mode", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NudgeColors.Ink)
            Text(if (isDark) "OLED dark" else "Light", fontSize = 11.sp, color = NudgeColors.InkMute)
        }
        Switch(
            checked = isDark,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NudgeColors.Emerald,
                checkedTrackColor = NudgeColors.EmeraldBg
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
