package com.gmail.vondenuelle.denuspend.ui.budget.components.section

import android.view.RoundedCorner
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChartSection(
    modifier: Modifier = Modifier,
    expense : Long,
    budget : Long,
) {
    val percentage = (expense.toDouble() / budget.toDouble()) * 100
    val formatted = "%.2f".format(percentage)

    var data by remember {
        mutableStateOf(
            CategoryDropdownButtonModel.list.drop(1).mapIndexed { index, label ->
                Pie(
                    label = label,
                    data = listOf(20.0, 45.0, 35.0).getOrElse(index) { 10.0 }, // sample data
                    color = CategoryDropdownButtonModel.colors[index],
                    selectedColor = CategoryDropdownButtonModel.selectedColors[index]
                )
            }
           )
    }

    var chipText by remember {
        mutableStateOf(  "Total: $formatted%")
    }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column {
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
                        val formatted = "%.2f".format((pie.data/100) * 100)
                        "${pie.label}: $formatted%"
                    } else {
                        "Total: $formatted%"
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
                    "₱${CurrencyUtils.formatCents(expense)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                )

                Text(
                    "₱${CurrencyUtils.formatCents(budget)}",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }

        // LEGEND
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(top = 16.dp),
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            FlowColumn (
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                expense = 254145600,
                budget = 456300000
            )
        }
    }
}