package com.nudge.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.NudgeColors

data class BottomNavItem(
    val id: String,
    val icon: @Composable () -> Unit,
    val label: String,
    val badgeCount: Int = 0
)

@Composable
fun BottomNav(
    items: List<BottomNavItem>,
    activeId: String,
    onSelect: (String) -> Unit,
    onFabClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Calculate the active indicator offset using spring animation
    val activeIndex = items.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    val itemWidth = 64.dp // approximate width per item slot
    val indicatorOffset by animateDpAsState(
        targetValue = itemWidth * activeIndex,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIndicator"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glass background using Haze
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(50), ambientColor = Color.Black.copy(alpha = 0.06f))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            (NudgeColors.Surface).copy(alpha = 0.85f),
                            (NudgeColors.Surface).copy(alpha = 0.70f)
                        )
                    )
                )
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isActive = item.id == activeId
                val iconColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else NudgeColors.InkSoft,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navIconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isActive) NudgeColors.Emerald else NudgeColors.InkMute,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navLabelColor"
                )

                val isCenterFab = (onFabClick != null) && (index == items.size / 2)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Active emerald pill indicator — slides between items
                    if (isActive && !isCenterFab) {
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .size(48.dp, 36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(NudgeColors.Emerald)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                if (isCenterFab) onFabClick?.invoke()
                                else onSelect(item.id)
                            }
                            .then(
                                if (isCenterFab) Modifier
                                    .offset(y = (-16).dp)
                                    .size(52.dp)
                                    .shadow(10.dp, CircleShape, ambientColor = NudgeColors.Emerald.copy(alpha = 0.3f))
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(NudgeColors.Emerald, NudgeColors.EmeraldDeep)
                                        )
                                    )
                                    .padding(12.dp)
                                else Modifier
                            )
                    ) {
                        if (!isCenterFab) {
                            item.icon()
                            if (isActive) {
                                Text(
                                    item.label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = labelColor,
                                    maxLines = 1
                                )
                            }
                        } else {
                            item.icon()
                        }

                        // Badge
                        if (item.badgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(16.dp)
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
