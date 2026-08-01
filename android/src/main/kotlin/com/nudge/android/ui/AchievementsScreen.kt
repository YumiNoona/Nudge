package com.nudge.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.GamificationProfileEntity
import com.nudge.android.ui.theme.MotionDuration
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.NudgeRadius
import com.nudge.android.ui.theme.SpringBouncy
import com.nudge.engine.GamificationMath
import kotlinx.coroutines.delay
import org.json.JSONArray


data class BadgeData(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val isSecret: Boolean,
    val unlockCondition: String
)

private fun allBadges(): List<BadgeData> = listOf(
    BadgeData("1", "First Blood", "Log your first transaction", "\uD83E\uDE78", false, "Log your first transaction"),
    BadgeData("2", "Detective", "Correct 10 mis-categorized transactions", "\uD83D\uDD0D", false, "Correct 10 mis-categorized transactions"),
    BadgeData("3", "Under Budget", "Finish a full month under budget in 3+ categories", "\uD83C\uDFAF", false, "Finish a full month under budget in 3+ categories"),
    BadgeData("4", "No-Spend Day", "Log a day with zero discretionary spend", "\uD83C\uDF34", false, "Log a day with zero discretionary spend"),
    BadgeData("5", "No-Spend Week", "7 consecutive no-spend days", "\uD83C\uDFDD\uFE0F", false, "7 consecutive no-spend days"),
    BadgeData("6", "Subscription Slayer", "Cancel a detected recurring subscription", "\uD83D\uDDE1\uFE0F", false, "Cancel a detected recurring subscription"),
    BadgeData("7", "Early Bird", "Review transactions before 9am, 5 days running", "\uD83C\uDF05", false, "Review transactions before 9am, 5 days running"),
    BadgeData("8", "Night Owl", "Review transactions after 10pm, 5 days running", "\uD83E\uDD89", false, "Review transactions after 10pm, 5 days running"),
    BadgeData("9", "Centurion", "Log 100 transactions total", "\uD83D\uDCAF", false, "Log 100 transactions total"),
    BadgeData("10", "Half-Millionaire", "Log 500 transactions total", "\uD83C\uDFE6", false, "Log 500 transactions total"),
    BadgeData("11", "Thousandaire", "Log 1,000 transactions total", "\uD83D\uDC51", false, "Log 1,000 transactions total"),
    BadgeData("12", "Penny Pincher", "Stay under budget in ALL categories for a month", "\uD83E\uDE99", false, "Stay under budget in ALL categories for a month"),
    BadgeData("13", "Big Spender", "Single transaction over \u20B910,000", "\uD83D\uDC8E", false, "Single transaction over \u20B910,000"),
    BadgeData("14", "Diversified", "Use 8+ different categories", "\uD83C\uDFA8", false, "Use 8+ different categories"),
    BadgeData("15", "Category Master", "Create a custom category", "\uD83D\uDCCA", false, "Create a custom category"),
    BadgeData("16", "Tag Team", "Use tags on 20+ transactions", "\uD83C\uDFF7\uFE0F", false, "Use tags on 20+ transactions"),
    BadgeData("17", "Note Taker", "Add notes to 30+ transactions", "\uD83D\uDCDD", false, "Add notes to 30+ transactions"),
    BadgeData("18", "Receipt Keeper", "Attach 10+ receipt photos", "\uD83D\uDCF8", false, "Attach 10+ receipt photos"),
    BadgeData("19", "CSV Wizard", "Import transactions via CSV", "\uD83D\uDCC4", false, "Import transactions via CSV"),
    BadgeData("20", "Backup Hero", "Complete a full data backup", "\uD83D\uDCBE", false, "Complete a full data backup"),
    BadgeData("21", "Manual Maven", "Add 50 manual transactions", "\u270D\uFE0F", false, "Add 50 manual transactions"),
    BadgeData("22", "SMS Sniper", "Have 20+ transactions auto-captured from SMS", "\uD83D\uDCF2", false, "Have 20+ transactions auto-captured from SMS"),
    BadgeData("23", "Week Streak", "7-day streak", "\uD83D\uDD25", false, "7-day streak"),
    BadgeData("24", "Month Streak", "30-day streak", "\uD83D\uDD25\uD83D\uDD25", false, "30-day streak"),
    BadgeData("25", "Century Streak", "100-day streak", "\uD83D\uDD25\uD83D\uDD25\uD83D\uDD25", false, "100-day streak"),
    BadgeData("26", "Year Streak", "365-day streak", "\uD83D\uDD25\uD83C\uDFC6", false, "365-day streak"),
    BadgeData("27", "Budget Rookie", "Set your first budget", "\uD83C\uDF31", false, "Set your first budget"),
    BadgeData("28", "Budget Pro", "Have 5+ active budgets", "\uD83D\uDCC8", false, "Have 5+ active budgets"),
    BadgeData("29", "Rollover King", "Use rollover budgets for 3 months", "\uD83D\uDD04", false, "Use rollover budgets for 3 months"),
    BadgeData("30", "Goal Getter", "Complete a savings goal", "\uD83C\uDFAF", false, "Complete a savings goal"),
    BadgeData("31", "Goal Crusher", "Complete 5 savings goals", "\uD83D\uDCAA", false, "Complete 5 savings goals"),
    BadgeData("32", "Challenge Accepted", "Complete a weekly challenge", "\uD83C\uDFC5", false, "Complete a weekly challenge"),
    BadgeData("33", "Challenge Champion", "Complete 10 challenges", "\uD83C\uDFC6", false, "Complete 10 challenges"),
    BadgeData("34", "Accountable", "Add 3+ accounts", "\uD83C\uDFE6", false, "Add 3+ accounts"),
    BadgeData("35", "Multi-Currency", "Use multiple currencies", "\uD83C\uDF0D", false, "Use multiple currencies"),
    BadgeData("36", "Dark Mode Dweller", "Use dark mode for 7 days", "\uD83C\uDF19", true, "Use dark mode for 7 days"),
    BadgeData("37", "Data Detective", "View analytics screen 10 times", "\uD83D\uDCCA", false, "View analytics screen 10 times"),
    BadgeData("38", "Envelope Master", "Use envelope budgeting for a full month", "\u2709\uFE0F", false, "Use envelope budgeting for a full month"),
    BadgeData("39", "Subscription Watcher", "Detect 3+ recurring subscriptions", "\uD83D\uDC40", false, "Detect 3+ recurring subscriptions"),
    BadgeData("40", "Clean Slate", "Review ALL pending transactions in one session", "\uD83E\uDDF9", true, "Review ALL pending transactions in one session"),
    BadgeData("41", "Weekend Warrior", "Log transactions 7 weekends in a row", "\u2694\uFE0F", false, "Log transactions 7 weekends in a row"),
    BadgeData("42", "Payday Pro", "Log income transactions in 6 different months", "\uD83D\uDCB0", false, "Log income transactions in 6 different months")
)

private fun parseUnlockedBadges(badgesJson: String): Set<String> {
    return try {
        val jsonArray = JSONArray(badgesJson)
        val set = mutableSetOf<String>()
        for (i in 0 until jsonArray.length()) {
            set.add(jsonArray.getString(i))
        }
        set
    } catch (_: Exception) {
        emptySet()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    gamificationProfile: GamificationProfileEntity?,
    onBack: () -> Unit
) {
    val badges = remember { allBadges() }
    val unlockedBadgeIds = remember(gamificationProfile) {
        gamificationProfile?.let { parseUnlockedBadges(it.badgesJson) } ?: emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Achievements",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Nc.ink
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190", fontSize = 18.sp, color = Nc.inkSoft)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Nc.background
                )
            )
        }
    ) { padding ->
        if (gamificationProfile == null) {
            AchievementsEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Nc.background)
            )
            return@Scaffold
        }

        val profile = gamificationProfile
        val unlockedCount = unlockedBadgeIds.size
        val totalCount = badges.size

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Nc.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                LevelXpHeader(profile)
            }

            item(span = { GridItemSpan(3) }) {
                BadgeCountHeader(unlockedCount, totalCount)
            }

            itemsIndexed(badges, key = { _, badge -> badge.id }) { index, badge ->
                val isUnlocked = badge.id in unlockedBadgeIds
                BadgeCard(badge = badge, isUnlocked = isUnlocked, index = index)
            }
        }
    }
}

@Composable
private fun LevelXpHeader(profile: GamificationProfileEntity) {
    val levelProgress = GamificationMath.levelProgress(profile.xpTotal)

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = Nc.accent.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "\uD83C\uDFC6",
                    fontSize = 20.sp
                )
                Text(
                    "${profile.level}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Nc.ink
                )
                Text(
                    GamificationMath.levelTitle(profile.level),
                    fontSize = 10.sp,
                    color = Nc.inkMute
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Level ${profile.level}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Nc.accent
                    )
                    Text(
                        "${profile.xpTotal} XP",
                        fontSize = 12.sp,
                        color = Nc.inkSoft
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = levelProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Nc.accent,
                    trackColor = Nc.accent.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${profile.currentStreakDays}d streak \uD83D\uDD25",
                    fontSize = 11.sp,
                    color = Nc.inkSoft
                )
            }
        }
    }
}

@Composable
private fun BadgeCountHeader(unlockedCount: Int, totalCount: Int) {
    Text(
        "$unlockedCount / $totalCount unlocked",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Nc.inkSoft
    )
}

@Composable
private fun BadgeCard(badge: BadgeData, isUnlocked: Boolean, index: Int) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 40L)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = SpringBouncy,
        label = "badgeScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(MotionDuration.STANDARD),
        label = "badgeAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(MotionDuration.STANDARD)) + scaleIn(initialScale = 0.6f, animationSpec = SpringBouncy)
    ) {
        Card(
            shape = RoundedCornerShape(NudgeRadius.LG),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked) Nc.surface else Nc.background
            ),
            border = if (isUnlocked) {
                androidx.compose.foundation.BorderStroke(1.dp, Nc.accent.copy(alpha = 0.3f))
            } else {
                null
            },
            modifier = Modifier.animateContentSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isUnlocked) {
                    Text(
                        badge.icon,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        badge.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Nc.ink,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Text(
                        badge.description,
                        fontSize = 9.sp,
                        color = Nc.inkMute,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                } else if (badge.isSecret) {
                    Text(
                        "\u2753",
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "???",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Nc.inkMute,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(NudgeRadius.SM))
                            .background(Nc.inkMute.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "\uD83D\uDD12",
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "???",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Nc.inkMute,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsEmptyState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(NudgeRadius.XL),
            colors = CardDefaults.cardColors(
                containerColor = Nc.accent.copy(alpha = 0.05f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "\uD83C\uDFC6",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No profile yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Nc.inkSoft
                )
                Text(
                    "Start tracking to earn badges!",
                    fontSize = 13.sp,
                    color = Nc.inkMute
                )
            }
        }
    }
}
