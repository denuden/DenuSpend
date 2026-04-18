package com.gmail.vondenuelle.denuspend.ui.budget.components.section

import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.longToAmountDouble
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChartSection(
    modifier: Modifier = Modifier,
    filter : String,
    budgetData : BudgetTotalSummaryModel,
    onChangeFilter : (String) -> Unit
) {
    var data by remember(budgetData) {
        mutableStateOf(
            CategoryDropdownButtonModel.list.drop(1).mapIndexed { index, label ->

                val amount = when (index) {
                    0 -> budgetData.food
                    1 -> budgetData.entertainment
                    2 -> budgetData.household
                    3 -> budgetData.transportation
                    4 -> budgetData.workEducation
                    5 -> budgetData.healthcare
                    6 -> budgetData.personal
                    7 -> budgetData.family
                    8 -> budgetData.others
                    else -> 0L
                }

                Triple(index, label, amount)
            }
                .filter { it.third > 0 }
                .map { (index, label, amount) ->
                    Pie(
                        label = label,
                        data = longToAmountDouble(amount),
                        color = CategoryDropdownButtonModel.colors[index],
                        selectedColor = CategoryDropdownButtonModel.selectedColors[index]
                    )
                }
       )
    }

    var chipText by remember {
        mutableStateOf(  "Total: 100%")
    }
    var expenseText by remember(budgetData.totalExpense) {
        mutableStateOf(
            "₱${CurrencyUtils.formatCents(budgetData.totalExpense)}",
        )
    }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
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
                    val clickedIndex = data.indexOf(it)

                    selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex

                    data = data.mapIndexed { index, pie ->
                        pie.copy(selected = index == selectedIndex)
                    }

                    chipText = if (selectedIndex != null) {
                        val pie = data[selectedIndex!!]
                        expenseText = CurrencyUtils.formatPesoFromDouble(pie.data)

                        val percentage = if (budgetData.totalExpense == 0L) {
                            0.0
                        } else {
                            (pie.data / (budgetData.totalExpense / 100)) * 100
                        }

                        val formatted = "%.2f".format(percentage)
                        "${pie.label}: $formatted%"
                    } else {
                        expenseText = "₱${CurrencyUtils.formatCents(budgetData.totalExpense)}"
                        "Total: 100%"

                    }
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
                    "Amount",
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

        Spacer(modifier = Modifier.height(16.dp))


        var expanded by remember { mutableStateOf(false) }
        val dropdownScrollState = rememberScrollState()

        val list = BudgetChartFilter.list

        LaunchedEffect(expanded) {
            if (expanded) {
                // Scroll to show the bottom menu items.
                dropdownScrollState.scrollTo(dropdownScrollState.maxValue)
            }
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            AssistChip(
                onClick = {
                    //Todo
                },
                label = {
                    Text(filter)
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
                list.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            expanded = false
                            onChangeFilter(option)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        // LEGEND
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(top = 16.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            FlowColumn (
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                maxItemsInEachColumn = 3,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CategoryDropdownButtonModel.list.drop(1).mapIndexed { index, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier= Modifier.size(16.dp).background(color = CategoryDropdownButtonModel.colors[index], shape = RoundedCornerShape(8.dp)),)
                        Text(label, modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }

            }
        }
    }

}

@Preview
@Composable
private fun ChartSectionPreview() {
    DenuSpendTheme() {
        Surface() {
            ChartSection(
                modifier = Modifier,
                filter = "This month",
                budgetData = BudgetTotalSummaryModel()
            ) {}
        }
    }
}