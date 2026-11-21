package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionListControls
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionSort
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionTypeFilter
import com.gmail.vondenuelle.denuspend.ui.add.components.arrangeTransactions
import com.gmail.vondenuelle.denuspend.ui.add.components.transactionItems
import com.gmail.vondenuelle.denuspend.ui.common.skeleton.SkeletonTransactionList
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.formatFirebaseDate
import androidx.compose.material3.MaterialTheme
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.gmail.vondenuelle.denuspend.R
import kotlin.math.abs

@Composable
fun TransactionDialogContent(
    modifier: Modifier = Modifier,
    day: DailyHistoryModel,
    list: List<TransactionModel>,
    isLoading: Boolean,
    onTransactionClick: (TransactionModel) -> Unit = {},
) {
    // Display-only choices for this day's list, same controls as "Last Added" on the Add tab
    var typeFilter by rememberSaveable { mutableStateOf(TransactionTypeFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(TransactionSort.LATEST) }
    var groupByCategory by rememberSaveable { mutableStateOf(false) }
    val shownTransactions = remember(list, typeFilter, sort) { arrangeTransactions(list, typeFilter, sort) }

    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        item {
            val isOverspent = day.totalExpense > day.totalIncome
            Text(
                formatFirebaseDate(day.date.toDate(), format = "EEEE, MMMM dd, yyyy"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)) {
                DayStat(
                    label = stringResource(R.string.add_lbl_allowance),
                    value = "₱${CurrencyUtils.formatCents(day.totalIncome)}",
                    color = colorResource(R.color.verified_green_text),
                    modifier = Modifier.weight(1f)
                )
                DayStat(
                    label = stringResource(R.string.add_lbl_expense),
                    value = "₱${CurrencyUtils.formatCents(day.totalExpense)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                DayStat(
                    label = stringResource(if (isOverspent) R.string.add_lbl_over else R.string.add_lbl_left),
                    value = "₱${CurrencyUtils.formatCents(abs(day.totalIncome - day.totalExpense))}",
                    color = if (isOverspent) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            TransactionListControls(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                type = typeFilter,
                onTypeChange = { typeFilter = it },
                sort = sort,
                onSortChange = { sort = it },
                groupByCategory = groupByCategory,
                onGroupByCategoryChange = { groupByCategory = it }
            )
        }
        if (isLoading) {
            items(3) {
                SkeletonTransactionList(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
        transactionItems(shownTransactions, groupByCategory, onTransactionClick)
        if (shownTransactions.isEmpty() && !isLoading) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.add_lbl_nothing_to_show), fontWeight = FontWeight.W300)
                }
            }
        }

    }
}

@Composable
private fun DayStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
