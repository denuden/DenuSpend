package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils

private data class CategoryToday(val name: String, val icon: ImageVector, val color: Color, val allowance: Long, val spent: Long)

// Today's allowance vs spending per category, from the daily doc the Add tab already listens to (0 extra reads).
// Only categories with an allowance or spending today, in dropdown order. Hidden when there's nothing yet.
@Composable
fun TodayByCategoryCard(
    modifier: Modifier = Modifier,
    allowanceByCategory: Map<String, Long>,
    expenseByCategory: Map<String, Long>,
) {
    val categories = remember(allowanceByCategory, expenseByCategory) {
        CategoryDropdownButtonModel.list.drop(1).mapIndexedNotNull { index, name ->
            val allowance = allowanceByCategory[name] ?: 0L
            val spent = expenseByCategory[name] ?: 0L
            if (allowance == 0L && spent == 0L) null
            else CategoryToday(name, CategoryDropdownButtonModel.icons[index], CategoryDropdownButtonModel.colors[index], allowance, spent)
        }
    }
    if (categories.isEmpty()) return

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                stringResource(R.string.add_lbl_today_by_category),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            categories.forEach { CategoryRow(it) }
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryToday) {
    val isOver = category.spent > category.allowance
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(category.color.copy(alpha = 0.35f), CircleShape)
        ) {
            Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        // Name and left/over on the first line, so the "spent of allowance" line below gets the full width
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (isOver) {
                        stringResource(R.string.add_lbl_amount_over, "₱${CurrencyUtils.formatCents(category.spent - category.allowance)}")
                    } else {
                        stringResource(R.string.add_lbl_amount_left, "₱${CurrencyUtils.formatCents(category.allowance - category.spent)}")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text)
                )
            }
            Text(
                if (category.allowance == 0L) {
                    stringResource(R.string.add_lbl_spent_no_allowance, "₱${CurrencyUtils.formatCents(category.spent)}")
                } else {
                    stringResource(
                        R.string.add_lbl_spent_of_allowance,
                        "₱${CurrencyUtils.formatCents(category.spent)}",
                        "₱${CurrencyUtils.formatCents(category.allowance)}"
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun TodayByCategoryCardPreview() {
    DenuSpendTheme {
        Surface {
            TodayByCategoryCard(
                allowanceByCategory = mapOf("Food" to 30000L, "Transportation" to 20000L),
                expenseByCategory = mapOf("Food" to 60400L, "Transportation" to 8000L, "Personal" to 15000L),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
