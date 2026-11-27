package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// One bar of InsightsBarChart: a day or a month. A null label leaves that bar unlabeled (long ranges skip some days).
data class ChartBar(
    val label: String?,
    val spent: Long,
    val allowance: Long,
    val isOver: Boolean,
    // Spending with no allowance (amber), apart from going over one (red)
    val isUnplanned: Boolean = false,
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
        // The last day always gets a label, so a regular one right before it is skipped ("26" and "27" touched)
        val tooCloseToLast = days.lastIndex - i in 1 until labelEvery / 2 + 1
        val label = when {
            days.size <= 7 -> day.date.format(formatter)
            i == days.lastIndex -> day.date.dayOfMonth.toString()
            i % labelEvery == 0 && !tooCloseToLast -> day.date.dayOfMonth.toString()
            else -> null
        }
        ChartBar(label, day.spent, day.allowance, day.isOver, day.isUnplanned)
    }
}

fun monthBars(months: List<InsightMonth>): List<ChartBar> = months.map { month ->
    ChartBar(
        month.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), month.spent, month.allowance,
        isOver = month.allowance > 0 && month.isOver,
        isUnplanned = month.allowance == 0L && month.spent > 0,
    )
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
        ChartLegend(category = category, showAllowance = bars.any { it.allowance > 0 }, showUnplanned = bars.any { it.isUnplanned })
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
    val unplannedColor = colorResource(R.color.unplanned)
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
                    color = when {
                        bar.isOver -> overColor
                        bar.isUnplanned -> unplannedColor
                        else -> barColor
                    },
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
                        append(boldAmounts(stringResource(R.string.insights_month_spent_of, peso(monthDays.spent), peso(monthDays.allowance), monthDays.category)))
                        withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.Bold)) {
                            append(
                                if (left >= 0) stringResource(R.string.budget_lbl_left, peso(left))
                                else stringResource(R.string.budget_lbl_over, peso(-left))
                            )
                        }
                    }
                } else {
                    boldAmounts(stringResource(R.string.insights_month_spent, peso(monthDays.spent)))
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                boldAmounts(
                    pluralStringResource(R.plurals.budget_transactions_made, monthDays.transactionCount.toInt(), monthDays.transactionCount.toInt()) +
                    " · " + stringResource(R.string.insights_month_avg_day, peso(monthDays.avgPerDay))
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            if (monthDays.allowance > 0 || monthDays.unplannedDays > 0) {
                Text(
                    if (monthDays.unplannedDays > 0) {
                        stringResource(R.string.insights_month_bad_days, monthDays.overDays, monthDays.unplannedDays, monthDays.dayCount)
                    } else {
                        stringResource(R.string.insights_month_over_days, monthDays.category, monthDays.overDays, monthDays.dayCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (monthDays.badDays > 0) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartLegend(category: String, showAllowance: Boolean, showUnplanned: Boolean) {
    // Wraps to a second line when the category name is long
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.insights_legend_spent))
        LegendItem(color = MaterialTheme.colorScheme.error, label = stringResource(R.string.insights_legend_over))
        if (showUnplanned) LegendItem(color = colorResource(R.color.unplanned), label = stringResource(R.string.insights_legend_unplanned))
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
