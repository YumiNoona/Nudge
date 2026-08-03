package com.nudge.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nudge.android.ui.theme.DSBridge

enum class NudgeHeroStyle { CashFlow, Analytics }

/** Distinct premium surfaces for the two primary data experiences. */
@Composable
fun NudgeHeroCard(
    modifier: Modifier = Modifier,
    style: NudgeHeroStyle,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    val accent = DSBridge.accent()
    val surface = DSBridge.surface()
    val ink = DSBridge.ink()
    val background = when (style) {
        NudgeHeroStyle.CashFlow -> Brush.linearGradient(
            listOf(Color(0xFF0D241C), Color(0xFF214638), Color(0xFF102B21)),
            start = Offset.Zero,
            end = Offset(1_050f, 760f),
        )
        NudgeHeroStyle.Analytics -> Brush.linearGradient(
            listOf(surface, DSBridge.accentBg(), surface),
            start = Offset.Zero,
            end = Offset(980f, 720f),
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (style == NudgeHeroStyle.CashFlow) 22.dp else 16.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = .22f),
                spotColor = accent.copy(alpha = if (style == NudgeHeroStyle.CashFlow) .32f else .20f),
            )
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    if (style == NudgeHeroStyle.CashFlow) {
                        listOf(Color.White.copy(alpha = .20f), accent.copy(alpha = .48f), Color.White.copy(alpha = .08f))
                    } else {
                        listOf(accent.copy(alpha = .48f), ink.copy(alpha = .08f), accent.copy(alpha = .18f))
                    },
                ),
                shape = shape,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (style == NudgeHeroStyle.CashFlow) {
                repeat(7) { index ->
                    val shift = index * 52.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = .025f),
                        start = Offset(size.width * .48f + shift, 0f),
                        end = Offset(size.width * .10f + shift, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = .20f), Color.Transparent),
                        center = Offset(size.width * .86f, size.height * .04f),
                        radius = size.minDimension * .62f,
                    ),
                    radius = size.minDimension * .62f,
                    center = Offset(size.width * .86f, size.height * .04f),
                )
            } else {
                repeat(4) { index ->
                    val x = size.width * (.72f + index * .075f)
                    drawLine(
                        color = accent.copy(alpha = .045f),
                        start = Offset(x, 0f),
                        end = Offset(x - size.height * .45f, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = if (style == NudgeHeroStyle.CashFlow) .13f else 0f), Color.Transparent),
                ),
                start = Offset(size.width * .08f, 1f),
                end = Offset(size.width * .92f, 1f),
                strokeWidth = 1.25f,
            )
        }
        content()
    }
}
