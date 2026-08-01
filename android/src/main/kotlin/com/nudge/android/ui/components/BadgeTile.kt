package com.nudge.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.Nc

@Composable
fun BadgeTile(
    icon: String,
    label: String,
    unlocked: Boolean,
    isSecret: Boolean = false,
    modifier: Modifier = Modifier
) {
    var mounted by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (mounted) 1f else 0.8f,
        animationSpec = spring(),
        label = "badgeScale"
    )

    LaunchedEffect(Unit) {
        mounted = true
    }

    val inkMuteColor = Nc.inkMute
    val inkColor = Nc.ink

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (!unlocked) {
                    Modifier.alpha(0.35f)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Nc.surface else Color.Transparent
        ),
        elevation = if (unlocked) CardDefaults.cardElevation(defaultElevation = 1.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (!unlocked) {
                        Modifier.drawBehind {
                            val strokeWidth = 2.dp.toPx()
                            drawRoundRect(
                                color = inkMuteColor.copy(alpha = 0.30f),
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                size = Size(size.width, size.height),
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(8.dp.toPx(), 4.dp.toPx()),
                                        phase = 0f
                                    )
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        unlocked -> icon
                        isSecret -> "?"
                        else -> "🔒"
                    },
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        unlocked -> label
                        else -> "???"
                    },
                    color = if (unlocked) inkColor else inkMuteColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
