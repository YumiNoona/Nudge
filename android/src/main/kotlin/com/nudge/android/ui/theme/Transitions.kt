package com.nudge.android.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

fun Modifier.pressScale(): Modifier = composed {
    var pressed by remember { mutableFloatStateOf(1f) }
    val scale by animateFloatAsState(
        targetValue = pressed,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "pressScale"
    )
    this
        .scale(scale)
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            pressed = 0.97f
            // Reset after a short delay — the actual click action should be handled
            // outside this modifier by the caller's onClick lambda, not here.
            // This modifier only handles the visual press state.
        }
}
