package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightMonth
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightWeekday
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.AllowanceLine
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// Spent vs allowance with a bar, then Avg per day · Avg per transaction · Overspent days (months on long ranges)
@Composable
fun InsightsSummaryCard(
    insights: BudgetInsightsModel,
    modifier: Modifier = Modifier,
) {
    val isOver = insights.spent > insights.allowance
    // No allowance but some spending shows as a full red bar, like the Add tab
    val progress = when {
        insights.allowance > 0 -> (insights.spent.toFloat() / insights.allowance).coerceIn(0f, 1f)
        insights.spent > 0 -> 1f
        else -> 0f
    }
    // Over or unplanned days. Months only when the range has no day marks (old months that weren't backfilled).
    val byMonths = !insights.hasDayCounts
    val overspent = if (byMonths) insights.overspentMonths else insights.badDays

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.insights_lbl_spent_on, insights.category),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                peso(insights.spent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )
            AllowanceLine(
                spent = insights.spent,
                allowance = insights.allowance,
                category = insights.category,
                modifier = Modifier.padding(top = 4.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = 16.dp)
            ) {
                StatTile(
                    value = peso(insights.avgPerDay),
                    label = stringResource(R.string.insights_stat_avg_day),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                StatTile(
                    value = if (insights.transactionCount > 0) peso(insights.avgPerTransaction) else stringResource(R.string.insights_lbl_none),
                    label = stringResource(R.string.insights_stat_avg_transaction),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                StatTile(
                    // Spending with no allowance counts as unplanned, so this counts even without any allowance.
                    // Months can't tell "over" from "unplanned", so without an allowance there's nothing to count.
                    value = if (!byMonths || insights.allowance > 0) overspent.toString() else stringResource(R.string.insights_lbl_none),
                    label = stringResource(
                        if (byMonths) R.string.insights_stat_overspent_months else R.string.insights_stat_overspent_days
                    ),
                    valueColor = if (overspent > 0 && (!byMonths || insights.allowance > 0)) MaterialTheme.colorScheme.error else Color.Unspecified,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun InsightsSummaryPreview() {
    val insights = BudgetInsightsModel(
        category = "Food",
        startDate = LocalDate.of(2026, 11, 1),
        endDate = LocalDate.of(2026, 11, 20),
        dayCount = 20,
        spent = 542000L,
        allowance = 600000L,
        transactionCount = 38L,
        allCategoriesSpent = 1230000L,
        months = listOf(
            InsightMonth(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), 542000L, 600000L, 38L)
        ),
        weekdays = DayOfWeek.entries.map {
            InsightWeekday(it, dayCount = 3, spent = if (it == DayOfWeek.FRIDAY) 118000L else 70000L)
        },
        previousSpent = 462000L,
        previousDayCount = 20,
    )
    DenuSpendTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                InsightsHeader(insights = insights, filterLabel = "This Month", onChangeFilter = {})
                InsightsSummaryCard(insights = insights, modifier = Modifier.padding(top = 16.dp))
                InsightsCompareCard(insights = insights, modifier = Modifier.padding(top = 12.dp))
                InsightsSentencesCard(insights = insights, modifier = Modifier.padding(top = 12.dp))
                InsightsWeekdayCard(insights = insights, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
