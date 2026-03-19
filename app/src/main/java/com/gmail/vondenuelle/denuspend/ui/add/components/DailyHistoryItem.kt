package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DailyHistoryItem(
    modifier: Modifier = Modifier,
    expense : String,
    totalBudget : String,
    date : String,
    onClick : () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        onClick = {
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
            ) {
                Text(
                    expense,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
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

            Spacer(modifier = Modifier.weight(1f))

            Text(
                date,
                fontWeight = FontWeight.Light,
                style = MaterialTheme.typography.labelLarge)
        }

    }
}

@Preview
@Composable
private fun DailyHistoryItemPreview() {
    DenuSpendTheme(){
        Surface() {
            DailyHistoryItem(
                modifier = Modifier.fillMaxWidth(),
                expense ="₱1,456.00",
                totalBudget = "₱3,000.00",
                date = "19 March 2026"
            ){}
        }
    }
}