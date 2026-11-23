package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

private const val LAST_ADDED_COUNT = 3

// Today's newest few transactions, display only (editing stays in the Add tab). The whole card opens the Add tab, which lists all of today.
@Composable
fun LastAddedCard(
    modifier: Modifier = Modifier,
    transactions: List<TransactionModel>, // newest first
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_lbl_last_added),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.home_btn_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            when {
                isLoading -> Text(
                    stringResource(R.string.lbl_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                transactions.isEmpty() -> Text(
                    stringResource(R.string.home_lbl_nothing_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                else -> Column(modifier = Modifier.padding(top = 12.dp)) {
                    transactions.take(LAST_ADDED_COUNT).forEach {
                        TransactionItem(transactionModel = it) // no onClick = not clickable, so taps reach the card
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun LastAddedCardPreview() {
    DenuSpendTheme {
        Surface {
            LastAddedCard(
                transactions = listOf(
                    TransactionModel(title = "Jeepney", category = "Transportation", amount = -3000),
                    TransactionModel(title = "Merienda", category = "Food", amount = -10000),
                ),
                isLoading = false,
                onClick = {}
            )
        }
    }
}
