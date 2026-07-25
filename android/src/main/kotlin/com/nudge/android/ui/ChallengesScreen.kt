package com.nudge.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.CategoryEntity
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.MotionDuration
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.engine.GamificationMath

private object NudgeRadius {
    const val SM = 8
    const val MD = 14
    const val LG = 20
    const val XL = 28
}

data class ChallengeData(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val targetProgress: Int,
    val currentProgress: Int,
    val rewardXp: Long,
    val categoryId: String?,
    val isCompleted: Boolean,
    val isCustom: Boolean
)

private fun generateChallenges(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>
): List<ChallengeData> {
    val now = System.currentTimeMillis()
    val dayMs = 86400000L
    val weekMs = dayMs * 7

    val thisWeekStart = now - (now % dayMs) - dayMs * 6
    val lastWeekStart = thisWeekStart - weekMs

    val thisWeekTxns = transactions.filter { it.timestampEpoch >= thisWeekStart }
    val lastWeekTxns = transactions.filter {
        it.timestampEpoch in lastWeekStart until thisWeekStart
    }
    val debitTxns = transactions.filter { it.type == "debit" }
    val creditTxns = transactions.filter { it.type == "credit" }

    val challenges = mutableListOf<ChallengeData>()

    // 1. Category with most spending last week → reduce challenge
    val categorySpending = mutableMapOf<String, Long>()
    lastWeekTxns.filter { it.type == "debit" }.forEach { txn ->
        val catId = txn.categoryId ?: "uncategorized"
        categorySpending[catId] = (categorySpending[catId] ?: 0L) + txn.amountCents
    }
    val topCategoryEntry = categorySpending.maxByOrNull { it.value }
    if (topCategoryEntry != null && topCategoryEntry.value > 0) {
        val cat = categories.find { it.id == topCategoryEntry.key }
        val catName = cat?.name ?: "this category"
        val target = (topCategoryEntry.value * 0.85).toLong()
        val thisWeekSpent = thisWeekTxns.filter { it.categoryId == topCategoryEntry.key && it.type == "debit" }
            .sumOf { it.amountCents }
        challenges.add(
            ChallengeData(
                id = "auto_reduce_category",
                name = "Cut Back on $catName",
                description = "Spend 15% less on $catName this week",
                emoji = "\uD83D\uDCC9",
                targetProgress = (target / 100).toInt(),
                currentProgress = (thisWeekSpent / 100).toInt().coerceAtMost((target / 100).toInt()),
                rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
                categoryId = topCategoryEntry.key,
                isCompleted = thisWeekSpent <= target && thisWeekTxns.isNotEmpty(),
                isCustom = false
            )
        )
    }

    // 2. No-spend days challenge
    val daysWithSpend = thisWeekTxns.map {
        (it.timestampEpoch - (it.timestampEpoch % dayMs)) / dayMs
    }.distinct()
    val totalDaysThisWeek = ((now - thisWeekStart) / dayMs).toInt().coerceAtLeast(1)
    val noSpendDaysThisWeek = totalDaysThisWeek - daysWithSpend.size
    challenges.add(
        ChallengeData(
            id = "auto_nospend",
            name = "No-Spend Days",
            description = "Have 3 no-spend days this week",
            emoji = "\uD83C\uDF34",
            targetProgress = 3,
            currentProgress = noSpendDaysThisWeek.coerceAtMost(3),
            rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
            categoryId = null,
            isCompleted = noSpendDaysThisWeek >= 3,
            isCustom = false
        )
    )

    // 3. Spending trend — keep under average
    val lastWeekTotal = lastWeekTxns.filter { it.type == "debit" }.sumOf { it.amountCents }
    if (lastWeekTotal > 0) {
        val targetTotal = (lastWeekTotal * 0.9).toLong()
        val thisWeekTotal = thisWeekTxns.filter { it.type == "debit" }.sumOf { it.amountCents }
        challenges.add(
            ChallengeData(
                id = "auto_keep_under",
                name = "Stay Under Budget",
                description = "Keep total spend under \u20B9${targetTotal / 100} this week",
                emoji = "\uD83D\uDCB0",
                targetProgress = (targetTotal / 100).toInt(),
                currentProgress = (thisWeekTotal / 100).toInt().coerceAtMost((targetTotal / 100).toInt()),
                rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
                categoryId = null,
                isCompleted = thisWeekTotal <= targetTotal,
                isCustom = false
            )
        )
    }

    // 4. Same-day logging (7 days)
    challenges.add(
        ChallengeData(
            id = "auto_sameday",
            name = "Same-Day Logger",
            description = "Log expenses same-day for 7 days straight",
            emoji = "\u23F0",
            targetProgress = 7,
            currentProgress = daysWithSpend.size.coerceAtMost(7),
            rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
            categoryId = null,
            isCompleted = daysWithSpend.size >= 7,
            isCustom = false
        )
    )

    // 5. Subscription review
    val hasSubscriptionCategory = categories.any { it.name.lowercase().contains("subscription") }
    if (hasSubscriptionCategory) {
        challenges.add(
            ChallengeData(
                id = "auto_review_subs",
                name = "Subscription Review",
                description = "Review all your subscriptions this week",
                emoji = "\uD83D\uDD0D",
                targetProgress = 1,
                currentProgress = 0,
                rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
                categoryId = null,
                isCompleted = false,
                isCustom = false
            )
        )
    }

    // 6. Log income
    val hasIncomeThisWeek = thisWeekTxns.any { it.type == "credit" }
    challenges.add(
        ChallengeData(
            id = "auto_log_income",
            name = "Track Your Income",
            description = "Log income at least once this week",
            emoji = "\uD83D\uDCB5",
            targetProgress = 1,
            currentProgress = if (hasIncomeThisWeek) 1 else 0,
            rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
            categoryId = null,
            isCompleted = hasIncomeThisWeek,
            isCustom = false
        )
    )

    // 7. Daily check-in (always included)
    challenges.add(
        ChallengeData(
            id = "auto_daily_checkin",
            name = "Daily Check-In",
            description = "Open the app and check in daily for 7 days",
            emoji = "\uD83D\uDD25",
            targetProgress = 7,
            currentProgress = daysWithSpend.size.coerceAtMost(7),
            rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
            categoryId = null,
            isCompleted = false,
            isCustom = false
        )
    )

    // 8. Unused category challenge
    val usedCategoryIds = transactions.map { it.categoryId }.filterNotNull().toSet()
    val unusedCategory = categories.firstOrNull { it.id !in usedCategoryIds }
    if (unusedCategory != null) {
        challenges.add(
            ChallengeData(
                id = "auto_try_category",
                name = "Explore ${unusedCategory.name}",
                description = "Try using the ${unusedCategory.name} category this week",
                emoji = unusedCategory.icon ?: "\uD83D\uDCC1",
                targetProgress = 1,
                currentProgress = 0,
                rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
                categoryId = unusedCategory.id,
                isCompleted = false,
                isCustom = false
            )
        )
    }

    return challenges
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit
) {
    val autoChallenges = remember(categories, transactions) {
        generateChallenges(categories, transactions)
    }
    var customChallenges by remember { mutableStateOf<List<ChallengeData>>(emptyList()) }
    var showCreateSheet by remember { mutableStateOf(false) }

    val allChallenges = remember(autoChallenges, customChallenges) {
        autoChallenges + customChallenges
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Challenges",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NudgeColors.ContentPrimary
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("\u2190", fontSize = 18.sp, color = NudgeColors.ContentSecondary)
                    }
                },
                actions = {
                    TextButton(onClick = { showCreateSheet = true }) {
                        Text(
                            "+ Custom",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NudgeColors.AccentPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NudgeColors.SurfaceBase
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NudgeColors.SurfaceBase),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "This Week's Challenges",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NudgeColors.ContentPrimary
                )
            }

            if (allChallenges.isEmpty()) {
                item {
                    ChallengesEmptyState()
                }
            } else {
                itemsIndexed(allChallenges, key = { _, ch -> ch.id }) { _, challenge ->
                    ChallengeCard(challenge = challenge)
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateChallengeSheet(
            categories = categories,
            onDismiss = { showCreateSheet = false },
            onCreate = { challenge ->
                customChallenges = customChallenges + challenge
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun ChallengeCard(challenge: ChallengeData) {
    val progress = if (challenge.targetProgress > 0) {
        (challenge.currentProgress.toFloat() / challenge.targetProgress.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(MotionDuration.STANDARD),
        label = "challengeProgress"
    )

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isCompleted) {
                NudgeColors.Positive.copy(alpha = 0.08f)
            } else {
                NudgeColors.SurfaceRaised
            }
        ),
        border = if (challenge.isCompleted) {
            androidx.compose.foundation.BorderStroke(1.dp, NudgeColors.Positive.copy(alpha = 0.4f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (challenge.isCompleted) NudgeColors.Positive.copy(alpha = 0.15f)
                        else NudgeColors.AccentPrimary.copy(alpha = 0.1f)
                    )
            ) {
                if (challenge.isCompleted) {
                    Text("\u2705", fontSize = 22.sp)
                } else {
                    Text(challenge.emoji, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    challenge.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (challenge.isCompleted) NudgeColors.ContentSecondary else NudgeColors.ContentPrimary,
                    textDecoration = if (challenge.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    challenge.description,
                    fontSize = 12.sp,
                    color = NudgeColors.ContentTertiary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (challenge.isCompleted) NudgeColors.Positive else NudgeColors.AccentPrimary,
                        trackColor = NudgeColors.ContentTertiary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${challenge.currentProgress}/${challenge.targetProgress}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NudgeColors.ContentSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(NudgeRadius.SM),
                color = NudgeColors.AccentPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    "+${challenge.rewardXp} XP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.AccentPrimary
                )
            }
        }
    }
}

@Composable
private fun ChallengesEmptyState() {
    Card(
        shape = RoundedCornerShape(NudgeRadius.XL),
        colors = CardDefaults.cardColors(
            containerColor = NudgeColors.AccentPrimary.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\uD83C\uDFC5",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No challenges yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NudgeColors.ContentSecondary
            )
            Text(
                "Add some transactions to generate challenges",
                fontSize = 13.sp,
                color = NudgeColors.ContentTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChallengeSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onCreate: (ChallengeData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var targetStr by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val targetNumber = targetStr.filter { it.isDigit() }.toIntOrNull()
    val isValid = name.isNotBlank() && targetNumber != null && targetNumber > 0

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = NudgeColors.SurfaceBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NudgeColors.ContentTertiary)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Create Custom Challenge",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NudgeColors.ContentPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Name",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Save more this month", fontSize = 14.sp, color = NudgeColors.ContentTertiary) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = NudgeColors.ContentTertiary.copy(alpha = 0.3f),
                    focusedBorderColor = NudgeColors.AccentPrimary,
                    focusedTextColor = NudgeColors.ContentPrimary,
                    unfocusedTextColor = NudgeColors.ContentPrimary
                ),
                textStyle = TextStyle(fontSize = 15.sp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Description",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Reduce eating out", fontSize = 14.sp, color = NudgeColors.ContentTertiary) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = NudgeColors.ContentTertiary.copy(alpha = 0.3f),
                    focusedBorderColor = NudgeColors.AccentPrimary,
                    focusedTextColor = NudgeColors.ContentPrimary,
                    unfocusedTextColor = NudgeColors.ContentPrimary
                ),
                textStyle = TextStyle(fontSize = 15.sp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Category (optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedCategory != null) "${selectedCategory!!.icon ?: "\uD83D\uDCC1"} ${selectedCategory!!.name}" else "None",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = NudgeColors.ContentTertiary.copy(alpha = 0.3f),
                        focusedBorderColor = NudgeColors.AccentPrimary,
                        focusedTextColor = NudgeColors.ContentPrimary,
                        unfocusedTextColor = NudgeColors.ContentPrimary
                    ),
                    textStyle = TextStyle(fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (all categories)", color = NudgeColors.ContentSecondary) },
                        onClick = {
                            selectedCategory = null
                            categoryDropdownExpanded = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text("${category.icon ?: "\uD83D\uDCC1"} ${category.name}")
                            },
                            onClick = {
                                selectedCategory = category
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Target",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NudgeColors.SurfaceRaised)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    "\uD83C\uDFAF",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.ContentPrimary,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(NudgeColors.AccentPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (targetStr.isEmpty()) {
                            Text(
                                "0",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NudgeColors.ContentTertiary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val newChallenge = ChallengeData(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name,
                        description = description.ifBlank { name },
                        emoji = "\u2B50",
                        targetProgress = targetNumber ?: 1,
                        currentProgress = 0,
                        rewardXp = GamificationMath.XP_CHALLENGE_COMPLETE,
                        categoryId = selectedCategory?.id,
                        isCompleted = false,
                        isCustom = true
                    )
                    onCreate(newChallenge)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NudgeColors.AccentPrimary,
                    disabledContainerColor = NudgeColors.ContentTertiary.copy(alpha = 0.3f)
                ),
                enabled = isValid
            ) {
                Text(
                    "Create Challenge",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
