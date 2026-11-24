package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.ui.common.components.InformationTooltip
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * "Statements" on a credit card's details: the open statement and the last closed one, as two separate cards that are never added together
 * (like a bank app). Both come from one listener that starts at the open cycle's first day, since the previous statement's
 * balance is worked out from today's owed amount minus what happened after it closed.
 */
@Composable
fun CardStatementSummaries(
    modifier: Modifier = Modifier,
    card: WalletModel,
    transactions: List<TransactionModel>,
    movements: List<WalletMovementModel>,
    hasLoaded: Boolean,
    hasError: Boolean,
    onOpenStatement: (monthsBack: Int) -> Unit,
) {
    val today = LocalDate.now()
    val current = CardStatementHelper.cycle(card.statementDay, 0, today)
    val previous = CardStatementHelper.cycle(card.statementDay, 1, today)
    val totals = CardStatementHelper.totals(transactions, movements)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.wallet_lbl_statements),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            InformationTooltip(
                richTooltipSubheadText = stringResource(R.string.wallet_tip_statements_title),
                richTooltipText = stringResource(R.string.wallet_tip_statements_text)
            )
        }

        when {
            hasError -> Text(
                stringResource(R.string.wallet_lbl_statement_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            !hasLoaded -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> {
                CurrentStatementCard(cycle = current, today = today, totals = totals, onClick = { onOpenStatement(0) })
                // Everything in the listener is after the previous close, so it's exactly what moved the balance since then
                val statementBalance = CardStatementHelper.statementBalance(card, totals)
                PreviousStatementCard(
                    cycle = previous,
                    statementBalance = statementBalance,
                    paidSince = totals.payments,
                    isFromEnteredAmount = CardStatementHelper.toLocalDate(card.createdAt).isAfter(previous.end),
                    onClick = { onOpenStatement(1) }
                )
            }
        }
    }
}

@Composable
private fun CurrentStatementCard(
    cycle: CardStatementHelper.Cycle,
    today: LocalDate,
    totals: CardStatementHelper.Totals,
    onClick: () -> Unit,
) {
    val daysLeft = ChronoUnit.DAYS.between(today, cycle.end).toInt()
    val closes = if (daysLeft <= 0) stringResource(R.string.wallet_lbl_statement_closes_today)
    else pluralStringResource(R.plurals.wallet_lbl_statement_closes_in, daysLeft, daysLeft)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryTagRow(
                tag = stringResource(R.string.wallet_lbl_statement_current_tag),
                tagColor = MaterialTheme.colorScheme.primary,
                tagTextColor = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                stringResource(R.string.wallet_lbl_statement_name, CardStatementHelper.formatDay(cycle.end)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                stringResource(
                    R.string.wallet_lbl_statement_period_open,
                    stringResource(R.string.wallet_lbl_statement_period, CardStatementHelper.formatDay(cycle.start), CardStatementHelper.formatDay(cycle.end)),
                    closes
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                formatBalance(totals.charges),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                pluralStringResource(R.plurals.wallet_lbl_statement_charge_count, totals.chargeCount, totals.chargeCount),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// Still unpaid never goes below ₱0: paying more than the bill lowers "Owed now" instead, like a bank's credit balance
@Composable
private fun PreviousStatementCard(
    cycle: CardStatementHelper.Cycle,
    statementBalance: Long,
    paidSince: Long,
    isFromEnteredAmount: Boolean,
    onClick: () -> Unit,
) {
    val stillUnpaid = (statementBalance - paidSince).coerceAtLeast(0L)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryTagRow(
                tag = stringResource(R.string.wallet_lbl_statement_previous_tag),
                tagColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                tagTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.wallet_lbl_statement_name, CardStatementHelper.formatDay(cycle.end)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )
            AmountRow(stringResource(R.string.wallet_lbl_statement_balance), formatBalance(statementBalance))
            AmountRow(
                stringResource(R.string.wallet_lbl_paid_since, CardStatementHelper.formatDay(cycle.end)),
                formatBalance(paidSince),
                // A payment is money you put in, not a negative: green like money in, no minus
                valueColor = if (paidSince > 0) colorResource(R.color.verified_green_text) else MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
            AmountRow(
                stringResource(R.string.wallet_lbl_still_unpaid),
                formatBalance(stillUnpaid),
                valueColor = if (stillUnpaid > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (isFromEnteredAmount) {
                Text(
                    stringResource(R.string.wallet_lbl_statement_from_entered),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryTagRow(tag: String, tagColor: Color, tagTextColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tagTextColor,
            modifier = Modifier
                .background(tagColor, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Box(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}

@Composable
fun AmountRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Preview
@Composable
private fun CardStatementSummariesPreview() {
    DenuSpendTheme {
        Surface {
            CardStatementSummaries(
                card = WalletModel(docId = "c", name = "Visa card", type = WalletModel.TYPE_CREDIT_CARD, balance = -1234000, creditLimit = 5000000, statementDay = 28),
                transactions = listOf(TransactionModel(docId = "1", title = "Groceries", category = "Food", amount = -182000)),
                movements = listOf(WalletMovementModel(docId = "2", title = "Card payment", amount = 782000)),
                hasLoaded = true,
                hasError = false,
                onOpenStatement = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
