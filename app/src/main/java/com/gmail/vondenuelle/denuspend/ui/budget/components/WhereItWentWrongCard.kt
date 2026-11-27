package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

/**
 * "Where it went wrong": only the categories that went over or had unplanned spending, as small rows with chips.
 * Red chips = over a set allowance, amber = spending with no allowance. A category under its allowance with no bad days isn't listed.
 * Tapping a row opens that category's Insights.
 */
@Composable
fun WhereItWentWrongCard(
    summary: BudgetTotalSummaryModel,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = wrongRows(summary)
    if (rows.isEmpty()) return

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                stringResource(R.string.budget_lbl_where_wrong),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            rows.forEachIndexed { i, row ->
                WrongRow(row = row, onClick = { onCategoryClick(row.category) })
                if (i < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
            Text(
                stringResource(R.string.budget_lbl_where_wrong_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

private data class WrongChip(val text: String, val isOver: Boolean)
private data class WrongRowData(val category: String, val spent: Long, val chips: List<WrongChip>)

@Composable
private fun wrongRows(summary: BudgetTotalSummaryModel): List<WrongRowData> {
    val categories = (summary.totals.keys + summary.allowances.keys).distinct()
    return categories.mapNotNull { category ->
        val spent = summary.totals[category] ?: 0L
        val allowance = summary.allowances[category] ?: 0L
        val overDays = summary.overDays[category] ?: 0
        val unplannedDays = summary.unplannedDays[category] ?: 0
        val badDays = overDays + unplannedDays
        val chips = when {
            // No allowance at all: every peso of it was unplanned
            allowance == 0L && spent > 0 -> listOf(WrongChip(stringResource(R.string.budget_chip_unplanned, peso(spent)), isOver = false))
            // Over overall: how much, and on how many days
            spent > allowance -> listOf(
                WrongChip(
                    if (summary.hasDayCounts && badDays > 0) {
                        stringResource(R.string.budget_chip_over_days, peso(spent - allowance), pluralStringResource(R.plurals.budget_days, badDays, badDays))
                    } else {
                        stringResource(R.string.budget_chip_over, peso(spent - allowance))
                    },
                    isOver = true
                )
            )
            // Under overall, but some days still went wrong
            else -> buildList {
                if (overDays > 0) add(WrongChip(stringResource(R.string.budget_chip_over_on, pluralStringResource(R.plurals.budget_days, overDays, overDays)), isOver = true))
                if (unplannedDays > 0) add(WrongChip(stringResource(R.string.budget_chip_unplanned, pluralStringResource(R.plurals.budget_days, unplannedDays, unplannedDays)), isOver = false))
            }
        }
        if (chips.isEmpty()) null else WrongRowData(category, spent, chips)
    }.sortedByDescending { it.spent }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrongRow(row: WrongRowData, onClick: () -> Unit) {
    val index = categoryIndex(row.category)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
        // The name gets at most about half the row, so a long one wraps instead of squeezing the chips
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.45f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(CategoryDropdownButtonModel.colors[index], CircleShape)
            )
            Text(
                row.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        // Chips sit side by side, the second moves under the first when there's no room, and a long one wraps inside itself
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(0.55f)
        ) {
            row.chips.forEach { chip -> Chip(chip) }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun Chip(chip: WrongChip) {
    val (container, content) = if (chip.isOver) {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    } else {
        colorResource(R.color.unplanned_container) to colorResource(R.color.unplanned_text)
    }
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Text(
            chip.text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Preview
@Composable
private fun WhereItWentWrongCardPreview() {
    DenuSpendTheme {
        Surface {
            WhereItWentWrongCard(
                summary = BudgetTotalSummaryModel(
                    totals = mapOf("Household" to 786500L, "Food" to 618000L, "Transportation" to 394000L, "Personal" to 431000L),
                    allowances = mapOf("Food" to 670000L, "Transportation" to 300000L, "Personal" to 430000L),
                    dayCount = 27,
                    overDays = mapOf("Food" to 10, "Transportation" to 9, "Personal" to 1),
                    unplannedDays = mapOf("Food" to 1, "Transportation" to 2, "Household" to 4, "Personal" to 3),
                ),
                onCategoryClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

