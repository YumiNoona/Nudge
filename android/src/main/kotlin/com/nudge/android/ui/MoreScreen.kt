package com.nudge.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeColors

data class SettingsSection(
    val header: String,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val id: String,
    val icon: @Composable () -> Unit,
    val label: String,
    val subtitle: String? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun MoreScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    notificationEnabled: Boolean,
    smsGranted: Boolean,
    onBack: () -> Unit,
    onNavigate: (NavScreen) -> Unit,
    onRequestSms: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val sections = remember(isDark, notificationEnabled, smsGranted) {
        listOf(
            SettingsSection("Preferences", listOf(
                SettingsItem(
                    "theme", { Lucide.Moon(size = 18.dp, strokeWidth = 1.8.dp) }, "Dark mode", if (isDark) "OLED dark" else "Light",
                    trailing = {
                        Switch(checked = isDark, onCheckedChange = onToggleTheme,
                            colors = SwitchDefaults.colors(checkedThumbColor = NudgeColors.Emerald, checkedTrackColor = NudgeColors.EmeraldBg))
                    }
                ),
                SettingsItem(
                    "permissions", { Lucide.Shield(size = 18.dp, strokeWidth = 1.8.dp) }, "Permissions",
                    subtitle = if (smsGranted || notificationEnabled) "Manage access" else "Setup required",
                    onClick = { onNavigate(NavScreen.Permissions) }
                )
            )),
            SettingsSection("Insights", listOf(
                SettingsItem("charts", { Lucide.ChartBar(size = 18.dp, strokeWidth = 1.8.dp) }, "Charts & Analytics", onClick = { onNavigate(NavScreen.Charts) }),
                SettingsItem("budgets", { Lucide.Wallet(size = 18.dp, strokeWidth = 1.8.dp) }, "Budgets", onClick = { onNavigate(NavScreen.Budget) }),
                SettingsItem("envelope", { Lucide.CreditCard(size = 18.dp, strokeWidth = 1.8.dp) }, "Envelope Budget", onClick = { onNavigate(NavScreen.Envelope) }),
                SettingsItem("achievements", { Lucide.Trophy(size = 18.dp, strokeWidth = 1.8.dp) }, "Achievements", onClick = { onNavigate(NavScreen.Achievements) }),
                SettingsItem("challenges", { Lucide.Target(size = 18.dp, strokeWidth = 1.8.dp) }, "Challenges", onClick = { onNavigate(NavScreen.Challenges) }),
                SettingsItem("goals", { Lucide.PiggyBank(size = 18.dp, strokeWidth = 1.8.dp) }, "Savings Goals", onClick = { onNavigate(NavScreen.Goals) }),
                SettingsItem("subscriptions", { Lucide.Calendar(size = 18.dp, strokeWidth = 1.8.dp) }, "Subscriptions", onClick = { onNavigate(NavScreen.Subscriptions) }),
            )),
            SettingsSection("Data & Sync", listOf(
                SettingsItem("backup", { Lucide.Database(size = 18.dp, strokeWidth = 1.8.dp) }, "Backup & Data", onClick = { onNavigate(NavScreen.Backup) }),
                SettingsItem("sync", { Lucide.RefreshCw(size = 18.dp, strokeWidth = 1.8.dp) }, "Sync Settings", onClick = { onNavigate(NavScreen.Sync) }),
            )),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Back", color = NudgeColors.InkSoft) }
            Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
            Spacer(Modifier.width(64.dp))
        }

        // Profile area
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Lucide.User(size = 24.dp, strokeWidth = 1.8.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Nudge", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NudgeColors.Ink)
                    Text("Your money, warmly understood", fontSize = 12.sp, color = NudgeColors.InkMute)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Settings sections
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { section ->
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        section.header,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.InkMute,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(section.items.size) { idx ->
                    val item = section.items[idx]
                    val isFirst = idx == 0
                    val isLast = idx == section.items.size - 1
                    val shape = when {
                        isFirst && isLast -> RoundedCornerShape(20.dp)
                        isFirst -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        isLast -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = NudgeColors.Surface),
                        modifier = Modifier.then(if (item.onClick != null) Modifier.clickable { item.onClick() } else Modifier)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    item.icon()
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(item.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NudgeColors.Ink)
                                    if (item.subtitle != null)
                                        Text(item.subtitle, fontSize = 11.sp, color = NudgeColors.InkMute)
                                }
                            }
                            if (item.trailing != null) {
                                item.trailing()
                            } else if (item.onClick != null) {
                                Text("›", fontSize = 20.sp, color = NudgeColors.InkMute)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}
