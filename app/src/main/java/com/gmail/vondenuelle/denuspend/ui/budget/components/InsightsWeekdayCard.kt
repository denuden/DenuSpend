package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// Average spending per weekday (Mon–Sun), the most expensive one highlighted. Tap a row for its details.
// Over / unplanned days per weekday need each day: short ranges have them, long ranges once their months have day marks.
@Composable
fun InsightsWeekdayCard(
    insights: BudgetInsightsModel,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val locale = Locale.getDefault()
    val index = categoryIndex(insights.category)
    val maxAvg = insights.weekdays.maxOfOrNull { it.avgPerDay }?.coerceAtLeast(1L) ?: 1L
    val top = insights.weekdays.filter { it.dayCount > 0 }.maxByOrNull { it.avgPerDay }
    // Needs each day's mark: daily ranges always have them, monthly ones once the months have day marks
    val showOverspent = insights.hasDayCounts

    InsightsCard(modifier = modifier) {
        Column {
            Text(stringResource(R.string.insights_weekday_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.insights_weekday_sub, insights.category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            insights.weekdays.forEach { weekday ->
                val isTop = weekday == top
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(if (selected == weekday.dayOfWeek) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { selected = if (selected == weekday.dayOfWeek) null else weekday.dayOfWeek }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        weekday.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(40.dp)
                    )
                    LinearProgressIndicator(
                        progress = { weekday.avgPerDay.toFloat() / maxAvg },
                        color = if (isTop) CategoryDropdownButtonModel.selectedColors[index] else CategoryDropdownButtonModel.colors[index],
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                    )
                    Text(
                        if (weekday.dayCount > 0) peso(weekday.avgPerDay) else stringResource(R.string.insights_lbl_none),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(84.dp)
                    )
                }
            }
        }

        val detail = insights.weekdays.find { it.dayOfWeek == selected }
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            if (detail == null) {
                Text(
                    stringResource(R.string.insights_weekday_hint),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                val dayName = detail.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.insights_weekday_detail_count, dayName, detail.dayCount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.insights_weekday_detail_total, peso(detail.spent), peso(detail.avgPerDay)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (showOverspent) {
                        Text(
                            stringResource(R.string.insights_weekday_detail_over, insights.category, detail.badDays, detail.dayCount, dayName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (detail.badDays > 0) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}
