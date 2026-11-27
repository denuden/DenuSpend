package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BillRow
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetBillsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

/**
 * "How it went": up to 4 plain sentences about the whole range, so the answer comes before the numbers.
 * Each line is a simple rule over numbers already on screen (no extra reads, no AI). A line that doesn't apply is skipped.
 */
@Composable
fun HowItWentCard(
    summary: BudgetTotalSummaryModel,
    bills: BudgetBillsModel,
    modifier: Modifier = Modifier,
) {
    val lines = howItWentLines(summary, bills)
    if (lines.isEmpty()) return

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                stringResource(R.string.budget_lbl_how_it_went),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .background(line.container, CircleShape)
                    ) {
                        Icon(line.icon, contentDescription = null, tint = line.tint, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        line.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

private data class HowLine(val icon: ImageVector, val text: String, val tint: Color, val container: Color)

@Composable
private fun howItWentLines(summary: BudgetTotalSummaryModel, bills: BudgetBillsModel): List<HowLine> {
    if (summary.totalExpense == 0L) return emptyList()
    val bad = HowLine(Icons.Filled.Warning, "", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
    val good = HowLine(Icons.Filled.CheckCircle, "", colorResource(R.color.verified_green_text), colorResource(R.color.verified_green_container))
    val info = HowLine(Icons.Filled.PieChart, "", MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)
    val lines = mutableListOf<HowLine>()
    val spent = summary.totalExpense
    val allowance = summary.totalAllowance

    // 1. Spent vs the allowance set
    lines += when {
        allowance == 0L -> bad.copy(text = stringResource(R.string.budget_how_no_allowance, peso(spent)))
        spent > allowance -> bad.copy(text = stringResource(R.string.budget_how_over, peso(spent), peso(spent - allowance), peso(allowance)))
        else -> good.copy(text = stringResource(R.string.budget_how_under, peso(spent), peso(allowance - spent), peso(allowance)))
    }

    // 2. The biggest category, and whether any of it was planned
    summary.totals.maxByOrNull { it.value }?.let { (category, amount) ->
        val share = percentOf(amount, spent)
        lines += info.copy(
            text = if ((summary.allowances[category] ?: 0L) == 0L) stringResource(R.string.budget_how_top_unplanned, category, peso(amount), share)
            else stringResource(R.string.budget_how_top, category, peso(amount), share)
        )
    }

    // 3. The category with the most bad days (over + unplanned), or the good news. Unknown on old months without day marks.
    if (summary.hasDayCounts && summary.dayCount > 0) {
        val worst = (summary.overDays.keys + summary.unplannedDays.keys)
            .associateWith { (summary.overDays[it] ?: 0) + (summary.unplannedDays[it] ?: 0) }
            .maxByOrNull { it.value }
        when {
            worst != null && worst.value > 0 -> lines += bad.copy(text = stringResource(R.string.budget_how_bad_days, worst.key, worst.value, summary.dayCount))
            allowance > 0 -> lines += good.copy(text = stringResource(R.string.budget_how_all_good, summary.dayCount))
        }
    }

    // 4. Card bills on top of everyday spending
    if (bills.total > 0) {
        lines += info.copy(
            icon = Icons.Filled.CreditCard,
            text = stringResource(R.string.budget_how_bills, peso(bills.total), billNames(bills.rows, stringResource(R.string.budget_how_list_and)))
        )
    }
    return lines
}

// "Laptop, Claude Pro, Netflix and YouTube Premium", biggest first, at most 4 names ([andFormat] comes from strings.xml)
private fun billNames(rows: List<BillRow>, andFormat: String): String {
    val names = rows.map { it.name }.distinct().take(4)
    return if (names.size <= 1) names.joinToString() else andFormat.format(names.dropLast(1).joinToString(", "), names.last())
}

@Preview
@Composable
private fun HowItWentCardPreview() {
    DenuSpendTheme {
        Surface {
            HowItWentCard(
                summary = BudgetTotalSummaryModel(
                    totals = mapOf("Household" to 786500L, "Food" to 618000L, "Transportation" to 394000L),
                    totalExpense = 2229500L,
                    allowances = mapOf("Food" to 670000L, "Transportation" to 300000L),
                    totalAllowance = 1400000L,
                    dayCount = 27,
                    overDays = mapOf("Food" to 10, "Transportation" to 11),
                    unplannedDays = mapOf("Food" to 1, "Household" to 4),
                ),
                bills = BudgetBillsModel(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
