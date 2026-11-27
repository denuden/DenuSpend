package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.roundDiv
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

// 2–4 short sentences picked by simple rules from the range's numbers
@Composable
fun InsightsSentencesCard(
    insights: BudgetInsightsModel,
    modifier: Modifier = Modifier,
) {
    val lines = insightLines(insights)
    if (lines.isEmpty()) return

    InsightsCard(modifier = modifier) {
        Text(
            stringResource(R.string.insights_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (line.isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        line.icon,
                        contentDescription = null,
                        tint = if (line.isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    boldAmounts(line.text),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 6.dp)
                )
            }
        }
    }
}

private data class InsightLine(val icon: ImageVector, val text: String, val isWarning: Boolean = false)

@Composable
private fun insightLines(insights: BudgetInsightsModel): List<InsightLine> {
    val lines = mutableListOf<InsightLine>()
    val locale = Locale.getDefault()
    val category = insights.category

    // 1. Most expensive weekday vs the rest. Needs 2 weeks, so every weekday shows up at least twice and one day isn't a "habit".
    val weekdaysWithDays = insights.weekdays.filter { it.dayCount > 0 }
    val top = weekdaysWithDays.maxByOrNull { it.avgPerDay }
    val rest = weekdaysWithDays - setOfNotNull(top)
    val restAvg = roundDiv(rest.sumOf { it.spent }, rest.sumOf { it.dayCount }.toLong())
    // With days known, one big purchase that's over half of that weekday's spending isn't a pattern (Biggest day covers it)
    val isOnePurchase = top != null && insights.days.filter { it.date.dayOfWeek == top.dayOfWeek }
        .maxOfOrNull { it.spent }?.let { it * 2 > top.spent } == true
    if (insights.dayCount >= 14 && top != null && restAvg > 0 && !isOnePurchase) {
        val percent = percentChange(top.avgPerDay, restAvg)
        if (percent >= 5) {
            val dayName = top.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            lines += InsightLine(
                Icons.AutoMirrored.Filled.TrendingUp,
                // A huge percent ("3709% more") reads better as "about 38×"
                if (percent >= 100) stringResource(R.string.insights_weekday_times, dayName, times(top.avgPerDay, restAvg), peso(top.avgPerDay), peso(restAvg))
                else stringResource(R.string.insights_weekday, dayName, percent, peso(top.avgPerDay), peso(restAvg))
            )
        }
    }

    // 2. Over and unplanned days. Ranges without day marks (old months, not backfilled) can only judge whole months.
    val over = insights.overDays
    val unplanned = insights.unplannedDays
    when {
        insights.allowance == 0L -> lines += InsightLine(
            Icons.Filled.Warning,
            stringResource(R.string.insights_no_allowance, category),
            isWarning = true
        )
        !insights.hasDayCounts && insights.overspentMonths > 0 -> {
            val names = insights.months.filter { it.isOver }.joinToString(", ") { it.month.month.getDisplayName(TextStyle.FULL, locale) }
            lines += InsightLine(
                Icons.Filled.Warning,
                stringResource(R.string.insights_over_months, category, insights.overspentMonths, insights.months.size, names),
                isWarning = true
            )
        }
        !insights.hasDayCounts -> lines += InsightLine(Icons.Filled.CheckCircle, stringResource(R.string.insights_no_over_months, category))
        over > 0 && unplanned > 0 -> lines += InsightLine(
            Icons.Filled.Warning,
            stringResource(R.string.insights_over_and_unplanned, category, pluralStringResource(R.plurals.budget_days, over, over), unplanned, insights.dayCount),
            isWarning = true
        )
        over > 0 -> {
            // Fri–Sun is only known when the days themselves are loaded
            val weekendDays = insights.days.count { it.isOver && it.date.dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
            lines += InsightLine(
                Icons.Filled.Warning,
                if (over >= 3 && insights.days.isNotEmpty()) stringResource(R.string.insights_over_days_weekend, category, over, insights.dayCount, weekendDays)
                else stringResource(R.string.insights_over_days, category, over, insights.dayCount),
                isWarning = true
            )
        }
        unplanned > 0 -> lines += InsightLine(
            Icons.Filled.Warning,
            stringResource(R.string.insights_unplanned_days, category, unplanned, insights.dayCount),
            isWarning = true
        )
        else -> lines += InsightLine(Icons.Filled.CheckCircle, stringResource(R.string.insights_no_over_days, category))
    }

    // 3. Heaviest month on long ranges, biggest day on short ones
    if (insights.isLongRange) {
        val monthsWithDays = insights.months.filter { it.dayCount > 0 }
        val heaviest = monthsWithDays.maxByOrNull { it.avgPerDay }
        // "Usual" is the other months' average, so the heaviest month isn't compared with itself
        val others = monthsWithDays.filter { it !== heaviest }
        val usual = roundDiv(others.sumOf { it.avgPerDay }, others.size.toLong())
        if (monthsWithDays.size >= 2 && heaviest != null && usual > 0) {
            val percent = percentChange(heaviest.avgPerDay, usual)
            val monthName = heaviest.month.month.getDisplayName(TextStyle.FULL, locale)
            if (percent >= 100) {
                lines += InsightLine(Icons.Filled.Star, stringResource(R.string.insights_heaviest_month_times, monthName, times(heaviest.avgPerDay, usual)))
            } else if (percent > 0) {
                lines += InsightLine(Icons.Filled.Star, stringResource(R.string.insights_heaviest_month, monthName, percent))
            }
        }
    } else {
        insights.biggestDay?.let { day ->
            lines += InsightLine(
                Icons.Filled.Star,
                stringResource(R.string.insights_biggest_day, day.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale)), peso(day.spent))
            )
        }
    }

    // 4. Share of all spending
    if (insights.allCategoriesSpent > 0) {
        lines += InsightLine(Icons.Filled.PieChart, stringResource(R.string.insights_share, category, percentOf(insights.spent, insights.allCategoriesSpent)))
    }
    return lines
}

// How many times bigger, rounded ("about 13×")
private fun times(value: Long, base: Long): Int = (value.toDouble() / base).roundToInt()
