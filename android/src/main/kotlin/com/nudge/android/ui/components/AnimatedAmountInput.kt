package com.nudge.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.MonoFamily

/** Animates the typed characters without changing the actual amount or delaying keypad input. */
@Composable
fun AnimatedAmountInput(value: String, modifier: Modifier = Modifier) {
    val displayed = value.ifEmpty { "0" }
    val scrollState = rememberScrollState()
    val ink by animateColorAsState(
        targetValue = if (value.isEmpty()) DSBridge.inkMute() else DSBridge.ink(),
        animationSpec = tween(160),
        label = "amountInk",
    )

    // Keep long amounts on one line; fixed slots retain character identity on deletion.
    LaunchedEffect(value, scrollState.maxValue) {
        scrollState.animateScrollTo(scrollState.maxValue, tween(160))
    }
    Box(
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = "Amount"
                text = AnnotatedString(displayed)
            }
            .horizontalScroll(scrollState),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The keypad accepts ten digits plus a decimal separator.
            repeat(maxOf(11, displayed.length)) { index ->
                AnimatedContent(
                    targetState = displayed.getOrNull(index),
                    transitionSpec = {
                        val deleting = targetState == null
                        (slideInVertically(
                            animationSpec = tween(210, easing = FastOutSlowInEasing),
                            initialOffsetY = { if (deleting) -it / 3 else it / 2 },
                        ) + fadeIn(tween(140)) + scaleIn(
                            initialScale = .88f,
                            animationSpec = tween(210, easing = FastOutSlowInEasing),
                        )).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(150, easing = FastOutSlowInEasing),
                                targetOffsetY = { if (deleting) it / 3 else -it / 3 },
                            ) + fadeOut(tween(110)),
                        ).using(SizeTransform(clip = false) { _, _ -> tween(180) })
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "amountDigit$index",
                ) { character ->
                    if (character == null) {
                        Spacer(Modifier.width(0.dp).height(42.dp))
                    } else {
                        Text(
                            text = character.toString(),
                            fontSize = 30.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MonoFamily,
                            color = ink,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
