package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BillRow
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetBillsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.NextBill
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance
import com.gmail.vondenuelle.denuspend.ui.wallet.components.repeatText
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import java.time.LocalDate

/**
 * "Bills & subscriptions" under the breakdown: what was charged to cards in the range, one row per subscription or
 * installment, and the next charge. Kept apart from "Spent" because card charges aren't in the allowance.
 */
@Composable
fun BudgetBillsCard(
    modifier: Modifier = Modifier,
    bills: BudgetBillsModel,
    emptyText: String,
    onOpenCard: (walletId: String) -> Unit,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.budget_lbl_bills),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.budget_lbl_bills_not_allowance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                if (bills.rows.isEmpty()) {
                    Text(
                        emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.budget_lbl_bills_charged),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(formatBalance(bills.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            pluralStringResource(R.plurals.budget_lbl_bills_charges, bills.chargeCount, bills.chargeCount) + "\n" +
                                pluralStringResource(R.plurals.budget_lbl_bills_cards, bills.cardCount, bills.cardCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                    bills.rows.forEach { row ->
                        BillRowItem(row = row, onClick = { onOpenCard(row.walletId) })
                    }
                }

                bills.nextUp?.let { next ->
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        stringResource(
                            R.string.budget_lbl_bills_next_up,
                            next.name,
                            formatBalance(next.amount),
                            CardStatementHelper.formatDay(next.date),
                            next.cardName
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// "Visa · every month · next Nov 30", "BPI card · 6 of 24 · next Dec 25", "Visa · 3 of 3 · paid off".
// A closed card's rows can't be opened, so they're faded like paid-off rows on the card screen.
@Composable
private fun BillRowItem(row: BillRow, onClick: () -> Unit) {
    val schedule = row.schedule
    val isCardClosed = row.cardName.isEmpty()
    val cardName = row.cardName.ifEmpty { stringResource(R.string.budget_lbl_bills_closed_card) }
    val parts = buildList {
        add(cardName)
        if (row.isInstallment) {
            if (row.lastNote.isNotEmpty()) add(row.lastNote)
            if (schedule?.endedReason == RecurringChargeModel.ENDED_PAID_OFF) add(stringResource(R.string.budget_lbl_bills_paid_off))
        } else if (schedule != null && row.nextDate != null) {
            add(repeatText(schedule).lowercase())
        }
        if (row.nextDate != null) add(stringResource(R.string.budget_lbl_bills_next, CardStatementHelper.formatDay(row.nextDate)))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCardClosed, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(if (isCardClosed) 0.6f else 1f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            Icon(
                if (row.isInstallment) Icons.Filled.EventRepeat else Icons.Filled.Autorenew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        row.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Charged more than once in the range (a weekly one in a month, or a long range)
                    if (row.count > 1) {
                        Text(
                            stringResource(R.string.budget_lbl_bills_times, row.count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(formatBalance(row.total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                parts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.isInstallment && schedule != null && schedule.totalCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (schedule.paidBefore + schedule.chargeCount).toFloat() / schedule.totalCount },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun BudgetBillsCardPreview() {
    DenuSpendTheme {
        Surface {
            BudgetBillsCard(
                modifier = Modifier.padding(16.dp),
                bills = BudgetBillsModel(
                    hasCards = true,
                    total = 573333L,
                    chargeCount = 4,
                    cardCount = 2,
                    rows = listOf(
                        BillRow("a", "Visa", "Laptop", 333333L, 1, true, "3 of 3", null, null),
                        BillRow("b", "BPI card", "Phone", 150000L, 1, true, "6 of 24", null, null),
                        BillRow("a", "Visa", "AI assistant", 110000L, 1, false, "", RecurringChargeModel(name = "AI assistant"), LocalDate.of(2026, 11, 30)),
                        BillRow("a", "", "Music", 30000L, 2, false, "", null, null),
                    ),
                    nextUp = NextBill("Phone", "BPI card", 150000L, LocalDate.of(2026, 11, 28)),
                ),
                emptyText = "No bills this month",
                onOpenCard = {},
            )
        }
    }
}
