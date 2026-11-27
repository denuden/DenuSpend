package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetMoneyModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

/**
 * Real money that came in (salary, gifts, refunds… added with "Add money"), next to what went out.
 * Dark like Home's balance card, because this is real money, not allowance.
 */
@Composable
fun MoneyInCard(
    money: BudgetMoneyModel,
    moneyOut: Long, // the Money out card's total
    modifier: Modifier = Modifier,
) {
    val difference = money.moneyIn - moneyOut

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(stringResource(R.string.budget_lbl_money_in), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    peso(money.moneyIn),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    money.biggestIn?.let { stringResource(R.string.budget_lbl_money_in_biggest, it.title, peso(it.amount)) }
                        ?: stringResource(R.string.budget_lbl_money_in_none),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            Text(
                when {
                    difference < 0 -> stringResource(R.string.budget_lbl_more_out, peso(-difference))
                    difference > 0 -> stringResource(R.string.budget_lbl_more_in, peso(difference))
                    else -> stringResource(R.string.budget_lbl_in_out_same)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
private fun MoneyInCardPreview() {
    DenuSpendTheme {
        Surface {
            MoneyInCard(
                money = BudgetMoneyModel(moneyIn = 1400000L, moneyInCount = 1, biggestIn = WalletMovementModel(title = "Payroll", amount = 1400000L)),
                moneyOut = 3248633L,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
