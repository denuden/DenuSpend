package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionItem(modifier: Modifier = Modifier) {
    Column(modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            IncomeIcon(modifier = Modifier.padding(end = 8.dp))

            Column(
                modifier = Modifier.weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text("From my checking account", fontWeight = FontWeight.W400)
                Text("Transfer", fontWeight = FontWeight.Light, style= MaterialTheme.typography.labelMediumEmphasized)
            }

            Text("₱500.00", fontWeight = FontWeight.Bold)
        }

        Text("01 March 2026", fontWeight = FontWeight.Light, style= MaterialTheme.typography.labelMedium, modifier = Modifier.align(
            Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 12.dp))
    }
}

@Composable
fun IncomeIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = colorResource(R.color.verified_green_container), shape = CircleShape).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_payment_arrow_down),
            contentDescription = null,
            tint = colorResource(R.color.verified_green)
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