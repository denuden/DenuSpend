package com.gmail.vondenuelle.denuspend.ui.home.components.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.home.components.tab.RecentTab
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

enum class QuickTabSectionOptions {
    RECENTS,
    BUDGET,
    GOALS,
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickTabSection(
    modifier: Modifier = Modifier,
    onChangeTab: (QuickTabSectionOptions) -> Unit,
) {
    var selected by remember { mutableStateOf(QuickTabSectionOptions.RECENTS) }

    val borderBottomColor =  MaterialTheme.colorScheme.secondary

    Column (
        modifier = modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
    ){
        LazyRow(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(QuickTabSectionOptions.entries.toTypedArray()) { option ->

                TextButton(
                    modifier = Modifier
                        .drawBehind {
                            if(selected == option) {
                                val strokeWidth = 2.dp.toPx()

                                drawLine(
                                    color = borderBottomColor,
                                    start = Offset(0f, size.height - strokeWidth / 2),
                                    end = Offset(size.width, size.height - strokeWidth / 2),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                        .height(34.dp)

                    ,
                    onClick = { onChangeTab(option) },
                ) {
                    Text(
                        when (option) {
                            QuickTabSectionOptions.RECENTS -> "Recents"
                            QuickTabSectionOptions.BUDGET -> "Budget"
                            QuickTabSectionOptions.GOALS -> "Goals"
                        },
                        color = if(selected == option) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),

                        )
                }
            }
        }

        Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            when(selected) {
                QuickTabSectionOptions.RECENTS -> RecentTab()
                else -> Unit
            }
        }

    }

}

@Preview
@Composable
private fun QuickTabSectionPreview() {
    DenuSpendTheme {
        Surface {
            QuickTabSection(
                modifier =  Modifier.fillMaxSize()
            ){}
        }
    }
}