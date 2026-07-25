package com.nudge.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val hasCenterFab = items.size % 2 != 0 && onFabClick != null
    val centerIndex = items.size / 2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            color = NudgeColors.Surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isActive = item.id == activeId
                    val isCenterSlot = hasCenterFab && index == centerIndex

                    if (isCenterSlot) {
                        Spacer(modifier = Modifier.width(56.dp))
                    } else {
                        NavSlot(
                            item = item,
                            isActive = isActive,
                            onClick = { onSelect(item.id) }
                        )
                    }
                }
            }
        }

        if (hasCenterFab) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NudgeColors.Purple, NudgeColors.PurpleDeep),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFabClick?.invoke() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = NudgeColors.Surface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun NavSlot(
    item: BottomNavItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (isActive) {
                    Modifier.background(NudgeColors.Purple.copy(alpha = 0.12f))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Box {
                item.icon()

                if (item.badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(NudgeColors.Coral),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                            color = NudgeColors.Surface,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = item.label,
                color = if (isActive) NudgeColors.Purple else NudgeColors.InkMute,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}
