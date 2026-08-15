package com.nudge.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nudge.android.ui.theme.DS
import com.nudge.android.ui.theme.DSBridge
import com.nudge.android.ui.theme.DSTypography
import com.nudge.android.ui.theme.Lucide

/** A single responsive modal surface used by Nudge editors and confirmations. */
@Composable
fun NudgeModal(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize().imePadding().padding(horizontal = 18.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(160)) + scaleIn(tween(220), initialScale = .96f),
            ) {
                Surface(
                    modifier = modifier.fillMaxWidth().widthIn(max = 560.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = DSBridge.surface(),
                    border = BorderStroke(1.dp, DSBridge.inkMute().copy(alpha = .14f)),
                    shadowElevation = 18.dp,
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(title, style = DSTypography.headlineMedium, color = DSBridge.ink())
                                subtitle?.let {
                                    Spacer(Modifier.height(5.dp))
                                    Text(it, style = DSTypography.bodySmall, color = DSBridge.inkMute())
                                }
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(42.dp)) {
                                Lucide.X(size = 20.dp, color = DSBridge.inkSoft())
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        content()
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NudgeConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    NudgeModal(
        title = title,
        subtitle = message,
        onDismiss = onDismiss,
        content = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (destructive) DS.Negative.copy(alpha = .10f) else DSBridge.accentBg(),
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (destructive) Lucide.Trash2(size = 20.dp, color = DS.Negative)
                    else Lucide.Check(size = 20.dp, color = DSBridge.accent())
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (destructive) "This action is permanent" else "Review before continuing",
                        style = DSTypography.labelMedium,
                        color = if (destructive) DS.Negative else DSBridge.accent(),
                    )
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { Text("Cancel") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.height(48.dp).widthIn(min = 118.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) DS.Negative else DSBridge.accent(),
                    contentColor = if (destructive) Color.White else DSBridge.surface(),
                ),
            ) { Text(confirmLabel) }
        },
    )
}
