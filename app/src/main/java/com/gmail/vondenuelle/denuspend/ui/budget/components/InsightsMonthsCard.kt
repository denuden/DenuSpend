package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightMonth
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Long ranges only. Newest first; the bold number is avg per day, and the chip compares it with the month before.
@Composable
fun InsightsMonthsCard(
    months: List<InsightMonth>,
    openMonth: YearMonth?,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    // The range has day marks, so each month can say how many days went wrong
    showBadDays: Boolean = false,
) {
    InsightsCard(modifier = modifier) {
        Column {
            Text(stringResource(R.string.insights_months_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.insights_months_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column {
            val newestFirst = months.reversed()
            newestFirst.forEachIndexed { i, month ->
                val previous = newestFirst.getOrNull(i + 1)
                MonthRow(
                    month = month,
                    percent = previous?.avgPerDay?.takeIf { it > 0 }?.let { percentChange(month.avgPerDay, it) },
                    showBadDays = showBadDays,
                    isOpen = month.month == openMonth,
                    onClick = { onMonthClick(month.month) }
                )
                if (i < newestFirst.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MonthRow(month: InsightMonth, percent: Int?, showBadDays: Boolean, isOpen: Boolean, onClick: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val left = month.allowance - month.spent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (isOpen) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    append(month.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    if (month.isPartial) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)) {
                            append("  (${month.firstDay.format(dateFormatter)}–${month.lastDay.dayOfMonth})")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.insights_months_total, peso(month.spent)))
                    if (month.allowance > 0) {
                        append(" · ")
                        if (left >= 0) {
                            append(stringResource(R.string.budget_lbl_left, peso(left)))
                        } else {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)) {
                                append(stringResource(R.string.budget_lbl_over, peso(-left)))
                            }
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // A month can stay under its allowance overall and still have bad days
            if (showBadDays && month.badDays > 0) {
                Text(
                    pluralStringResource(R.plurals.insights_months_bad_days, month.badDays, month.badDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            stringResource(R.string.insights_per_day, peso(month.avgPerDay)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        // The oldest month has nothing before it in the range
        Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
            if (percent != null) DeltaChip(percent)
        }
    }
}
