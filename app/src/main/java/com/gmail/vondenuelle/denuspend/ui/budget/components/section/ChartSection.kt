package com.gmail.vondenuelle.denuspend.ui.budget.components.section

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.longToAmountDouble
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChartSection(
    modifier: Modifier = Modifier,
    filterLabel : String, //the filter name, or the picked dates for a custom range
    budgetData : BudgetTotalSummaryModel,
    onChangeFilter : (BudgetChartFilter) -> Unit
) {
    // Only the selected category is state; the chip and amount below are computed from it.
    // No remember key on purpose: PieChart keeps its first onPieClick (pointerInput(Unit)), so this state must never be replaced.
    var selectedLabel by remember { mutableStateOf<String?>(null) }

    val data = remember(budgetData, selectedLabel) {
        CategoryDropdownButtonModel.list.drop(1).mapIndexed { index, label ->
            Triple(index, label, budgetData.totals[label] ?: 0L)
        }
            .filter { it.third > 0 }
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

    val totalAmount = longToAmountDouble(budgetData.totalExpense)
    val selectedPie = data.find { it.selected } //null when nothing is picked or that category left the range

    val chipText = if (selectedPie != null) {
        val percentage = if (totalAmount == 0.0) 0.0 else selectedPie.data / totalAmount * 100
        stringResource(R.string.budget_chip_category_percent, selectedPie.label.orEmpty(), "%.2f".format(percentage))
    } else {
        stringResource(R.string.budget_chip_total)
    }
    val expenseText = CurrencyUtils.formatPesoFromDouble(selectedPie?.data ?: totalAmount)

    Column(modifier = modifier) {
        // Header: total spent, with the filter that changes it right next to it
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.budget_lbl_spent),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            "₱${CurrencyUtils.formatCents(budgetData.totalExpense)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    BudgetFilterChip(filterLabel = filterLabel, onChangeFilter = onChangeFilter)
                }
                // Full card width (not squeezed next to the filter chip), so it stays on one line
                AllowanceLine(
                    spent = budgetData.totalExpense,
                    allowance = budgetData.totalAllowance,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgetData.totalExpense == 0L) {
            EmptyChart(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
        } else {
            Box(
                contentAlignment = Alignment.Center
            ) {
                PieChart(
                    spaceDegree = 3f,
                    selectedPaddingDegree = 4f,
                    style = Pie.Style.Stroke(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    data = data,
                    onPieClick = {
                        selectedLabel = if (selectedLabel == it.label) null else it.label
                    },
                    selectedScale = 1.2f,
                    scaleAnimEnterSpec = spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    colorAnimEnterSpec = tween(300),
                    colorAnimExitSpec = tween(300),
                    scaleAnimExitSpec = tween(300),
                    spaceDegreeAnimExitSpec = tween(300),
                )

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(chipText, fontSize = 12.sp) })
                    Text(
                        text = expenseText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp)
                    )

                    Text(
                        stringResource(R.string.budget_lbl_amount),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally)
                    )
    //                Text(
    //                    "₱${CurrencyUtils.formatCents(budget)}",
    //                    style = MaterialTheme.typography.labelLargeEmphasized,
    //                    fontWeight = FontWeight.Light,
    //                    modifier = Modifier
    //                        .padding(top = 4.dp)
    //                        .align(Alignment.CenterHorizontally)
    //                )
                }
            }
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
            append(
                if (category.isEmpty()) stringResource(R.string.budget_lbl_of_allowance, allowanceText)
                else stringResource(R.string.budget_lbl_of_category_allowance, allowanceText, category)
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
                filterLabel = "This Month",
                budgetData = BudgetTotalSummaryModel()
            ) {}
        }
    }
}