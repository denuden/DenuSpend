package com.gmail.vondenuelle.denuspend.ui.add.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun AddButtonsSection(
    modifier: Modifier = Modifier,
    onIncomeClick : () -> Unit,
    onExpenseClick : () -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        FilledTonalButton(
            onClick = {
                onExpenseClick()
            },
            colors = ButtonDefaults.filledTonalButtonColors()
                .copy(containerColor = colorResource(R.color.not_verified_red_container))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_credit_card),
                    contentDescription = null,
                    tint = colorResource(R.color.not_verified_red)
                )
                Text("Add Expense", modifier = Modifier.padding(start = 12.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = {
                onIncomeClick()
            },
            colors = ButtonDefaults.filledTonalButtonColors()
                .copy(containerColor = colorResource(R.color.verified_green_container))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_payment_arrow_down),
                    contentDescription = null,
                    tint = colorResource(R.color.verified_green)
                )
                Text("Add Income", modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Preview
@Composable
private fun AddButtonsSectionPreview() {
    DenuSpendTheme {
        Surface {
            AddButtonsSection(
                modifier = Modifier.fillMaxSize()
                , onIncomeClick = {}
            ){}
        }
    }
}