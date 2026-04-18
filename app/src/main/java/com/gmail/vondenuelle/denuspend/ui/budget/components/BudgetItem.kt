package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun BudgetItem(
    modifier: Modifier = Modifier,
    category : String,
    transactionCount : Int,
    icon : @Composable () -> Unit,
    onClick : () -> Unit,
) {
    ElevatedCard (
        modifier = modifier,
        onClick = { onClick() },
        colors = CardDefaults.elevatedCardColors().copy(containerColor = colorResource(R.color.light_white)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = CircleShape).padding(16.dp)
            ) {
                icon()
            }

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$transactionCount Transaction(s) made",style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Preview
@Composable
private fun BudgetItemPreview() {
    DenuSpendTheme() {
        Surface() {
            BudgetItem(
                category = "Food",
                transactionCount = 5,
                onClick = {},
                icon = {}
            )
        }
    }
}