package com.gmail.vondenuelle.denuspend.ui.budget.components.section

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.gmail.vondenuelle.denuspend.ui.budget.components.percentOf
import com.gmail.vondenuelle.denuspend.ui.budget.components.boldAmounts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.longToAmountDouble
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie

/**
 * "Spending by category": a full pie with a legend (name + %) beside it, and one line under it for the tapped slice
 * (or the total when nothing is tapped). Text never sits inside the pie, so long amounts always fit.
 */
@Composable
fun ChartSection(
    modifier: Modifier = Modifier,
    budgetData : BudgetTotalSummaryModel,
) {
    // Only the selected category is state; the line below is computed from it.
    // No remember key on purpose: PieChart keeps its first onPieClick (pointerInput(Unit)), so this state must never be replaced.
    var selectedLabel by remember { mutableStateOf<String?>(null) }

    val data = remember(budgetData, selectedLabel) {
        CategoryDropdownButtonModel.list.drop(1).mapIndexed { index, label ->
            Triple(index, label, budgetData.totals[label] ?: 0L)
        }
            .filter { it.third > 0 }
            .sortedByDescending { it.third }
            .map { (index, label, amount) ->
                Pie(
                    label = label,
                    data = longToAmountDouble(amount),
                    color = CategoryDropdownButtonModel.colors[index],
                    selectedColor = CategoryDropdownButtonModel.selectedColors[index],
                    selected = label == selectedLabel
                )
            }
    }
    val total = budgetData.totalExpense
    val selected = data.find { it.selected } //null when nothing is picked or that category left the range

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                stringResource(R.string.budget_lbl_spending_by_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (total == 0L) {
                EmptyChart(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PieChart(
                    spaceDegree = 2f,
                    selectedPaddingDegree = 4f,
                    style = Pie.Style.Fill,
                    modifier = Modifier.size(140.dp),
                    data = data,
                    onPieClick = {
                        selectedLabel = if (selectedLabel == it.label) null else it.label
                    },
                    selectedScale = 1.08f,
                    scaleAnimEnterSpec = spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    colorAnimEnterSpec = tween(300),
                    colorAnimExitSpec = tween(300),
                    scaleAnimExitSpec = tween(300),
                    spaceDegreeAnimExitSpec = tween(300),
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Legend: the same order as the slices, biggest first. Tapping a name picks its slice too.
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    data.forEach { pie ->
                        val label = pie.label.orEmpty()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { selectedLabel = if (selectedLabel == label) null else label }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(pie.color, CircleShape)
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (pie.selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                            Text(
                                "${percentOf(budgetData.totals[label] ?: 0L, total)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            // The tapped slice, or the total when nothing is tapped
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val selectedAmount = selected?.let { budgetData.totals[it.label] ?: 0L }
                    Text(
                        if (selected != null) {
                            stringResource(R.string.budget_lbl_pie_category, selected.label.orEmpty(), percentOf(selectedAmount ?: 0L, total))
                        } else {
                            stringResource(R.string.budget_lbl_everyday)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "₱${CurrencyUtils.formatCents(selectedAmount ?: total)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                stringResource(R.string.budget_lbl_pie_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// The range chip with the BudgetChartFilter menu. Shared by the dashboard and Insights.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFilterChip(
    filterLabel: String,
    onChangeFilter: (BudgetChartFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val dropdownScrollState = rememberScrollState()

    LaunchedEffect(expanded) {
        if (expanded) {
            // Scroll to show the bottom menu items.
            dropdownScrollState.scrollTo(dropdownScrollState.maxValue)
        }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        AssistChip(
            onClick = {
                //Todo
            },
            label = {
                Text(filterLabel, maxLines = 1)
            },
            leadingIcon = {
                Icon(Icons.Filled.CalendarMonth, null)
            },
            modifier = Modifier
                .padding(bottom = 4.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = dropdownScrollState,
            matchAnchorWidth = false
        ) {
            BudgetChartFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        expanded = false
                        onChangeFilter(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

// "of ₱11,000.00 allowance · ₱1,705.00 left", green when left and red when over. Nothing allowed yet shows a hint instead.
@Composable
fun AllowanceLine(spent: Long, allowance: Long, modifier: Modifier = Modifier, category: String = "") {
    if (allowance == 0L) {
        Text(
            stringResource(R.string.budget_lbl_no_allowance_in_range),
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
        return
    }
    val isOver = spent > allowance
    val status = if (isOver) {
        stringResource(R.string.budget_lbl_over, "₱${CurrencyUtils.formatCents(spent - allowance)}")
    } else {
        stringResource(R.string.budget_lbl_left, "₱${CurrencyUtils.formatCents(allowance - spent)}")
    }
    val statusColor = if (isOver) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text)
    // One Text so it wraps like a sentence if it ever has to, instead of the status breaking off on its own
    Text(
        buildAnnotatedString {
            val allowanceText = "₱${CurrencyUtils.formatCents(allowance)}"
            // Insights passes its category ("of ₱600 Food allowance"), the dashboard's total line doesn't
            // The allowance amount is SemiBold too, so it doesn't read as faint next to the bold status
            append(
                boldAmounts(
                    if (category.isEmpty()) stringResource(R.string.budget_lbl_of_allowance, allowanceText)
                    else stringResource(R.string.budget_lbl_of_category_allowance, allowanceText, category)
                )
            )
            append(" · ")
            withStyle(SpanStyle(color = statusColor, fontWeight = FontWeight.Bold)) { append(status) }
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}

@Composable
private fun EmptyChart(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            Icons.Outlined.PieChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.budget_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            stringResource(R.string.budget_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Preview
@Composable
private fun ChartSectionPreview() {
    DenuSpendTheme() {
        Surface() {
            ChartSection(
                modifier = Modifier,
                budgetData = BudgetTotalSummaryModel(
                    totals = mapOf("Food" to 618000L, "Household" to 786500L, "Personal" to 431000L),
                    totalExpense = 1835500L,
                )
            )
        }
    }
}