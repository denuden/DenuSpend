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
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.formatCents
import com.gmail.vondenuelle.denuspend.utils.formatFirebaseDate
import com.gmail.vondenuelle.denuspend.utils.formatIsoDate
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionItem(
    modifier: Modifier = Modifier,
    transactionModel: TransactionModel
) {
    Column(
        modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if(
                transactionModel.amount < 0
            ) {
                ExpenseIcon(modifier = Modifier.padding(end = 8.dp))
            } else {
                IncomeIcon(modifier = Modifier.padding(end = 8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    transactionModel.title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    transactionModel.description,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }

            val formattedAmount = if (transactionModel.amount < 0) {
                "-₱${formatCents(-transactionModel.amount)}" //add negative to make it positive Long
            } else {
                "₱${formatCents(transactionModel.amount)}"
            }

            Text(
                formattedAmount,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp
            )
        }

        Text(
            formatFirebaseDate(transactionModel.date.toDate(), format = "dd MMMM yyyy"),
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.labelLargeEmphasized,
            modifier = Modifier
                .align(
                    Alignment.CenterHorizontally
                )
                .padding(top = 6.dp, bottom = 8.dp)
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(1.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))
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
            TransactionItem(
                transactionModel = TransactionModel(
                    title = "From my checking account",
                    description = "Transfer",
                    category = "Food",
                    date = Timestamp.now()
                )
            )
        }
    }
}