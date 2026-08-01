package com.nudge.android.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.Nc

@Composable
fun <T> ScrollableChipRow(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (T) -> Unit,
    chipMinWidth: Dp = 72.dp,
    selectedContainerColor: Color = Nc.accentBg,
    selectedLabelColor: Color = Nc.accent,
    unselectedContainerColor: Color = Nc.background,
    unselectedLabelColor: Color = Nc.inkSoft
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSel = item == selected
            FilterChip(
                selected = isSel,
                onClick = { onSelect(item) },
                label = { Box(Modifier.widthIn(min = chipMinWidth), contentAlignment = androidx.compose.ui.Alignment.Center) { label(item) } },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedContainerColor,
                    selectedLabelColor = selectedLabelColor,
                    containerColor = unselectedContainerColor,
                    labelColor = unselectedLabelColor
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.heightIn(min = 36.dp)
            )
        }
    }
}

// Convenience for simple string-labeled chips
@Composable
fun StringChipRow(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableChipRow(
        items = items,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        label = { item ->
            Text(
                item,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    )
}
