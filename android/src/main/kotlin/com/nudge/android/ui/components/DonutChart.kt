package com.nudge.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.NudgeColors

data class DonutSegment(
    val label: String,
    val fraction: Float,
    val color: Color
)

@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    centerLabel: String,
    centerSubtext: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val animatedFractions = segments.map { segment ->
        animateFloatAsState(
            targetValue = segment.fraction.coerceIn(0f, 1f),
            animationSpec = tween(600),
            label = "donutFraction"
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = size.toPx() * 0.18f
                val halfStroke = strokeWidth / 2f
                val canvasSize = this.size
                val arcSize = Size(
                    canvasSize.width - halfStroke * 2,
                    canvasSize.height - halfStroke * 2
                )
                val topLeft = Offset(halfStroke, halfStroke)

                var currentAngle = -90f
                segments.forEachIndexed { index, segment ->
                    val sweep = animatedFractions[index].value * 360f
                    drawArc(
                        color = segment.color,
                        startAngle = currentAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    currentAngle += sweep
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = centerLabel,
                    color = NudgeColors.Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (centerSubtext != null) {
                    Text(
                        text = centerSubtext,
                        color = NudgeColors.InkMute,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, segment ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(segment.color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = segment.label,
                    color = NudgeColors.InkSoft,
                    fontSize = 10.sp
                )
                if (index < segments.lastIndex) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}
