package com.nudge.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nudge.android.ui.theme.DS
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.Lucide
import com.nudge.android.ui.theme.NudgeHaptics

@Composable
fun FloatingAddDock(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.fillMaxWidth().navigationBarsPadding().height(88.dp)
            .background(DSBridge.background()),
        contentAlignment = Alignment.BottomCenter,
    ) {
        FloatingActionCube(
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = Modifier.offset(y = (-10).dp),
        ) { Lucide.Plus(size = 24.dp, strokeWidth = 2.4.dp, color = DS.InkPrimary) }
    }
}

@Composable
fun FloatingActionCube(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val haptics = remember(context) { NudgeHaptics(context) }
    Box(
        modifier.size(58.dp)
            .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = DS.Signal.copy(alpha = .34f))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFE8FF76), DS.Signal)))
            .semantics { this.contentDescription = contentDescription; role = Role.Button }
            .clickable {
                haptics.impactMedium()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { icon() }
}
