package com.nudge.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeHaptics

@Composable
fun KeypadGrid(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = remember { NudgeHaptics(context) }

    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isBack = key == "⌫"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DSBridge.surfaceVariant())
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                haptics.impactLight()
                                onKey(
                                    when (key) {
                                        "⌫" -> KeypadKey.Backspace
                                        "."  -> KeypadKey.Dot
                                        else -> KeypadKey.Digit(key)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            key,
                            fontSize = if (isBack) 18.sp else 24.sp,
                            fontWeight = if (isBack) FontWeight.Normal else FontWeight.Medium,
                            fontFamily = MonoFamily,
                            color = if (isBack) DSBridge.inkSoft() else DSBridge.ink()
                        )
                    }
                }
            }
        }
    }
}

sealed class KeypadKey {
    data class Digit(val value: String) : KeypadKey()
    data object Dot : KeypadKey()
    data object Backspace : KeypadKey()
}

fun applyKeypadInput(current: String, key: KeypadKey): String = when (key) {
    is KeypadKey.Digit -> if (current.length < 9) current + key.value else current
    KeypadKey.Dot -> if (current.contains(".")) current else current + "."
    KeypadKey.Backspace -> if (current.isNotEmpty()) current.dropLast(1) else current
}
