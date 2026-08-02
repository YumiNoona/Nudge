package com.nudge.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.DS
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.MonoFamily
import com.nudge.android.ui.theme.NudgeHaptics

data class DockItem(
    val id: String,
    val icon: @Composable (Color) -> Unit,
    val label: String,
    val badgeCount: Int = 0
)

/** High-contrast, CRED-inspired navigation dock with an integrated add action. */
@Composable
fun BottomDock(
    items: List<DockItem>,
    activeId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
) {
    val actionIndex = if (onFabClick != null) items.size / 2 else -1
    val localContext = LocalContext.current
    val haptics = remember(localContext) { NudgeHaptics(localContext) }
    val dockBackground = DSBridge.surface()
    val dockInactive = DSBridge.inkMute()
    val dockActive = DSBridge.accent()
    val activeBackground = DSBridge.accentBg()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(70.dp)
            .shadow(14.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.14f))
            .clip(RoundedCornerShape(22.dp))
            .background(dockBackground)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isAction = index == actionIndex
            val active = item.id == activeId && !isAction
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = spring(stiffness = 650f, dampingRatio = 0.72f),
                label = "dockPress"
            )
            val tint by animateColorAsState(
                targetValue = if (active) dockActive else dockInactive,
                label = "dockTint"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .scale(scale)
                    .clip(RoundedCornerShape(15.dp))
                    .then(if (active) Modifier.background(activeBackground) else Modifier)
                    .clickable(interactionSource = interaction, indication = null) {
                        if (isAction) haptics.impactMedium() else haptics.impactLight()
                        if (isAction) onFabClick?.invoke() else onSelect(item.id)
                    }
                    .semantics { contentDescription = if (isAction) "Add transaction" else item.label },
                contentAlignment = Alignment.Center
            ) {
                if (isAction) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = DS.Signal.copy(alpha = 0.26f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(DS.Signal),
                        contentAlignment = Alignment.Center
                    ) {
                        item.icon(DS.InkPrimary)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            item.icon(tint)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label.uppercase(),
                            color = if (active) dockActive else dockInactive,
                            fontFamily = MonoFamily,
                            fontSize = 7.sp,
                            lineHeight = 10.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            maxLines = 1
                        )
                    }

                    if (item.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 1.dp)
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(DS.Negative),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.badgeCount.coerceAtMost(9).toString(),
                                color = Color.White,
                                fontFamily = MonoFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
