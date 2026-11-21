package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils

private data class TodayCategory(val name: String, val color: Color, val allowance: Long, val spent: Long)

// One chip per category that has an allowance or spending today, in dropdown order. Tapping one opens its Insights.
// A category that isn't in the dropdown list gets no chip, but still counts in the card's totals (same as Budget).
@Composable
fun TodayCategoryChips(
    modifier: Modifier = Modifier,
    allowanceByCategory: Map<String, Long>,
    expenseByCategory: Map<String, Long>,
    onCategoryClick: (String) -> Unit,
) {
    val categories = remember(allowanceByCategory, expenseByCategory) {
        CategoryDropdownButtonModel.list.drop(1).mapIndexedNotNull { index, name ->
            val allowance = allowanceByCategory[name] ?: 0L
            val spent = expenseByCategory[name] ?: 0L
            if (allowance == 0L && spent == 0L) null
            else TodayCategory(name, CategoryDropdownButtonModel.colors[index], allowance, spent)
        }
    }
    if (categories.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        items(categories, key = { it.name }) { category ->
            CategoryChip(category = category, onClick = { onCategoryClick(category.name) })
        }
    }
}

@Composable
private fun CategoryChip(category: TodayCategory, onClick: () -> Unit) {
    val isOver = category.spent > category.allowance
    val left = "₱${CurrencyUtils.formatCents(if (isOver) category.spent - category.allowance else category.allowance - category.spent)}"

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isOver) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (isOver) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(category.color, CircleShape))
                Text(category.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Text(
                stringResource(if (isOver) R.string.home_lbl_amount_over else R.string.home_lbl_amount_left, left),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Preview
@Composable
private fun TodayCategoryChipsPreview() {
    DenuSpendTheme {
        Surface {
            TodayCategoryChips(
                allowanceByCategory = mapOf("Food" to 40000L, "Transportation" to 20000L, "Personal" to 30000L),
                expenseByCategory = mapOf("Food" to 28000L, "Transportation" to 23000L),
                onCategoryClick = {}
            )
        }
    }
}
