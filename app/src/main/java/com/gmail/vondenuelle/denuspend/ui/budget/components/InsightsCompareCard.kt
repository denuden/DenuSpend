package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import java.time.format.DateTimeFormatter
import java.util.Locale

// Avg per day of this range vs the period right before it (same number of days, or months on long ranges)
@Composable
fun InsightsCompareCard(
    insights: BudgetInsightsModel,
    modifier: Modifier = Modifier,
) {
    val current = insights.avgPerDay
    val previous = insights.previousAvgPerDay
    val maxAvg = maxOf(current, previous).coerceAtLeast(1L)

    // The year shows when the period before isn't all in this range's year ("Oct 1, 2025 – May 31")
    val previousStart = insights.previousStartDate
    val previousEnd = insights.startDate.minusDays(1)
    val startFormatter = DateTimeFormatter.ofPattern(if (previousStart.year != insights.startDate.year) "MMM d, yyyy" else "MMM d", Locale.getDefault())
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val previousDates = if (insights.previousDayCount > 0) {
        stringResource(R.string.insights_compare_sub, "${previousStart.format(startFormatter)} – ${previousEnd.format(dateFormatter)}")
    } else {
        stringResource(R.string.insights_compare_sub_untracked)
    }

    // Rounds to 0% (under 0.5% either way) = the same
    val percent = if (previous > 0) percentChange(current, previous) else null

    InsightsCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.insights_compare_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    previousDates,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (percent != null) DeltaChip(percent)
        }
        CompareRow(
            label = stringResource(R.string.insights_compare_this_range),
            value = peso(current),
            fraction = current.toFloat() / maxAvg,
            barColor = CategoryDropdownButtonModel.selectedColors[categoryIndex(insights.category)],
        )
        CompareRow(
            label = stringResource(R.string.insights_compare_before),
            value = if (previous > 0) peso(previous) else stringResource(R.string.insights_lbl_none),
            fraction = previous.toFloat() / maxAvg,
            barColor = MaterialTheme.colorScheme.outline,
        )
        // With nothing tracked before, the subtitle already says so, so no second line saying the same thing
        if (insights.previousDayCount > 0) {
            Text(
                boldAmounts(
                    when {
                        percent == null -> stringResource(R.string.insights_compare_no_data, insights.category)
                        percent == 0 -> stringResource(R.string.insights_compare_same, insights.category)
                        current > previous -> stringResource(R.string.insights_compare_more, peso(current - previous), insights.category)
                        else -> stringResource(R.string.insights_compare_less, peso(previous - current), insights.category)
                    }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CompareRow(label: String, value: String, fraction: Float, barColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(76.dp)
        )
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
            gapSize = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(84.dp)
        )
    }
}
