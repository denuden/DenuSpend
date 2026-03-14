package com.gmail.vondenuelle.denuspend.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionItem(modifier: Modifier = Modifier) {
    Column(
        modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            ExpenseIcon(modifier = Modifier.padding(end = 8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    "From my checking account",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Transfer",
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }

            Text(
                "₱500.00",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp
            )
        }

        Text(
            "01 March 2026",
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.labelLargeEmphasized,
            modifier = Modifier
                .align(
                    Alignment.CenterHorizontally
                )
                .padding(top = 10.dp, bottom = 12.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(1.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun IncomeIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = colorResource(R.color.verified_green_container),
                shape = CircleShape
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_payment_arrow_down),
            contentDescription = null,
            tint = colorResource(R.color.verified_green)
        )
    }
}

@Composable
fun ExpenseIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = colorResource(R.color.not_verified_red_container),
                shape = CircleShape
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_credit_card),
            contentDescription = null,
            tint = colorResource(R.color.not_verified_red)
        )
    }
}

@Preview
@Composable
private fun TransactionItemPreview() {
    DenuSpendTheme {
        Surface {
            TransactionItem()
        }
    }
}