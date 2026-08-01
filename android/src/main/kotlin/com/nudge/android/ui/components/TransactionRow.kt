package com.nudge.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nudge.android.ui.theme.Nc
import com.nudge.android.ui.theme.NudgeColors

@Composable
fun TransactionRow(
    modifier: Modifier = Modifier,
    icon: String,
    merchant: String,
    subtext: String,
    amount: String,
    isExpense: Boolean,
    categoryColor: Color = NudgeColors.CatBlue,
    categoryBgColor: Color = NudgeColors.CatBlueBg
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(
                icon = icon,
                color = categoryColor,
                bgColor = categoryBgColor,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = merchant,
                    color = Nc.ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtext,
                    color = Nc.inkMute,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = amount,
                color = if (isExpense) Nc.negative else Nc.positive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider(
            color = Nc.inkMute.copy(alpha = 0.12f),
            thickness = 0.5.dp
        )
    }
}
