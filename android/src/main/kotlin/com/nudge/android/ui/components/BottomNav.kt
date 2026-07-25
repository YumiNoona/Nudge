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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.NudgeColors

data class BNavItem(
    val id: String,
    val icon: @Composable (Color) -> Unit,
    val label: String,
    val badgeCount: Int = 0
)

@Composable
fun BottomDock(
    items: List<BNavItem>,
    activeId: String,
    onSelect: (String) -> Unit,
    onFabClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeIndex = items.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    val slotWidth = 64.dp

    val indicatorX by animateDpAsState(
        targetValue = slotWidth * activeIndex,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navSlide"
    )

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        // Track — frosted pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(16.dp, RoundedCornerShape(50), spotColor = Color.Black.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.88f))
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, item ->
                val isFab = onFabClick != null && i == items.size / 2
                val active = item.id == activeId

                val tint by animateColorAsState(
                    if (active && !isFab) NudgeColors.Emerald
                    else if (isFab) Color.White
                    else Color(0xFF444444),
                    label = "navTint"
                )

                if (isFab) {
                    // Raised center FAB
                    Box(
                        modifier = Modifier
                            .offset(y = (-14).dp)
                            .size(48.dp)
                            .shadow(10.dp, CircleShape, spotColor = NudgeColors.Emerald.copy(alpha = 0.35f))
                            .clip(CircleShape)
                            .background(NudgeColors.Emerald)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onFabClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        item.icon(tint)
                    }
                } else {
                    // Normal slot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onSelect(item.id) }
                    ) {
                        // Active pill behind icon
                        if (active) {
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorX - slotWidth * i)
                                    .size(48.dp, 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NudgeColors.Emerald.copy(alpha = 0.15f))
                            )
                        }

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
                            item.icon(tint)
                        }

                        // Label
                        Text(
                            item.label,
                            fontSize = 9.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) NudgeColors.Emerald else Color(0xFF666666),
                            maxLines = 1
                        )

                        // Badge
                        if (item.badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 10.dp, y = (-28).dp)
                                    .clip(CircleShape)
                                    .background(NudgeColors.Coral),
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
            }
        }
    }
}
