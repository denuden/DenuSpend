package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.formatPesoFromDouble
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.extensions.format
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.VerticalIndicatorProperties

@Composable
fun PerDayAnalysisCard(modifier: Modifier = Modifier, title : String, numberOfDays : Int, onBarClick: (String) -> Unit) {
    val color = colorResource(R.color.chart_solid_color)

    ElevatedCard(modifier = modifier
        , shape = MaterialTheme.shapes.extraLarge) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium,)
                Spacer(modifier = Modifier.weight(1f)
                )
                Text("₱1,400.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.medium
                        ).padding(vertical = 8.dp, horizontal = 16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            RowChart(
                modifier = Modifier.fillMaxWidth().height(700.dp),
                labelHelperProperties = LabelHelperProperties(enabled = false),
                dividerProperties = DividerProperties(enabled = false),
                gridProperties = GridProperties(                    enabled = true,
                    yAxisProperties = GridProperties.AxisProperties(color = SolidColor(Color.LightGray)),
                    xAxisProperties = GridProperties.AxisProperties(enabled = false, )
                ),
                labelProperties = LabelProperties(enabled = true, textStyle = TextStyle(fontSize = 11.sp)),
                popupProperties = PopupProperties(
                    containerColor = MaterialTheme.colorScheme.surface,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    contentBuilder = { _, _, value ->
                        formatPesoFromDouble(value.format(2).toDouble())
                    }
                ),
                indicatorProperties = VerticalIndicatorProperties(
                    enabled = false
                ),
                data = remember {
                    (1..numberOfDays).map { num ->
                        Bars(
                            label = "$num",
                            values = listOf(
                                Bars.Data(
                                    value = 520.43,
                                    color = SolidColor(color)
                                )
                            )
                        )
                    }
                },
                barProperties = BarProperties(
                    cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, bottomRight = 6.dp),
                    spacing = 2.dp,
                    thickness = 12.dp
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
                    onBarClick("${it.dataIndex + 1} $title") //31 Jan, 2025
                }
            )
        }
    }
}

@Preview
@Composable
private fun PerDayAnalysisCardPreview() {
    DenuSpendTheme() {
        Surface() {
            PerDayAnalysisCard(title = "Jan, 2025", numberOfDays = 31){ _ ->}
        }
    }
}