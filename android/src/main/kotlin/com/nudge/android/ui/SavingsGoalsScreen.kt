package com.nudge.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.data.TransactionEntity
import com.nudge.android.ui.theme.MotionDuration
import com.nudge.android.ui.theme.NudgeColors
import com.nudge.android.ui.theme.NudgeRadius
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil


data class SavingsGoalData(
    val id: String,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val visualMetaphor: VisualMetaphor,
    val targetDate: Long?,
    val monthlyContribution: Long?
)

enum class VisualMetaphor { GROWING_PLANT, FILLING_JAR, BUILDING_HOUSE, LAUNCHING_ROCKET }

private fun defaultGoals(): List<SavingsGoalData> = listOf(
    SavingsGoalData(
        id = "default_emergency",
        name = "Emergency Fund",
        targetAmount = 50_000_00L,
        currentAmount = 0L,
        visualMetaphor = VisualMetaphor.FILLING_JAR,
        targetDate = null,
        monthlyContribution = 5_000_00L
    ),
    SavingsGoalData(
        id = "default_gadget",
        name = "New Gadget",
        targetAmount = 30_000_00L,
        currentAmount = 0L,
        visualMetaphor = VisualMetaphor.LAUNCHING_ROCKET,
        targetDate = null,
        monthlyContribution = 3_000_00L
    ),
    SavingsGoalData(
        id = "default_vacation",
        name = "Vacation",
        targetAmount = 100_000_00L,
        currentAmount = 0L,
        visualMetaphor = VisualMetaphor.GROWING_PLANT,
        targetDate = null,
        monthlyContribution = 10_000_00L
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    transactions: List<TransactionEntity>,
    onBack: () -> Unit
) {
    var goals by remember { mutableStateOf(defaultGoals()) }
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Savings Goals",
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
                            "+ New Goal",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (goals.isEmpty()) {
                item {
                    GoalsEmptyState()
                }
            } else {
                itemsIndexed(goals, key = { _, goal -> goal.id }) { _, goal ->
                    SavingsGoalCard(goal = goal)
                }
            }
        }
    }

    if (showCreateSheet) {
        AddGoalSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { goal ->
                goals = goals + goal
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun SavingsGoalCard(goal: SavingsGoalData) {
    val progress = if (goal.targetAmount > 0) {
        (goal.currentAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(MotionDuration.STANDARD),
        label = "goalProgress"
    )

    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val currentFormatted = formatter.format(goal.currentAmount / 100.0)
    val targetFormatted = formatter.format(goal.targetAmount / 100.0)
    val percentText = "${(animatedProgress * 100).toInt()}%"

    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val dateText = goal.targetDate?.let { dateFormat.format(Date(it)) }

    Card(
        shape = RoundedCornerShape(NudgeRadius.LG),
        colors = CardDefaults.cardColors(
            containerColor = NudgeColors.SurfaceRaised
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                VisualMetaphorIllustration(
                    metaphor = goal.visualMetaphor,
                    progress = animatedProgress,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NudgeColors.ContentPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "\u20B9$currentFormatted of \u20B9$targetFormatted",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NudgeColors.ContentPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NudgeColors.AccentPrimary,
                        trackColor = NudgeColors.ContentTertiary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            percentText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NudgeColors.AccentPrimary
                        )
                        if (dateText != null) {
                            Text(
                                "by $dateText",
                                fontSize = 11.sp,
                                color = NudgeColors.ContentTertiary
                            )
                        }
                        if (goal.monthlyContribution != null && goal.monthlyContribution > 0) {
                            Text(
                                "\u20B9${formatter.format(goal.monthlyContribution / 100)}/mo",
                                fontSize = 11.sp,
                                color = NudgeColors.ContentTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualMetaphorIllustration(
    metaphor: VisualMetaphor,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val size = 80.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(NudgeRadius.MD))
            .background(NudgeColors.AccentPrimary.copy(alpha = 0.06f))
    ) {
        when (metaphor) {
            VisualMetaphor.GROWING_PLANT -> PlantIllustration(progress)
            VisualMetaphor.FILLING_JAR -> JarIllustration(progress)
            VisualMetaphor.BUILDING_HOUSE -> HouseIllustration(progress)
            VisualMetaphor.LAUNCHING_ROCKET -> RocketIllustration(progress)
        }
    }
}

@Composable
private fun PlantIllustration(progress: Float) {
    val stemHeight = 12f + progress * 36f
    val leafCount = ceil(progress * 5).toInt().coerceAtLeast(0)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val baseY = size.height * 0.85f
        val stemTopY = baseY - stemHeight

        // Pot
        drawRect(
            color = Color(0xFF8B7355),
            topLeft = Offset(centerX - 12f, baseY),
            size = Size(24f, 10f)
        )
        drawRect(
            color = Color(0xFFA0845C),
            topLeft = Offset(centerX - 10f, baseY - 3f),
            size = Size(20f, 5f)
        )

        // Stem
        drawLine(
            color = Color(0xFF4CAF50),
            start = Offset(centerX, baseY),
            end = Offset(centerX, stemTopY),
            strokeWidth = 3.5f
        )

        // Leaves
        for (i in 0 until leafCount) {
            val leafY = baseY - 10f - i * 8f
            val side = if (i % 2 == 0) 1f else -1f
            drawOval(
                color = Color(0xFF66BB6A),
                topLeft = Offset(centerX + side * 6f - 5f, leafY - 4f),
                size = Size(10f, 8f)
            )
        }

        // Top bud/flower
        if (progress >= 0.9f) {
            drawCircle(
                color = Color(0xFFFF7043),
                radius = 5f,
                center = Offset(centerX, stemTopY)
            )
        }
    }
}

@Composable
private fun JarIllustration(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val jarLeft = size.width * 0.22f
        val jarRight = size.width * 0.78f
        val jarBottom = size.height * 0.85f
        val jarTop = size.height * 0.18f
        val jarHeight = jarBottom - jarTop
        val jarWidth = jarRight - jarLeft

        // Liquid fill
        val fillHeight = jarHeight * progress
        drawRect(
            color = NudgeColors.AccentPrimary.copy(alpha = 0.5f),
            topLeft = Offset(jarLeft, jarBottom - fillHeight),
            size = Size(jarWidth, fillHeight)
        )

        // Liquid surface line
        if (progress > 0f) {
            drawLine(
                color = NudgeColors.AccentPrimary.copy(alpha = 0.7f),
                start = Offset(jarLeft, jarBottom - fillHeight),
                end = Offset(jarRight, jarBottom - fillHeight),
                strokeWidth = 2f
            )
        }

        // Jar outline
        val jarPath = Path().apply {
            moveTo(jarLeft + 5f, jarBottom)
            lineTo(jarLeft + 2f, jarTop + 4f)
            lineTo(jarRight - 2f, jarTop + 4f)
            lineTo(jarRight - 5f, jarBottom)
        }
        drawPath(
            path = jarPath,
            color = NudgeColors.ContentSecondary.copy(alpha = 0.4f),
            style = Stroke(width = 2f)
        )

        // Jar lid
        drawRect(
            color = NudgeColors.ContentSecondary.copy(alpha = 0.3f),
            topLeft = Offset(jarLeft, jarTop),
            size = Size(jarWidth, 5f)
        )

        // Label line
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(jarLeft + 4f, jarTop + 18f),
            end = Offset(jarRight - 4f, jarTop + 22f),
            strokeWidth = 1.5f
        )
    }
}

@Composable
private fun HouseIllustration(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val baseY = size.height * 0.85f
        val houseWidth = 40f
        val houseLeft = centerX - houseWidth / 2
        val houseRight = centerX + houseWidth / 2

        // Build stages: foundation → walls → roof
        val stage = (progress * 3).coerceIn(0f, 3f)

        // Foundation
        if (stage >= 0.3f) {
            drawRect(
                color = Color(0xFF6D4C41),
                topLeft = Offset(houseLeft - 3f, baseY - 4f),
                size = Size(houseWidth + 6f, 6f)
            )
        }

        // Walls
        val wallHeight = if (stage >= 1f) {
            (stage - 1f).coerceIn(0f, 1f) * 28f + if (stage >= 1.5f) 4f else 0f
        } else {
            0f
        }
        if (wallHeight > 0f) {
            drawRect(
                color = Color(0xFFFFCC80),
                topLeft = Offset(houseLeft, baseY - 4f - wallHeight),
                size = Size(houseWidth, wallHeight)
            )
        }

        // Door
        if (stage >= 1.5f) {
            drawRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(centerX - 5f, baseY - 14f),
                size = Size(10f, 14f)
            )
        }

        // Roof
        val roofProgress = (stage - 2f).coerceIn(0f, 1f)
        if (roofProgress > 0f) {
            val roofPath = Path().apply {
                moveTo(centerX, baseY - 36f)
                lineTo(houseLeft - 4f, baseY - 4f)
                lineTo(houseRight + 4f, baseY - 4f)
                close()
            }
            drawPath(
                path = roofPath,
                color = Color(0xFFE57373)
            )
        }

        // Ground line
        drawLine(
            color = Color(0xFFBDBDBD),
            start = Offset(houseLeft - 10f, baseY),
            end = Offset(houseRight + 10f, baseY),
            strokeWidth = 1.5f
        )
    }
}

@Composable
private fun RocketIllustration(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val baseY = size.height * 0.8f
        val maxTravel = size.height * 0.55f
        val rocketY = baseY - maxTravel * progress

        // Launch pad
        drawLine(
            color = Color(0xFFBDBDBD),
            start = Offset(centerX - 18f, baseY),
            end = Offset(centerX + 18f, baseY),
            strokeWidth = 2f
        )

        // Rocket body
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(centerX - 7f, rocketY),
            size = Size(14f, 24f)
        )

        // Rocket nose cone
        val nosePath = Path().apply {
            moveTo(centerX, rocketY - 2f)
            lineTo(centerX - 7f, rocketY + 6f)
            lineTo(centerX + 7f, rocketY + 6f)
            close()
        }
        drawPath(
            path = nosePath,
            color = Color(0xFFEF5350)
        )

        // Window
        drawCircle(
            color = Color(0xFF81D4FA),
            radius = 3.5f,
            center = Offset(centerX, rocketY + 12f)
        )

        // Fins
        drawRect(
            color = Color(0xFFEF5350),
            topLeft = Offset(centerX - 10f, rocketY + 18f),
            size = Size(5f, 7f)
        )
        drawRect(
            color = Color(0xFFEF5350),
            topLeft = Offset(centerX + 5f, rocketY + 18f),
            size = Size(5f, 7f)
        )

        // Flame (only when progress > 0 and < 1)
        if (progress > 0f && progress < 1f) {
            val flameScale = (1f - progress).coerceIn(0.3f, 1f) * 8f
            val flamePath = Path().apply {
                moveTo(centerX, rocketY + 24f)
                lineTo(centerX - 5f, rocketY + 24f + flameScale)
                lineTo(centerX + 5f, rocketY + 24f + flameScale)
                close()
            }
            drawPath(
                path = flamePath,
                color = Color(0xFFFF9800).copy(alpha = 0.8f)
            )
        }

        // Stars / particles
        if (progress > 0.3f) {
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.6f),
                radius = 2f,
                center = Offset(centerX - 14f, rocketY + 5f)
            )
        }
    }
}

@Composable
private fun GoalsEmptyState() {
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
                "\uD83C\uDF31",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No savings goals yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NudgeColors.ContentSecondary
            )
            Text(
                "Tap \"+ New Goal\" to set your first savings target",
                fontSize = 13.sp,
                color = NudgeColors.ContentTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    onDismiss: () -> Unit,
    onCreate: (SavingsGoalData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedMetaphor by remember { mutableStateOf(VisualMetaphor.FILLING_JAR) }
    var targetDateEpoch by remember { mutableStateOf<Long?>(null) }
    var monthlyContributionStr by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountInCents = amountStr.filter { it.isDigit() }.toLongOrNull()?.times(100L)
    val monthlyCents = monthlyContributionStr.filter { it.isDigit() }.toLongOrNull()?.times(100L)
    val isValid = name.isNotBlank() && amountInCents != null && amountInCents > 0

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val metaphors = listOf(
        VisualMetaphor.GROWING_PLANT to "\uD83C\uDF31",
        VisualMetaphor.FILLING_JAR to "\uD83C\uDFFA",
        VisualMetaphor.BUILDING_HOUSE to "\uD83C\uDFE0",
        VisualMetaphor.LAUNCHING_ROCKET to "\uD83D\uDE80"
    )

    val metaphorLabels = mapOf(
        VisualMetaphor.GROWING_PLANT to "Plant",
        VisualMetaphor.FILLING_JAR to "Jar",
        VisualMetaphor.BUILDING_HOUSE to "House",
        VisualMetaphor.LAUNCHING_ROCKET to "Rocket"
    )

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
                "Add Savings Goal",
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
                placeholder = { Text("e.g. Emergency Fund", fontSize = 14.sp, color = NudgeColors.ContentTertiary) },
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
                "Target Amount",
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
                    "\u20B9",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.AccentPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
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
                        if (amountStr.isEmpty()) {
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Visual Style",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metaphors.forEach { (metaphor, emoji) ->
                    val isSelected = selectedMetaphor == metaphor
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMetaphor = metaphor },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    metaphorLabels[metaphor] ?: "",
                                    fontSize = 12.sp
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeColors.AccentPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = NudgeColors.AccentPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Target Date (optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NudgeColors.ContentSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NudgeColors.ContentTertiary.copy(alpha = 0.3f))
                ) {
                    Text(
                        if (targetDateEpoch != null) dateFormat.format(Date(targetDateEpoch!!)) else "No target date",
                        fontSize = 15.sp,
                        color = if (targetDateEpoch != null) NudgeColors.ContentPrimary else NudgeColors.ContentTertiary
                    )
                }

                if (targetDateEpoch != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { targetDateEpoch = null }) {
                        Text("Clear", fontSize = 13.sp, color = NudgeColors.ContentTertiary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Monthly Contribution (optional)",
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
                    "\u20B9",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NudgeColors.AccentPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = monthlyContributionStr,
                    onValueChange = { monthlyContributionStr = it },
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
                        if (monthlyContributionStr.isEmpty()) {
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
                    val newGoal = SavingsGoalData(
                        id = "goal_${System.currentTimeMillis()}",
                        name = name,
                        targetAmount = amountInCents ?: return@Button,
                        currentAmount = 0L,
                        visualMetaphor = selectedMetaphor,
                        targetDate = targetDateEpoch,
                        monthlyContribution = monthlyCents
                    )
                    onCreate(newGoal)
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
                    "Create Goal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDateEpoch ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        targetDateEpoch = millis
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = NudgeColors.AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = NudgeColors.ContentSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
