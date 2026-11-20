package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import kotlin.math.abs

enum class TransactionTypeFilter(@StringRes val label: Int) {
    ALL(R.string.add_filter_all),
    ALLOWANCE(R.string.add_lbl_allowance),
    EXPENSE(R.string.add_lbl_expense),
}

enum class TransactionSort {
    LATEST, //the order Firestore gives (newest first)
    AMOUNT, //biggest amount first, allowance or expense
}

// Filters and sorts on the device. The lists using this (today, one history day) are always fully loaded,
// so no extra Firestore query or index is needed.
fun arrangeTransactions(
    list: List<TransactionModel>,
    type: TransactionTypeFilter,
    sort: TransactionSort,
): List<TransactionModel> {
    val filtered = when (type) {
        TransactionTypeFilter.ALL -> list
        TransactionTypeFilter.ALLOWANCE -> list.filter { it.amount > 0 }
        TransactionTypeFilter.EXPENSE -> list.filter { it.amount < 0 }
    }
    return when (sort) {
        TransactionSort.LATEST -> filtered
        TransactionSort.AMOUNT -> filtered.sortedByDescending { abs(it.amount) }
    }
}

@Composable
fun TransactionListControls(
    modifier: Modifier = Modifier,
    type: TransactionTypeFilter,
    onTypeChange: (TransactionTypeFilter) -> Unit,
    sort: TransactionSort,
    onSortChange: (TransactionSort) -> Unit,
    groupByCategory: Boolean,
    onGroupByCategoryChange: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        TransactionTypeFilter.entries.forEach { option ->
            FilterChip(
                selected = type == option,
                onClick = { onTypeChange(option) },
                label = { Text(stringResource(option.label)) },
            )
        }
        // One chip that flips between newest first and biggest amount first
        FilterChip(
            selected = sort == TransactionSort.AMOUNT,
            onClick = { onSortChange(if (sort == TransactionSort.AMOUNT) TransactionSort.LATEST else TransactionSort.AMOUNT) },
            label = { Text(stringResource(R.string.add_sort_amount)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
        )
        FilterChip(
            selected = groupByCategory,
            onClick = { onGroupByCategoryChange(!groupByCategory) },
            label = { Text(stringResource(R.string.add_lbl_by_category)) },
            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
        )
    }
}

// Adds the transaction rows to a LazyColumn, either flat or under one header per category.
// Groups follow the category dropdown's order. The list's own order (latest or amount) is kept inside each group.
fun LazyListScope.transactionItems(
    list: List<TransactionModel>,
    groupByCategory: Boolean,
    onTransactionClick: (TransactionModel) -> Unit,
) {
    if (!groupByCategory) {
        items(list) {
            TransactionItem(transactionModel = it, onClick = { onTransactionClick(it) })
        }
        return
    }

    val order = CategoryDropdownButtonModel.list.drop(1)
    val groups = list.groupBy { it.category }.toList()
        .sortedBy { (category, _) -> order.indexOf(category).let { index -> if (index == -1) Int.MAX_VALUE else index } }

    groups.forEach { (category, transactions) ->
        item {
            Text(
                pluralStringResource(R.plurals.add_lbl_category_group, transactions.size, category, transactions.size),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
            )
        }
        items(transactions) {
            TransactionItem(transactionModel = it, onClick = { onTransactionClick(it) })
        }
    }
}
