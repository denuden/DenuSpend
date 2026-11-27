package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetMoneyModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.AllowanceLine
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.BudgetFilterChip
import com.gmail.vondenuelle.denuspend.ui.common.components.InformationTooltip
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The top of the Budget tab: everything that really left your wallets in the range, with the filter next to it.
 * Everyday spending (expenses, the allowance math) + card bills + other money out (fees, emergencies…). Transfers never count.
 */
@Composable
fun MoneyOutCard(
    summary: BudgetTotalSummaryModel,
    money: BudgetMoneyModel,
    filterLabel: String,
    onChangeFilter: (BudgetChartFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bills = money.bills.total
    val total = summary.totalExpense + bills + money.otherOut

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.budget_lbl_money_out),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                BudgetFilterChip(filterLabel = filterLabel, onChangeFilter = onChangeFilter)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    peso(total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // Allowance is a made-up daily limit, not money, so the same explainer as Home and Wallet (why this total ≠ allowance)
                Box {
                    InformationTooltip(
                        richTooltipSubheadText = stringResource(R.string.lbl_wallet_vs_allowance_title),
                        richTooltipText = stringResource(R.string.lbl_wallet_vs_allowance_text)
                    )
                }
            }
            rangeText(summary.startDate, summary.endDate)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // The divider sits above all three rows, so they read as the parts of the total (not Everyday as the total)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, end = 8.dp))

            // Everyday spending, with how it did against the allowance right under it
            Column(modifier = Modifier.padding(end = 8.dp)) {
                MoneyRow(label = stringResource(R.string.budget_lbl_everyday), amount = summary.totalExpense)
                AllowanceLine(
                    spent = summary.totalExpense,
                    allowance = summary.totalAllowance,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (bills > 0 || money.otherOut > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 14.dp, end = 8.dp)
                ) {
                    if (bills > 0) MoneyRow(label = stringResource(R.string.budget_lbl_card_bills), amount = bills)
                    if (money.otherOut > 0) MoneyRow(label = stringResource(R.string.budget_lbl_other_out), amount = money.otherOut)
                }
            }
        }
    }
}

@Composable
private fun MoneyRow(label: String, amount: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(peso(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// "Nov 1 – 27", with the year only when it isn't this year
private fun rangeText(start: LocalDate?, end: LocalDate?): String? {
    if (start == null || end == null) return null
    val locale = Locale.getDefault()
    val withYear = start.year != LocalDate.now().year || end.year != LocalDate.now().year
    val startText = start.format(DateTimeFormatter.ofPattern(if (withYear && start.year != end.year) "MMM d, yyyy" else "MMM d", locale))
    val endPattern = when {
        withYear -> if (start.month == end.month && start.year == end.year) "d, yyyy" else "MMM d, yyyy"
        start.month == end.month -> "d"
        else -> "MMM d"
    }
    return if (start == end) startText else "$startText – ${end.format(DateTimeFormatter.ofPattern(endPattern, locale))}"
}

@Preview
@Composable
private fun MoneyOutCardPreview() {
    DenuSpendTheme {
        Surface {
            MoneyOutCard(
                summary = BudgetTotalSummaryModel(
                    totalExpense = 2229500L, totalAllowance = 1400000L,
                    startDate = LocalDate.of(2026, 11, 1), endDate = LocalDate.of(2026, 11, 27)
                ),
                money = BudgetMoneyModel(otherOut = 500000L),
                filterLabel = "This Month",
                onChangeFilter = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
