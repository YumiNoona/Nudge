package com.nudge.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeHaptics

@Composable
fun KeypadGrid(onKey: (KeypadKey) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptics = remember { NudgeHaptics(context) }
    val keys = listOf(
        listOf(KeypadKey.Digit("1"), KeypadKey.Digit("2"), KeypadKey.Digit("3")),
        listOf(KeypadKey.Digit("4"), KeypadKey.Digit("5"), KeypadKey.Digit("6")),
        listOf(KeypadKey.Digit("7"), KeypadKey.Digit("8"), KeypadKey.Digit("9")),
        listOf(KeypadKey.Dot, KeypadKey.Digit("0"), KeypadKey.Backspace)
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        if (pressed) .91f else 1f,
                        spring(stiffness = 700f, dampingRatio = .68f),
                        label = "keypadKeyPress"
                    )
                    Box(
                        Modifier.weight(1f).height(62.dp).scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (pressed) DSBridge.accentBg() else DSBridge.surfaceVariant())
                            .clickable(interactionSource = interaction, indication = null) {
                                haptics.impactLight()
                                onKey(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            KeypadKey.Backspace -> Lucide.Backspace(size = 22.dp, color = DSBridge.inkSoft())
                            KeypadKey.Dot -> Text(".", fontSize = 24.sp, fontWeight = FontWeight.Medium, fontFamily = MonoFamily, color = DSBridge.ink())
                            is KeypadKey.Digit -> Text(key.value, fontSize = 23.sp, fontWeight = FontWeight.Medium, fontFamily = MonoFamily, color = DSBridge.ink())
                        }
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
    is KeypadKey.Digit -> {
        val decimals = current.substringAfter('.', "")
        if (current.length >= 10 || (current.contains('.') && decimals.length >= 2)) current else current + key.value
    }
    KeypadKey.Dot -> when {
        current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }
    KeypadKey.Backspace -> current.dropLast(1)
}
