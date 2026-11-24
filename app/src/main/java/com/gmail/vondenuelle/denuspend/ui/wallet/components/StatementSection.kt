package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import java.time.LocalDate

// One line on the statement. amount is always positive; isCredit = a payment or credit (shown green, no minus: it pays the balance down).
data class StatementRow(
    val key: String,
    val date: LocalDate,
    val title: String,
    val subtitle: String,
    val amount: Long,
    val isCredit: Boolean = false,
)

/**
 * One group on the Statement screen, like a section of a bank's statement: "Purchases · 3 ... ₱3,316.00", then its lines.
 * Display only: edits and deletes still happen from Add/History (expenses) or the card's details (money in/out).
 */
@Composable
fun StatementSection(
    modifier: Modifier = Modifier,
    title: String,
    rows: List<StatementRow>,
    isCredit: Boolean = false,
) {
    val green = colorResource(R.color.verified_green_text)
    val total = rows.sumOf { it.amount }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.wallet_lbl_section_header, title, rows.size),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatBalance(total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) green else MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))

        rows.forEach { row ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    CardStatementHelper.formatDay(row.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp).padding(top = 2.dp)
                )
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(row.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (row.subtitle.isNotBlank()) {
                        Text(
                            row.subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    formatBalance(row.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (row.isCredit) green else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview
@Composable
private fun StatementSectionPreview() {
    DenuSpendTheme {
        Surface {
            StatementSection(
                title = "Purchases",
                rows = listOf(
                    StatementRow("1", LocalDate.of(2026, 11, 22), "Groceries", "Food", 182000),
                    StatementRow("2", LocalDate.of(2026, 11, 3), "Coffee", "Food", 23000),
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
