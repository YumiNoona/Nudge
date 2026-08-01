package com.nudge.android.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reusable animation composables implementing the §7.3 motion spec.
 *
 * Principles:
 * - Standard duration: 200-300ms for small UI, 400-600ms for celebrations
 * - Spring physics (damping ~0.8, stiffness ~300) for touch-driven interactions
 * - Respect reduced-motion preference where possible
 */

// --- Spring specs matching §7.3 ---

val SpringBouncy = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

val SpringDefault = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val SpringStiff = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 600f
)

object MotionDuration {
    const val QUICK = 150
    const val STANDARD = 250
    const val CELEBRATION = 500
}

// ================================================
// 1. Amount digit count-up / roll animation
// §7.3: "Amount digit count-up/roll animation + card slide-in from bottom with slight overshoot spring"
// ================================================

@Composable
fun AmountCountUp(
    targetAmount: Long, // in cents
    modifier: Modifier = Modifier,
    durationMs: Int = MotionDuration.CELEBRATION,
    prefix: String = "₹",
    fontSize: Int = 36,
    color: Color = Nc.ink,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetAmount / 100f,
        animationSpec = tween(
            durationMillis = durationMs,
            easing = FastOutSlowInEasing
        ),
        label = "amountCountUp"
    )

    val formatted = remember(animatedValue) {
        val whole = animatedValue.toInt()
        val decimal = ((animatedValue - whole) * 100).roundToInt()
        "%,d.%02d".format(whole, decimal)
    }

    Text(
        text = "$prefix$formatted",
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        fontFamily = MonoFamily,
        color = color,
        modifier = modifier
    )
}

// ================================================
// 2. Card slide-in from bottom with overshoot spring
// §7.3 adds: "card slide-in from bottom with slight overshoot spring"
// ================================================

@Composable
fun CardSlideIn(
    visible: Boolean,
    modifier: Modifier = Modifier,
    initialOffsetY: Dp = 100.dp,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
        ) + fadeIn(animationSpec = tween(MotionDuration.STANDARD)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(MotionDuration.QUICK)
        ) + fadeOut(animationSpec = tween(MotionDuration.QUICK)),
        modifier = modifier
    ) {
        content()
    }
}

// ================================================
// 3. Budget ring animated arc
// §7.3: "Ring arc animates via eased path interpolation, not a snap;
//        color shifts smoothly from accent → warning → negative as it fills"
// ================================================

@Composable
fun AnimatedBudgetRing(
    progress: Float, // 0.0 - 1.5 (overspend allowed)
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 4.dp,
    isDark: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1.5f),
        animationSpec = tween(
            durationMillis = MotionDuration.STANDARD,
            easing = FastOutSlowInEasing
        ),
        label = "budgetRing"
    )

    // Color interpolation based on progress
    val ringColor by animateColorAsState(
        targetValue = when {
            animatedProgress > 1f -> Nc.negative
            animatedProgress > 0.8f -> Nc.warning
            else -> Nc.accent
        },
        animationSpec = tween(MotionDuration.STANDARD),
        label = "ringColor"
    )

    val trackColor = if (isDark) {
        Nc.inkMute.copy(alpha = 0.2f)
    } else {
        Nc.inkMute.copy(alpha = 0.2f)
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        CircularProgressIndicator(
            progress = animatedProgress.coerceAtMost(1f),
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            strokeWidth = strokeWidth,
            trackColor = trackColor
        )
        Text(
            "${(animatedProgress * 100).toInt()}%",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Nc.inkSoft
        )
    }
}

// ================================================
// 4. Level-Up full-screen celebration animation
// §7.3: "Full-screen radial burst, confetti physics-simulated, number count-up"
// ================================================

@Composable
fun LevelUpCelebration(
    newLevel: Int,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    // Auto-dismiss after celebration duration
    LaunchedEffect(isVisible) {
        delay(2500L)
        onDismiss()
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "celebrationScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(MotionDuration.CELEBRATION),
        label = "celebrationAlpha"
    )

    // Radial burst background
    val burstScale by animateFloatAsState(
        targetValue = if (isVisible) 2.5f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "burst"
    )

    // Confetti particles — simplified representation
    val particleCount = 20
    val particles = remember { List(particleCount) { it } }

    val title = when {
        newLevel <= 5 -> "Budget Rookie"
        newLevel <= 10 -> "Coin Collector"
        newLevel <= 18 -> "Saving Scout"
        newLevel <= 28 -> "Spending Sensei"
        newLevel <= 40 -> "Finance Ninja"
        else -> "Wealth Wizard"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha * 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // Radial burst
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(burstScale)
                .alpha(alpha * 0.3f)
                .background(Nc.accent, shape = CircleShape)
        )

        // Confetti particles
        particles.forEach { i ->
            val angle = i * (360f / particleCount).toDouble()
            val distance = (150 + (i % 3) * 50).toDouble()
            val particleAlpha = (alpha * (0.5f + (i % 3) * 0.15f)).coerceIn(0f, 1f)

            val particleColor = listOf(
                Nc.accent,
                Nc.positive,
                Nc.warning,
                Nc.accentDeep,
                Nc.negative
            )[i % 5]

            Box(
                modifier = Modifier
                    .offset(
                        x = (kotlin.math.cos(angle) * distance * burstScale / 2.5).dp,
                        y = (kotlin.math.sin(angle) * distance * burstScale / 2.5).dp
                    )
                    .size((6 + i % 4 * 2).dp)
                    .alpha(particleAlpha)
                    .background(particleColor, shape = CircleShape)
            )
        }

        // Level-up content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale)
        ) {
            Text("⚡", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "LEVEL UP!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Level $newLevel",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MonoFamily,
                color = Nc.accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 18.sp,
                color = Nc.inkSoft
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onDismiss) {
                Text("Nice! 🎉", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// ================================================
// 5. Delete swipe animation
// §7.3: "Swipe-to-delete with elastic resistance past threshold,
//        item collapses height smoothly (not just fades)"
// ================================================

@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteThreshold: Float = 0.5f, // fraction of item width to trigger delete
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isRemoved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val alpha by animateFloatAsState(
        targetValue = if (isRemoved) 0f else 1f,
        animationSpec = tween(MotionDuration.STANDARD),
        label = "deleteAlpha"
    )

    val height by animateDpAsState(
        targetValue = if (isRemoved) 0.dp else Dp.Unspecified,
        animationSpec = tween(MotionDuration.STANDARD),
        label = "deleteHeight"
    )

    // Delete background that reveals as you swipe
    val deleteBgAlpha = (abs(offsetX) / 200f).coerceIn(0f, 1f)
    val hasCrossedThreshold = abs(offsetX) > 150f

    Box(modifier = modifier.then(if (isRemoved) Modifier.height(height) else Modifier)) {
        // Red delete background
        if (deleteBgAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(deleteBgAlpha)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Nc.negative.copy(alpha = 0.15f))
            )
        }

        Box(
            modifier = Modifier
                .alpha(alpha)
                .offset(x = offsetX.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (hasCrossedThreshold) {
                                isRemoved = true
                                scope.launch {
                                    delay(MotionDuration.STANDARD.toLong())
                                    onDelete()
                                }
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            // Use horizontal for swipe-to-delete
                            offsetX = (offsetX + dragAmount).coerceIn(-200f, 0f)
                        }
                    )
                }
        ) {
            content()
        }
    }
}

// ================================================
// 6. Pull-to-refresh custom animation
// §7.3: "Custom coin/piggy-bank icon that fills or spins rather than generic spinner"
// ================================================

@Composable
fun NudgePullRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) rotation else pullProgress * 360f,
        animationSpec = tween(MotionDuration.QUICK),
        label = "pullRotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isRefreshing) 1.1f else pullProgress.coerceIn(0f, 1f),
        animationSpec = SpringBouncy,
        label = "pullScale"
    )

    val coinColor = if (isRefreshing) Nc.accent else Nc.inkMute

    Text(
        text = "🪙",
        fontSize = (24 * scale).sp,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotationAngle
                scaleX = scale
                scaleY = scale
            }
    )
}

// ================================================
// 7. Streak flame idle micro-animation
// §7.3: "Idle: subtle scale-breathing loop (2s cycle, 2% scale). On increment: flame flares briefly"
// ================================================

@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier,
    isBreathing: Boolean = true
) {
    val infiniteBreath = rememberInfiniteTransition(label = "flameBreath")
    val breathScale by infiniteBreath.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val scale = if (isBreathing) breathScale else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale)
    ) {
        Text("🔥", fontSize = 20.sp)
        Text(
            "${streakDays}d",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Nc.ink
        )
    }
}

// ================================================
// 8. Shake animation for error states
// §7.3: "Gentle horizontal shake (3px amplitude, 2 cycles), never harsh red flash"
// ================================================

@Composable
fun ShakeOnError(
    isError: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shakeOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isError) {
        if (isError) {
            // Two cycles of shake, 3px amplitude
            val shakeSequence = listOf(0f, -3f, 3f, -3f, 3f, 0f)
            for (offset in shakeSequence) {
                shakeOffset = offset
                delay(40L)
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer { translationX = shakeOffset }
    ) {
        content()
    }
}

// ================================================
// 9. Badge unlock shine-sweep animation
// §7.3: "Card flip reveal + shine-sweep gradient pass"
// ================================================

@Composable
fun BadgeUnlockAnimation(
    badgeIcon: String,
    badgeName: String,
    isUnlocking: Boolean,
    modifier: Modifier = Modifier
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isUnlocking) 0f else 90f,
        animationSpec = tween(
            durationMillis = MotionDuration.CELEBRATION,
            easing = FastOutSlowInEasing
        ),
        label = "badgeRotate"
    )

    // Shine sweep effect using a sliding gradient
    val shineOffset by animateFloatAsState(
        targetValue = if (isUnlocking) 1.5f else -1.5f,
        animationSpec = tween(
            durationMillis = 600,
            easing = LinearEasing
        ),
        label = "shineSweep"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Nc.accent.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Text(badgeIcon, fontSize = 36.sp)
            Text(
                badgeName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Nc.accent
            )
        }
    }
}

// ================================================
// 10. Accessibility — Reduced Motion support
// §7.3: "Respect prefers-reduced-motion / Android's Remove animations"
// ================================================

object ReducedMotion {
    // In production, this would check Android's animation scale settings
    var isEnabled = false

    fun duration(normalMs: Int): Int {
        return if (isEnabled) 0 else normalMs
    }

    fun <T> animationSpec(normalSpec: AnimationSpec<T>): AnimationSpec<T> {
        return if (isEnabled) snap() else normalSpec
    }
}
