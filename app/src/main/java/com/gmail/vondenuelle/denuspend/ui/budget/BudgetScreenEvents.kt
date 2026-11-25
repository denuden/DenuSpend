package com.gmail.vondenuelle.denuspend.ui.budget

import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import java.time.YearMonth

sealed class BudgetScreenEvents {
    data object OnGetBudgetSummary : BudgetScreenEvents()
    data class OnChangeFilter(val filter : BudgetChartFilter) : BudgetScreenEvents()
    data class OnShowRangePicker(val value : Boolean) : BudgetScreenEvents()
    data class OnCustomRangeSelected(val startMillis : Long, val endMillis : Long) : BudgetScreenEvents()
    data class OnNavigateToBudgetTransactionScreen(val category : String) : BudgetScreenEvents()
    // A bill row opens that card's Subscriptions & installments screen
    data class OnOpenBillCard(val walletId : String) : BudgetScreenEvents()
    data class OnGetBudgetInsights(val category : String) : BudgetScreenEvents()
    data class OnGetInsightDayTransactions(val dailyDocId : String) : BudgetScreenEvents()
    // null goes back to the month bars
    data class OnOpenInsightsMonth(val month : YearMonth?) : BudgetScreenEvents()
}
