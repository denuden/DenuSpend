package com.gmail.vondenuelle.denuspend.ui.add.components.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.getDateNow
import kotlin.math.roundToInt

// Compact card: a small ring with the % used on the left, today's numbers on the right.
// It replaced the big centered ring so the list below gets more room.
@Composable
fun TodayIncomeAndExpensesSection(
    modifier: Modifier = Modifier,
    progress: Float,
    expense: String,
    totalBudget: String,
    remaining: String, //already formatted, e.g. "₱3,870.00 left" or "₱200.00 over"
    isOverspent: Boolean = false,
) {
    // Not remembered, so it shows the new day when the listener restarts after midnight
    val dateToday = getDateNow("dd MMM")
    val ringColor = if (isOverspent) MaterialTheme.colorScheme.error else ProgressIndicatorDefaults.circularColor

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    strokeWidth = 6.dp,
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.add_lbl_today_date, dateToday),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.add_lbl_amount_spent, expense),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        stringResource(R.string.add_lbl_of_total, totalBudget) + " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        remaining,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverspent) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TodayIncomeAndExpensesSectionPreview() {
    DenuSpendTheme {
        Surface {
            TodayIncomeAndExpensesSection(
                progress = 0.08f,
                expense = "₱330.00",
                totalBudget = "₱4,200.00",
                remaining = "₱3,870.00 left"
            )
        }
    }
}
