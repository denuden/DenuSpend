package com.gmail.vondenuelle.denuspend.ui.budget

import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetBillsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import java.time.YearMonth
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel

data class BudgetScreenState(
    val isLoading : Boolean = false,
    val filter : BudgetChartFilter = BudgetChartFilter.THIS_MONTH,
    val showRangePicker : Boolean = false,
    // Only set once a custom range is picked. UTC-midnight millis, as the date range picker gives them.
    val customStartMillis : Long? = null,
    val customEndMillis : Long? = null,
    val budgetTotalSummaryModel: BudgetTotalSummaryModel = BudgetTotalSummaryModel(),
    // "Bills & subscriptions" card (dashboard only), same range as the summary
    val budgetBills : BudgetBillsModel = BudgetBillsModel(),
    // Insights screen (its own ViewModel instance, so filter/custom range above are its own too). Empty = dashboard.
    val insightsCategory : String = "",
    val budgetInsights : BudgetInsightsModel = BudgetInsightsModel(),
    val isInsightsLoading : Boolean = false,
    // Insights day sheet: the tapped day's transactions in this category
    val insightDayTransactions : List<TransactionModel> = emptyList(),
    val isInsightDaySheetLoading : Boolean = false,
    // Long ranges: the month opened from the month bars (null = showing months) and its daily bars
    val insightsMonth : YearMonth? = null,
    val insightsMonthDays : BudgetInsightsModel? = null,
    val isInsightsMonthLoading : Boolean = false,
)
