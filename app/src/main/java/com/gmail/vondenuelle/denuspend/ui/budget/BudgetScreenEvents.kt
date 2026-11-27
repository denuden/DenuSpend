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
    // A "By wallet" row opens that wallet's details
    data class OnOpenWallet(val walletId : String) : BudgetScreenEvents()
    // filter/custom* = the dashboard's range, used only on the first load (later filter changes inside Insights are kept)
    data class OnGetBudgetInsights(
        val category : String,
        val filter : String = "",
        val customStartMillis : Long? = null,
        val customEndMillis : Long? = null,
    ) : BudgetScreenEvents()
    data class OnGetInsightDayTransactions(val dailyDocId : String) : BudgetScreenEvents()
    // null goes back to the month bars
    data class OnOpenInsightsMonth(val month : YearMonth?) : BudgetScreenEvents()
}
