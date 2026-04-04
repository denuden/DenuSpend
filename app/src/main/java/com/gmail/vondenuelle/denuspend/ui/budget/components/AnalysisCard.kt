package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.formatPesoFromDouble
import com.gmail.vondenuelle.denuspend.utils.getMonthByIndex
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisCard(
    modifier: Modifier = Modifier,
    title : String,
    onBarClick : (Int, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dropdownScrollState = rememberScrollState()
    val list = remember {
        listOf(
            "2024",
            "2025",
            "2026",
        )
    }
    var selectedFilter by remember { mutableIntStateOf(1) }
    val color = colorResource(R.color.chart_solid_color)

    ElevatedCard(modifier = modifier
        , shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.weight(1f))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    AssistChip(
                        onClick = {
                            //Todo
                        },
                        label = {
                            Text(list[selectedFilter])
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
                                    selectedFilter = index
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ColumnChart(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                labelHelperProperties = LabelHelperProperties(enabled = false),
                dividerProperties = DividerProperties(enabled = false),
                gridProperties = GridProperties(
                    enabled = true,
                    yAxisProperties = GridProperties.AxisProperties(enabled = false),
                    xAxisProperties = GridProperties.AxisProperties(color = SolidColor(Color.LightGray))
                ),
                labelProperties = LabelProperties(enabled = true, textStyle = TextStyle(fontSize = 11.sp)),
                popupProperties = PopupProperties(
                    containerColor = MaterialTheme.colorScheme.surface,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    contentBuilder = { _, _, value ->
                        formatPesoFromDouble(value.format(2).toDouble())
                    }
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                   enabled = false
                ),
                data = remember {
                    listOf(
                        Bars(
                            label = "Jan",
                            values = listOf(
                                Bars.Data(
                                    value = 520.43,
                                    color = SolidColor(color)
                                ),
                            ),
                        ),
                        Bars(
                            label = "Feb",
                            values = listOf(
                                Bars.Data(value = 260.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Mar",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Apr",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "May",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Jun",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Jul",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Aug",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Sep",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Oct",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Nov",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        ),
                        Bars(
                            label = "Dec",
                            values = listOf(
                                Bars.Data(value = 60.0, color = SolidColor(color))
                            ),
                        )
                    )
                },
                barProperties = BarProperties(
                    cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
                    spacing = 3.dp,
                    thickness = 20.dp
                ),
                animationMode = AnimationMode.Together(delayBuilder = {
                    it * 200L
                }),
                animationSpec = tween(
                    durationMillis = 1000,
                    delayMillis = 200,
                    easing = EaseInOutCubic
                ),
                onBarClick = {
                    onBarClick(it.dataIndex, list[selectedFilter])
                }
            )
        }
    }
}


@Preview
@Composable
private fun AnalysisCardPreview() {
    DenuSpendTheme() {
        Surface() {
            AnalysisCard(title = "Food Analysis"){ _ , _ ->}
        }
    }
}