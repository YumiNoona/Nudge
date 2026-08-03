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

/** Shared layered surface for the primary data card on Transactions and Analytics. */
@Composable
fun NudgeHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    val accent = DSBridge.accent()
    val accentSurface = DSBridge.accentBg()
    val surface = DSBridge.surface()
    val ink = DSBridge.ink()

    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = .12f),
                spotColor = accent.copy(alpha = .24f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(accentSurface, surface, accentSurface),
                    start = Offset.Zero,
                    end = Offset(1_000f, 760f),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = .42f), ink.copy(alpha = .08f), accent.copy(alpha = .16f)),
                ),
                shape = shape,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = .09f),
                radius = size.minDimension * .48f,
                center = Offset(size.width * 1.03f, size.height * -.08f),
            )
            drawCircle(
                color = surface.copy(alpha = .34f),
                radius = size.minDimension * .30f,
                center = Offset(size.width * .87f, size.height * .08f),
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, ink.copy(alpha = .13f), Color.Transparent),
                ),
                start = Offset(size.width * .08f, 1f),
                end = Offset(size.width * .92f, 1f),
                strokeWidth = 1.25f,
            )
        }
        content()
    }
}
