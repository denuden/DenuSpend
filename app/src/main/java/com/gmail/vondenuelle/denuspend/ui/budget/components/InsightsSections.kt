package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightDay
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightMonth
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightWeekday
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.AllowanceLine
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.BudgetFilterChip
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// All the Insights screen's sections live here, in screen order. They're only used by BudgetInsightsScreen.

private fun peso(cents: Long) = "₱${CurrencyUtils.formatCents(cents)}"

// Category icon + name, the range's dates, and the range chip
@Composable
fun InsightsHeader(
    insights: BudgetInsightsModel,
    filterLabel: String,
    onChangeFilter: (BudgetChartFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = categoryIndex(insights.category)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val dates = if (insights.startDate == insights.endDate) {
        insights.startDate.format(dateFormatter)
    } else {
        "${insights.startDate.format(dateFormatter)} – ${insights.endDate.format(dateFormatter)}"
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CategoryDropdownButtonModel.colors[index])
        ) {
            Icon(
                CategoryDropdownButtonModel.icons[index],
                contentDescription = null,
                tint = CategoryDropdownButtonModel.selectedColors[index]
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                insights.category,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                pluralStringResource(R.plurals.insights_lbl_range_days, insights.dayCount, dates, insights.dayCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BudgetFilterChip(filterLabel = filterLabel, onChangeFilter = onChangeFilter)
    }
}

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
    val overspent = if (insights.isLongRange) insights.overspentMonths else insights.overspentDays

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
                    // Without any allowance every spending day would count as over, so it isn't counted
                    value = if (insights.allowance > 0) overspent.toString() else stringResource(R.string.insights_lbl_none),
                    label = stringResource(
                        if (insights.isLongRange) R.string.insights_stat_overspent_months else R.string.insights_stat_overspent_days
                    ),
                    valueColor = if (insights.allowance > 0 && overspent > 0) MaterialTheme.colorScheme.error else Color.Unspecified,
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

// Avg per day of this range vs the period right before it (same number of days, or months on long ranges)
@Composable
fun InsightsCompareCard(
    insights: BudgetInsightsModel,
    modifier: Modifier = Modifier,
) {
    val current = insights.avgPerDay
    val previous = insights.previousAvgPerDay
    val maxAvg = maxOf(current, previous).coerceAtLeast(1L)

    val previousStart = if (insights.isLongRange) {
        insights.startDate.minusMonths(insights.months.size.toLong())
    } else {
        insights.startDate.minusDays(insights.dayCount.toLong())
    }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val previousDates = "${previousStart.format(dateFormatter)} – ${insights.startDate.minusDays(1).format(dateFormatter)}"

    // Under 1% either way counts as the same
    val percent = if (previous > 0) ((current - previous) * 100 / previous).toInt() else null

    InsightsCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.insights_compare_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.insights_compare_sub, previousDates),
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
        Text(
            when {
                percent == null -> stringResource(R.string.insights_compare_no_data, insights.category)
                percent == 0 -> stringResource(R.string.insights_compare_same, insights.category)
                current > previous -> stringResource(R.string.insights_compare_more, peso(current - previous), insights.category)
                else -> stringResource(R.string.insights_compare_less, peso(previous - current), insights.category)
            },
            style = MaterialTheme.typography.bodyMedium
        )
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

// ▲ red when spending went up, ▼ green when it went down. Also used by Month by month.
@Composable
fun DeltaChip(percent: Int, modifier: Modifier = Modifier) {
    val (container, content, text) = when {
        percent > 0 -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "▲ $percent%")
        percent < 0 -> Triple(colorResource(R.color.verified_green_container), colorResource(R.color.verified_green_text), "▼ ${-percent}%")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.insights_compare_same_chip))
    }
    Surface(shape = MaterialTheme.shapes.small, color = container, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

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

// One bar of InsightsBarChart: a day or a month. A null label leaves that bar unlabeled (long ranges skip some days).
data class ChartBar(
    val label: String?,
    val spent: Long,
    val allowance: Long,
    val isOver: Boolean,
)

// Days as bars: "Fri 14" up to 7 days, otherwise the day number every 2 or 5 days
fun dayBars(days: List<InsightDay>): List<ChartBar> {
    val formatter = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())
    val labelEvery = when {
        days.size <= 7 -> 1
        days.size <= 15 -> 2
        else -> 5
    }
    return days.mapIndexed { i, day ->
        val label = when {
            days.size <= 7 -> day.date.format(formatter)
            i % labelEvery == 0 || i == days.lastIndex -> day.date.dayOfMonth.toString()
            else -> null
        }
        ChartBar(label, day.spent, day.allowance, day.isOver)
    }
}

fun monthBars(months: List<InsightMonth>): List<ChartBar> = months.map { month ->
    ChartBar(month.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), month.spent, month.allowance, month.isOver)
}

/**
 * The Insights chart card: per day (short ranges or an opened month) or per month (long ranges).
 * [onBack] shows "Back to months" above the title when a month is open. [footer] goes under the legend.
 */
@Composable
fun InsightsChartCard(
    title: String,
    subtitle: String,
    bars: List<ChartBar>,
    selectedIndex: Int?,
    onBarClick: (Int) -> Unit,
    // Days draw the allowance as one dashed step line, months as a short dashed line on each bar
    allowanceAsSteps: Boolean,
    category: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onBack: (() -> Unit)? = null,
    footer: @Composable () -> Unit = {},
) {
    InsightsCard(modifier = modifier) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.insights_back_to_months), modifier = Modifier.padding(start = 6.dp))
            }
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isLoading && bars.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            InsightsBarChart(bars = bars, selectedIndex = selectedIndex, onBarClick = onBarClick, allowanceAsSteps = allowanceAsSteps)
        }
        ChartLegend(category = category, showAllowance = bars.any { it.allowance > 0 })
        footer()
    }
}

// Drawn on a Canvas: compose-charts' ColumnChart has no per-bar colors with an allowance line on top, nor a tap per bar
@Composable
private fun InsightsBarChart(
    bars: List<ChartBar>,
    selectedIndex: Int?,
    onBarClick: (Int) -> Unit,
    allowanceAsSteps: Boolean,
) {
    if (bars.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val barColor = MaterialTheme.colorScheme.primary
    val overColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val allowanceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val currentOnBarClick by rememberUpdatedState(onBarClick)
    // Months are few, so their bars are thinner inside a wide slot
    val barShare = if (allowanceAsSteps) 0.7f else 0.5f

    // Y axis in whole pesos, rounded up to a nice step so the gridlines land on round numbers
    val maxPesos = (bars.maxOf { maxOf(it.spent, it.allowance) } / 100.0 * 1.1).coerceAtLeast(1.0)
    val step = niceStep(maxPesos / 3)
    val topPesos = ceil(maxPesos / step) * step

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    val left = CHART_Y_AXIS.toPx()
                    val slot = (size.width - left) / bars.size
                    val index = ((offset.x - left) / slot).toInt()
                    if (offset.x >= left && index in bars.indices) currentOnBarClick(index)
                }
            }
    ) {
        val left = CHART_Y_AXIS.toPx()
        val bottom = size.height - CHART_X_AXIS.toPx()
        val slot = (size.width - left) / bars.size
        fun y(pesos: Double) = (bottom - pesos / topPesos * bottom).toFloat()

        var gridValue = 0.0
        while (gridValue <= topPesos + 0.001) {
            drawLine(gridColor, Offset(left, y(gridValue)), Offset(size.width, y(gridValue)), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure(axisLabel(gridValue), labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y(gridValue) - label.size.height / 2))
            gridValue += step
        }

        bars.forEachIndexed { i, bar ->
            val barWidth = slot * barShare
            val x = left + i * slot + slot * (1 - barShare) / 2
            val top = y(bar.spent / 100.0)
            val alpha = if (selectedIndex != null && i != selectedIndex) 0.45f else 1f
            if (bar.spent > 0) {
                drawRoundRect(
                    color = if (bar.isOver) overColor else barColor,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, bottom - top),
                    cornerRadius = CornerRadius(minOf(4.dp.toPx(), barWidth / 3)),
                    alpha = alpha
                )
            }
            if (!allowanceAsSteps && bar.allowance > 0) {
                val lineY = y(bar.allowance / 100.0)
                drawLine(
                    allowanceColor,
                    Offset(x - 3.dp.toPx(), lineY),
                    Offset(x + barWidth + 3.dp.toPx(), lineY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }
            bar.label?.let { text ->
                val label = textMeasurer.measure(text, labelStyle)
                drawText(label, topLeft = Offset(x + barWidth / 2 - label.size.width / 2, bottom + 4.dp.toPx()))
            }
        }

        // Allowance as a dashed step line: flat across each day's slot
        if (allowanceAsSteps && bars.any { it.allowance > 0 }) {
            val path = Path()
            bars.forEachIndexed { i, bar ->
                val lineY = y(bar.allowance / 100.0)
                val x = left + i * slot
                if (i == 0) path.moveTo(x, lineY) else path.lineTo(x, lineY)
                path.lineTo(x + slot, lineY)
            }
            drawPath(
                path,
                color = allowanceColor,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            )
        }
    }
}

// The opened month's numbers under its daily bars. Totals come from its daily docs, so overspent days are known.
@Composable
fun InsightsMonthDetail(monthDays: BudgetInsightsModel, modifier: Modifier = Modifier) {
    val left = monthDays.allowance - monthDays.spent
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val monthName = monthDays.startDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val statusColor = if (left < 0) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text)

    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(monthName) }
                    append(" · ${monthDays.startDate.format(dateFormatter)} – ${monthDays.endDate.format(dateFormatter)}")
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (monthDays.allowance > 0) {
                    buildAnnotatedString {
                        append(stringResource(R.string.insights_month_spent_of, peso(monthDays.spent), peso(monthDays.allowance), monthDays.category))
                        withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.Bold)) {
                            append(
                                if (left >= 0) stringResource(R.string.budget_lbl_left, peso(left))
                                else stringResource(R.string.budget_lbl_over, peso(-left))
                            )
                        }
                    }
                } else {
                    AnnotatedString(stringResource(R.string.insights_month_spent, peso(monthDays.spent)))
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                pluralStringResource(R.plurals.budget_transactions_made, monthDays.transactionCount.toInt(), monthDays.transactionCount.toInt()) +
                    " · " + stringResource(R.string.insights_month_avg_day, peso(monthDays.avgPerDay)),
                style = MaterialTheme.typography.bodyMedium
            )
            if (monthDays.allowance > 0) {
                Text(
                    stringResource(R.string.insights_month_over_days, monthDays.category, monthDays.overspentDays, monthDays.dayCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (monthDays.overspentDays > 0) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
        }
    }
}

// Long ranges only. Newest first; the bold number is avg per day, and the chip compares it with the month before.
@Composable
fun InsightsMonthsCard(
    months: List<InsightMonth>,
    openMonth: YearMonth?,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
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
                    percent = previous?.avgPerDay?.takeIf { it > 0 }?.let { ((month.avgPerDay - it) * 100 / it).toInt() },
                    isOpen = month.month == openMonth,
                    onClick = { onMonthClick(month.month) }
                )
                if (i < newestFirst.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MonthRow(month: InsightMonth, percent: Int?, isOpen: Boolean, onClick: () -> Unit) {
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

private val CHART_Y_AXIS = 40.dp
private val CHART_X_AXIS = 20.dp

// "₱300", "₱1.5k"
private fun axisLabel(pesos: Double): String = when {
    pesos >= 1000 -> {
        val thousands = pesos / 1000
        if (thousands % 1.0 == 0.0) "₱${thousands.toInt()}k" else "₱${"%.1f".format(thousands)}k"
    }
    else -> "₱${pesos.toInt()}"
}

// 1, 2, 5 or 10 times a power of ten, the closest round number to the wanted step
private fun niceStep(raw: Double): Double {
    val power = 10.0.pow(floor(log10(raw)))
    val fraction = raw / power
    return when {
        fraction < 1.5 -> 1.0
        fraction < 3 -> 2.0
        fraction < 7 -> 5.0
        else -> 10.0
    } * power
}

@Composable
private fun ChartLegend(category: String, showAllowance: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.insights_legend_spent))
        LegendItem(color = MaterialTheme.colorScheme.error, label = stringResource(R.string.insights_legend_over))
        if (showAllowance) {
            val dashColor = MaterialTheme.colorScheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(width = 16.dp, height = 10.dp)) {
                    drawLine(
                        dashColor,
                        Offset(0f, size.height / 2),
                        Offset(size.width, size.height / 2),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    )
                }
                Text(
                    stringResource(R.string.insights_legend_allowance, category),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// Bottom sheet for a tapped day: spent vs that day's allowance, then the day's transactions in this category
@Composable
fun InsightsDaySheetContent(
    day: InsightDay,
    category: String,
    transactions: List<TransactionModel>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(stringResource(R.string.insights_day_spent, peso(day.spent)), style = MaterialTheme.typography.bodyMedium)
        if (day.allowance > 0) {
            AllowanceLine(spent = day.spent, allowance = day.allowance, category = category)
        } else {
            Text(
                stringResource(R.string.insights_day_no_allowance, category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            transactions.isEmpty() -> Text(
                stringResource(R.string.insights_day_empty, category),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            else -> transactions.forEach { transaction ->
                TransactionItem(transactionModel = transaction, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Average spending per weekday (Mon–Sun), the most expensive one highlighted. Tap a row for its details.
// Overspent days per weekday are only shown on short ranges, because long ranges only have monthly totals.
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
    val showOverspent = !insights.isLongRange && insights.allowance > 0

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
                            stringResource(R.string.insights_weekday_detail_over, insights.category, detail.overspentDays, detail.dayCount, dayName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (detail.overspentDays > 0) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

// Shown instead of the charts and insights when the range has no spending in this category
@Composable
fun InsightsEmptyCard(category: String, modifier: Modifier = Modifier) {
    InsightsCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 8.dp)
        ) {
            Icon(
                Icons.Filled.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(40.dp)
            )
            Text(
                stringResource(R.string.insights_empty_title, category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.insights_empty_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Plain section card used by every Insights section below the summary
@Composable
private fun InsightsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) { content() }
    }
}

// Unknown categories fall back to "Others" (last in the list)
private fun categoryIndex(category: String): Int {
    val names = CategoryDropdownButtonModel.list.drop(1)
    return names.indexOf(category).takeIf { it >= 0 } ?: names.lastIndex
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
