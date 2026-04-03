package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.IndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.ViewRange
import kotlinx.coroutines.launch

@Composable
fun MiniAnalysisCard(
    modifier: Modifier = Modifier,
    values : List<Double> = listOf(28.0, 41.0, 5.0, 10.0, 35.0,28.0, 41.0, 5.0, 10.0, 35.0,28.0, 41.0, 5.0, 10.0, 35.0,28.0, 41.0, 5.0, 10.0, 35.0,28.0, 41.0, 5.0, 10.0, 35.0,28.0, 41.0, 5.0, 10.0, 35.0,),
    filter : List<String> = listOf("Yesterday", "Today", "Last 3 days", "Last 7 days", "Last 15 days",
        "Last 30 days"),
    onChangeFilter : (String) -> Unit,
) {
     var selectedFilterIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(selectedFilterIndex) {
        onChangeFilter(filter[selectedFilterIndex])
    }


    Column(
    ) {
        Row(modifier = modifier.height(IntrinsicSize.Min)) {
            ElevatedCard(modifier = Modifier.weight(1f)

                , shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                    ) {
                        Column(modifier = Modifier) {
                            AnimatedContent(
                                targetState = selectedFilterIndex,
                                transitionSpec = {
                                    val isForward = targetState > initialState

                                    if (isForward) {
                                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                                slideOutHorizontally( targetOffsetX = { -it }) + fadeOut()
                                    } else {
                                        slideInHorizontally(initialOffsetX ={ -it }) + fadeIn() togetherWith
                                                slideOutHorizontally( targetOffsetX={ it }) + fadeOut()
                                    }
                                },
                                label = "FilterTextAnimation"
                            ) { index ->
                                Text(
                                    filter[index],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Light
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "₱500.00",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Column(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "All time",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "₱500.00",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LineChart(
                        modifier = Modifier.height(120.dp),
                        gridProperties = GridProperties(enabled = false),
                        dividerProperties = DividerProperties(enabled = false),
                        indicatorProperties = HorizontalIndicatorProperties(enabled = false),
                        labelHelperPadding = 0.dp,
                        labelHelperProperties = LabelHelperProperties(enabled = false),
                        data = remember {
                            listOf(
                                Line(
                                    label = "",
                                    values = values,
                                    color = SolidColor(Color(0xFF23af92)),
                                    firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                                    secondGradientFillColor = Color.Transparent,
                                    strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                                    gradientAnimationDelay = 500,
                                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                                    curvedEdges = true,


                                    )
                            )
                        },
                        animationMode = AnimationMode.Together(delayBuilder = {
                            it * 500L
                        }),
                    )
                }
            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            ,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    selectedFilterIndex = if(selectedFilterIndex == 0) filter.size - 1 else selectedFilterIndex - 1
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            repeat(filter.size) { indicatorIndex ->
                val color by animateColorAsState(
                    if (selectedFilterIndex == indicatorIndex)
                        MaterialTheme.colorScheme.primary
                    else Color.Gray
                )
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(horizontal = 2.dp).size(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = {
                    selectedFilterIndex = if(selectedFilterIndex == filter.size - 1) 0 else selectedFilterIndex + 1
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.size(24.dp)
            ){
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

        }

    }



}

@Preview
@Composable
private fun MiniAnalysisCardPreview() {
    DenuSpendTheme {
        Surface() {
            MiniAnalysisCard(modifier = Modifier.fillMaxWidth(), onChangeFilter = { })
        }
    }
}