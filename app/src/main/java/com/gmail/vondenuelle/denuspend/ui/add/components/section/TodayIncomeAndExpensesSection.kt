package com.gmail.vondenuelle.denuspend.ui.add.components.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.getDateNow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodayIncomeAndExpensesSection(
    modifier: Modifier = Modifier,
    progress: Float,
    expense: String,
    totalBudget: String,
) {
    val dateToday = remember { getDateNow("dd MMMM yyyy") }
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            CircularProgressIndicator(
                progress = {
                    progress
                },
                strokeWidth = 10.dp,
                modifier = Modifier
                    .size(148.dp)
            )
            ElevatedCard(
                modifier = Modifier.size(134.dp),
                shape = CircleShape,
                onClick = {}
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Today".uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            dateToday,
                            fontWeight = FontWeight.Light,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Text(
            expense,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        Text(
            "of $totalBudget",
            style = MaterialTheme.typography.labelLargeEmphasized,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .padding(top = 4.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Preview
@Composable
private fun TodayIncomeAndExpensesSectionPreview() {
    DenuSpendTheme {
        Surface {
            TodayIncomeAndExpensesSection(
                modifier = Modifier.fillMaxSize(),
                progress = 0.65f,
                expense = "₱1,456.00",
                totalBudget = "₱3,000.00"
            )
        }
    }
}