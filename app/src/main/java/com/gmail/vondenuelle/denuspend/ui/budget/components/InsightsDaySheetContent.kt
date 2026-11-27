package com.gmail.vondenuelle.denuspend.ui.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightDay
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.AllowanceLine
import java.time.format.DateTimeFormatter
import java.util.Locale

// Bottom sheet for a tapped day: spent vs that day's allowance, then the day's transactions in this category
@Composable
fun InsightsDaySheetContent(
    day: InsightDay,
    category: String,
    transactions: List<TransactionModel>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            day.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(boldAmounts(stringResource(R.string.insights_day_spent, peso(day.spent))), style = MaterialTheme.typography.bodyMedium)
        if (day.allowance > 0) {
            AllowanceLine(spent = day.spent, allowance = day.allowance, category = category)
        } else {
            Text(
                stringResource(R.string.insights_day_no_allowance, category),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            transactions.isEmpty() -> Text(
                stringResource(R.string.insights_day_empty, category),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            else -> transactions.forEach { transaction ->
                TransactionItem(transactionModel = transaction, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
