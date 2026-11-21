package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// This month's spending vs this month's allowance, from the monthly doc. Tapping it opens the Budget tab.
// The tick on the bar is where spending would be if it were spread evenly (day 21 of 30 = 70%), so a bar past it means spending fast.
@Composable
fun MonthCard(
    modifier: Modifier = Modifier,
    allowance: Long, // cents, whole month
    spent: Long, // cents, whole month
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    // Not remembered, so it shows the new day/month after the listener restarts
    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val daysLeft = daysInMonth - today.dayOfMonth + 1 // today counts as a day you can still spend
    val evenPace = today.dayOfMonth / daysInMonth.toFloat()

    val isOver = spent > allowance
    val progress = when {
        allowance > 0 -> (spent.toFloat() / allowance).coerceIn(0f, 1f)
        spent > 0 -> 1f
        else -> 0f
    }
    val onContainer = MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = onContainer,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(onContainer.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.home_lbl_month_day, today.dayOfMonth, daysInMonth),
                    style = MaterialTheme.typography.labelMedium,
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    if (isLoading) "—" else "₱${CurrencyUtils.formatCents(spent)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
                if (!isLoading && allowance > 0) {
                    Text(
                        stringResource(R.string.home_lbl_month_of, "₱${CurrencyUtils.formatCents(allowance)}"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Bar + even-pace tick. The tick is drawn over the bar at the same fraction of its width.
            BoxWithConstraints(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { if (isLoading) 0f else progress },
                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    trackColor = onContainer.copy(alpha = 0.16f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                if (allowance > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = maxWidth * evenPace - 1.dp)
                            .width(2.dp)
                            .height(16.dp)
                            // A dark tick disappears into the red bar when over, so it switches to the light surface color
                            .background(if (isOver) MaterialTheme.colorScheme.surface else onContainer)
                    )
                }
            }

            when {
                isLoading -> Text(stringResource(R.string.lbl_loading), style = MaterialTheme.typography.bodySmall)
                allowance == 0L && spent == 0L -> Text(
                    stringResource(R.string.home_lbl_month_no_allowance),
                    style = MaterialTheme.typography.bodySmall
                )
                isOver -> Text(
                    stringResource(R.string.home_lbl_amount_over, "₱${CurrencyUtils.formatCents(spent - allowance)}"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                else -> Row {
                    val left = allowance - spent
                    Text(
                        stringResource(R.string.home_lbl_amount_left, "₱${CurrencyUtils.formatCents(left)}"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.verified_green_text)
                    )
                    Text(
                        " · " + stringResource(R.string.home_lbl_month_per_day, "₱${CurrencyUtils.formatCents(left / daysLeft)}"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MonthCardPreview() {
    DenuSpendTheme {
        Surface {
            MonthCard(allowance = 1100000, spent = 823000, isLoading = false, onClick = {})
        }
    }
}

@Preview
@Composable
private fun MonthCardOverPreview() {
    DenuSpendTheme {
        Surface {
            MonthCard(allowance = 500000, spent = 620000, isLoading = false, onClick = {})
        }
    }
}
