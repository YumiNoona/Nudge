package com.nudge.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.Nc

@Composable
fun NudgeScaffold(
    title: String,
    onBack: (() -> Unit)?,
    showDock: Boolean,
    dockItems: List<DockItem>,
    dockActiveId: String,
    onDockSelect: (String) -> Unit,
    onFabClick: (() -> Unit)?,
    content: @Composable (Modifier) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Nc.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Lucide.ChevronLeft(size = 20.dp, strokeWidth = 2.dp, color = Nc.inkSoft)
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Nc.ink,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            content(Modifier.fillMaxSize())
        }

        // Bottom dock
        if (showDock) {
            DockBar(
                items = dockItems,
                activeId = dockActiveId,
                onSelect = onDockSelect,
                onFabClick = onFabClick,
                screenWidth = screenWidth
            )
        }
    }
}

@Composable
private fun DockBar(
    items: List<DockItem>,
    activeId: String,
    onSelect: (String) -> Unit,
    onFabClick: (() -> Unit)?,
    screenWidth: Dp
) {
    val fabIndex = if (onFabClick != null) items.size / 2 else -1
    val trackW = screenWidth - 32.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Track
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(20.dp, RoundedCornerShape(50), spotColor = Color.Black.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(50))
                .background(Nc.surface.copy(alpha = 0.94f))
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, item ->
                val isFab = i == fabIndex
                val active = item.id == activeId && !isFab

                if (isFab) {
                    Box(modifier = Modifier.weight(1f).height(56.dp), contentAlignment = Alignment.Center) {
                        // Invisible placeholder — FAB renders above track
                        Spacer(Modifier.size(1.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(item.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        NavigationSlot(item, active)
                    }
                }
            }
        }

        // FAB — rendered above the track
        if (onFabClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-7).dp)
                    .size(52.dp)
                    .shadow(14.dp, CircleShape, spotColor = Nc.accent.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(Nc.accent)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onFabClick() },
                contentAlignment = Alignment.Center
            ) {
                items[fabIndex].icon(Color.White)
            }
        }
    }
}

@Composable
private fun NavigationSlot(item: DockItem, active: Boolean) {
    val tint by animateColorAsState(
        if (active) Nc.accent else Nc.inkSoft,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "navTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(24.dp + if (active) 0.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            item.icon(tint)
        }
        if (item.label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                item.label,
                fontSize = 9.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) Nc.accent else Nc.inkSoft,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
        // Badge
        if (item.badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .offset(x = 12.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(Nc.negative),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${item.badgeCount}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Reusable card with proper shadow + rounded corners
@Composable
fun NudgeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val mod = modifier
        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.06f))
        .clip(RoundedCornerShape(20.dp))
        .background(Nc.surface)
    Box(
        modifier = if (onClick != null) mod.clickable { onClick() } else mod
    ) {
        content()
    }
}
