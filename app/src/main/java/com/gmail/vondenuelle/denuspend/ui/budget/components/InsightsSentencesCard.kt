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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
                    line.text,
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

    // 1. Most expensive weekday vs the rest (needs at least a week to mean anything)
    val weekdaysWithDays = insights.weekdays.filter { it.dayCount > 0 }
    val top = weekdaysWithDays.maxByOrNull { it.avgPerDay }
    val rest = weekdaysWithDays - setOfNotNull(top)
    val restDays = rest.sumOf { it.dayCount }
    val restAvg = if (restDays > 0) rest.sumOf { it.spent } / restDays else 0L
    if (insights.dayCount >= 7 && top != null && restAvg > 0) {
        val percent = ((top.avgPerDay - restAvg) * 100 / restAvg).toInt()
        if (percent >= 5) {
            val dayName = top.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            lines += InsightLine(
                Icons.AutoMirrored.Filled.TrendingUp,
                stringResource(R.string.insights_weekday, dayName, percent, peso(top.avgPerDay), peso(restAvg))
            )
        }
    }

    // 2. Overspending: per day on short ranges, per month on long ones
    when {
        insights.allowance == 0L -> lines += InsightLine(
            Icons.Filled.Warning,
            stringResource(R.string.insights_no_allowance, category),
            isWarning = true
        )
        insights.isLongRange && insights.overspentMonths > 0 -> {
            val names = insights.months.filter { it.isOver }.joinToString(", ") { it.month.month.getDisplayName(TextStyle.FULL, locale) }
            lines += InsightLine(
                Icons.Filled.Warning,
                stringResource(R.string.insights_over_months, category, insights.overspentMonths, insights.months.size, names),
                isWarning = true
            )
        }
        insights.isLongRange -> lines += InsightLine(Icons.Filled.CheckCircle, stringResource(R.string.insights_no_over_months, category))
        insights.overspentDays > 0 -> {
            val overDays = insights.days.filter { it.isOver }
            val weekendDays = overDays.count { it.date.dayOfWeek in listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
            lines += InsightLine(
                Icons.Filled.Warning,
                if (overDays.size >= 3) stringResource(R.string.insights_over_days_weekend, category, overDays.size, insights.dayCount, weekendDays)
                else stringResource(R.string.insights_over_days, category, overDays.size, insights.dayCount),
                isWarning = true
            )
        }
        else -> lines += InsightLine(Icons.Filled.CheckCircle, stringResource(R.string.insights_no_over_days, category))
    }

    // 3. Heaviest month on long ranges, biggest day on short ones
    if (insights.isLongRange) {
        val monthsWithDays = insights.months.filter { it.dayCount > 0 }
        val heaviest = monthsWithDays.maxByOrNull { it.avgPerDay }
        val usual = if (monthsWithDays.isNotEmpty()) monthsWithDays.sumOf { it.avgPerDay } / monthsWithDays.size else 0L
        if (monthsWithDays.size >= 2 && heaviest != null && usual > 0) {
            val percent = ((heaviest.avgPerDay - usual) * 100 / usual).toInt()
            if (percent > 0) {
                lines += InsightLine(
                    Icons.Filled.Star,
                    stringResource(R.string.insights_heaviest_month, heaviest.month.month.getDisplayName(TextStyle.FULL, locale), percent)
                )
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
        val share = (insights.spent * 100 / insights.allCategoriesSpent).toInt()
        lines += InsightLine(Icons.Filled.PieChart, stringResource(R.string.insights_share, category, share))
    }
    return lines
}
