package com.nudge.android.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Card with proper shadow + rounded corners ──

@Composable
fun DSCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val mod = modifier
        .shadow(10.dp, RoundedCornerShape(DSRadius.card), spotColor = Color(0xFF1F1E24).copy(alpha = 0.06f))
        .clip(RoundedCornerShape(DSRadius.card))
        .background(DSBridge.surface())
    Box(
        modifier = if (onClick != null) mod.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() } else mod
    ) { content() }
}

// ── Card with tinted accent shadow ──

@Composable
fun DSHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val mod = modifier
        .shadow(16.dp, RoundedCornerShape(DSRadius.card), spotColor = DSBridge.accent().copy(alpha = 0.3f))
        .clip(RoundedCornerShape(DSRadius.card))
        .background(DSBridge.accent())
    Box(
        modifier = if (onClick != null) mod.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() } else mod
    ) { content() }
}

// ── Small icon in a tinted circle ──

@Composable
fun DSIconChip(
    icon: @Composable () -> Unit,
    tint: Color = DSBridge.accent(),
    bg: Color = DSBridge.accentBg(),
    size: Dp = 40.dp,
    radius: Dp = DSRadius.md,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(radius)).background(bg),
        contentAlignment = Alignment.Center
    ) { icon() }
}

// ── Text styles with DS typography ──

@Composable
fun DSTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = DSTypography.titleLarge, color = DSBridge.ink(), modifier = modifier)
}

@Composable
fun DSBody(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Text(text, style = DSTypography.bodyMedium, color = if (muted) DSBridge.inkMute() else DSBridge.ink(), modifier = modifier)
}

@Composable
fun DSCaption(text: String, modifier: Modifier = Modifier, muted: Boolean = true) {
    Text(text, style = DSTypography.labelSmall, color = DSBridge.inkMute(), modifier = modifier)
}

@Composable
fun DSMoney(text: String, modifier: Modifier = Modifier, color: Color = DSBridge.ink(), large: Boolean = false) {
    Text(text, style = if (large) DSTypography.displayMedium else DSTypography.displaySmall,
        color = color, modifier = modifier)
}

// ── Section header ──

@Composable
fun DSSectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = DSSpace.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = DSTypography.titleMedium, color = DSBridge.inkSoft())
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, style = DSTypography.labelMedium, color = DSBridge.accent()) }
        }
    }
}

// ── Badge ──

@Composable
fun DSBadge(text: String, tint: Color, bg: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = DSTypography.labelSmall, fontWeight = FontWeight.Bold, color = tint)
    }
}

// ── Ghost dashed-border card ──

@Composable
fun DSGhostCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DSRadius.card))
            .clickable(onClick = onClick)
            .then(Modifier)
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DSRadius.card - 2.dp))
                .background(Color.Transparent)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}
