package com.gmail.vondenuelle.denuspend.ui.home.components.section

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MonthlySpendingSection(
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { /*TODO*/ },
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (true) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "24, October 2026",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = null,
                    )

                }
            }
            Text(text = stringResource(R.string.home_lbl_total_spent_this_month))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₱100.00",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(
                        0.7.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .padding(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = {
                        0.56f
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                )
            }

            if (true) {
                Text(
                    text = "You're almost at your limit!",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }


        }
    }
}

@Preview
@Composable
private fun MonthlySpendingCardPreview() {
    DenuSpendTheme {
        Surface {
            MonthlySpendingSection()
        }
    }
}